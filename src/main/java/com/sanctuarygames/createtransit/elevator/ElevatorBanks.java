package com.sanctuarygames.createtransit.elevator;

import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.IndicatorStyle;
import com.sanctuarygames.createtransit.elevator.model.CabinMotionController;
import com.sanctuarygames.createtransit.elevator.model.CabinMotionController.State;
import com.sanctuarygames.createtransit.elevator.model.ModelLandingBlock;
import com.sanctuarygames.createtransit.elevator.model.ModelLandingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.*;

/** World-owned bank membership and dispatch. All changes run on the server thread. */
@EventBusSubscriber(modid=CreateTransit.MOD_ID)
public final class ElevatorBanks extends SavedData {
    private record Car(UUID id,BlockPos drive){}
    public record Landing(ElevatorDriveEntity drive,ElevatorFloor floor,BlockPos base){}
    private record Scope(UUID bank,BlockPos panel,int floor,Direction side){}
    private record Call(Scope scope,int direction,Integer destination){}
    private static final class Job {
        final Call call;
        final Set<UUID> riders=new LinkedHashSet<>();
        Car car;
        long departAt;
        boolean departing;
        long assignedAt;
        Job(Call call){this.call=call;}
    }
    private static final class Bank {
        final UUID id;
        final int rootFloor;
        final Direction side;
        final List<Car> cars=new ArrayList<>();
        int paint,accent;
        boolean showFloorNames=true,directionalLights;
        int callVolume=70;
        ElevatorPartEntity.CallFace callFace=ElevatorPartEntity.CallFace.DIGITAL_TOUCH_PANEL;
        final List<Bank> merged=new ArrayList<>();
        IndicatorStyle button,header;
        Bank(UUID id,int rootFloor,Direction side){this.id=id;this.rootFloor=rootFloor;this.side=side;}
    }
    private final Map<UUID,Bank> banks=new LinkedHashMap<>();
    private final Map<BlockPos,UUID> selectorOverrides=new HashMap<>();
    private final Map<UUID,Bank> membership=new HashMap<>();
    private final List<Job> jobs=new ArrayList<>();
    private record PanelResolution(net.minecraft.world.level.block.state.BlockState state,Scope scope){}
    private final Map<BlockPos,PanelResolution> panelResolutions=new HashMap<>();
    private long nextAmbientLaunch;
    private long resolutionTick=Long.MIN_VALUE;
    private static final String SELECTION="CsuBankSelection";
    private static final String PRIMARY="CsuMergePrimary", SECONDARY="CsuMergeSecondary";

    public static ElevatorBanks get(ServerLevel level){
        return level.getDataStorage().computeIfAbsent(new Factory<>(ElevatorBanks::new,ElevatorBanks::load,null),"createtransit_elevator_banks");
    }

    private static Car car(ElevatorDriveEntity d){return new Car(d.identifier(),d.getBlockPos().immutable());}
    private static ElevatorDriveEntity drive(ServerLevel level,Car c){
        return level.hasChunkAt(c.drive)&&level.getBlockEntity(c.drive) instanceof ElevatorDriveEntity d&&d.identifier().equals(c.id)?d:null;
    }

    /** Search the nearby landing blocks, never an arbitrary shaft elsewhere in the dimension. */
    public static List<Landing> neighboring(ServerLevel level,BlockPos panel,Direction side){
        var candidates=new LinkedHashMap<UUID,Landing>();
        var inspected=new HashSet<BlockPos>();
        Direction across=side.getClockWise();
        for(int row=0;row<3;row++)for(int depth=0;depth<=1;depth++)for(int offset=-5;offset<=5;offset++){
            var pos=panel.below(row).relative(side.getOpposite(),depth).relative(across,offset);
            if(!level.hasChunkAt(pos))continue;
            var state=level.getBlockState(pos);
            if(!(state.getBlock() instanceof ModelLandingBlock)||state.getValue(ElevatorPartBlock.FACING)!=side.getOpposite())continue;
            var base=ModelLandingBlock.base(pos,state);
            if(!inspected.add(base))continue;
            if(!(level.getBlockEntity(base) instanceof ModelLandingEntity landing)||landing.markerDrive()==null||!level.hasChunkAt(landing.markerDrive()))continue;
            if(!(level.getBlockEntity(landing.markerDrive()) instanceof ElevatorDriveEntity drive)||drive.model()==null)continue;
            drive.ensureFloors();
            int floor=base.getY()-1;
            if(!drive.model().servesLanding(floor,side.getOpposite()))continue;
            for(var stop:drive.floorList())if(stop.y()==floor&&stop.enabled()&&stop.reachable())
                candidates.putIfAbsent(drive.identifier(),new Landing(drive,stop,base));
        }
        if(candidates.isEmpty())return List.of();
        int y=candidates.values().stream().mapToInt(l->l.floor.y()).max().orElseThrow();
        return candidates.values().stream().filter(l->l.floor.y()==y).toList();
    }

    private static int axis(BlockPos p,Direction axis){return p.getX()*axis.getStepX()+p.getZ()*axis.getStepZ();}
    private static List<Landing> pair(List<Landing> local,BlockPos panel,Direction side){
        Direction across=side.getOpposite().getClockWise();
        int middle=axis(panel,across);
        Landing left=null,right=null;
        for(var l:local){
            int first=axis(l.base,across);
            int last=axis(ModelLandingBlock.part(l.base,side.getOpposite(),l.drive.model().config().size()-1,0),across);
            if(Math.max(first,last)<middle&&(left==null||l.base.distSqr(panel)<left.base.distSqr(panel)))left=l;
            if(Math.min(first,last)>middle&&(right==null||l.base.distSqr(panel)<right.base.distSqr(panel)))right=l;
        }
        return left==null||right==null?List.of():List.of(left,right);
    }

    private Scope resolve(ServerLevel level,BlockPos panel){
        var block=level.getBlockState(panel);
        if(resolutionTick!=level.getGameTime()){
            resolutionTick=level.getGameTime();
            panelResolutions.clear();
        }
        var cached=panelResolutions.get(panel);
        if(cached!=null&&cached.state==block)return cached.scope;
        var scope=resolvePanel(level,panel,block);
        panelResolutions.put(panel.immutable(),new PanelResolution(block,scope));
        return scope;
    }

    private Scope resolvePanel(ServerLevel level,BlockPos panel,net.minecraft.world.level.block.state.BlockState block){
        if(!(block.getBlock() instanceof ElevatorCallBlock call))return null;
        if(!call.destinationSelector()&&level.getBlockEntity(panel) instanceof ElevatorPartEntity part
                &&(!part.callMarkers().isEmpty()||part.markerDrive()!=null))return null;
        Direction side=block.getValue(ElevatorPartBlock.FACING);
        var local=neighboring(level,panel,side);
        if(local.isEmpty())return null;
        var recognized=new HashSet<Bank>();
        for(var l:local){
            var bank=membership.get(l.drive.identifier());
            if(bank!=null)recognized.add(bank);
        }
        if(call.destinationSelector()){
            var chosen=activeBank(selectorOverrides.get(panel));
            if(chosen!=null&&recognized.contains(chosen))return new Scope(chosen.id,panel.immutable(),local.getFirst().floor.y(),side);
        }
        if(recognized.size()>1)return null;
        if(!recognized.isEmpty())return new Scope(recognized.iterator().next().id,panel.immutable(),local.getFirst().floor.y(),side);
        var adjacent=pair(local,panel,side);
        if(adjacent.size()==2&&adjacent.stream().noneMatch(l->membership.containsKey(l.drive.identifier())))
            return new Scope(null,panel.immutable(),adjacent.getFirst().floor.y(),side);
        return null;
    }

    private List<Car> cars(ServerLevel level,Scope scope){
        if(scope.bank!=null){
            var bank=banks.get(scope.bank);
            return bank==null?List.of():bank.cars;
        }
        return pair(neighboring(level,scope.panel,scope.side),scope.panel,scope.side).stream()
            .filter(l->l.floor.y()==scope.floor).map(l->car(l.drive)).toList();
    }

    private boolean sameLanding(Scope a,Scope b){
        return a.floor==b.floor&&a.side==b.side&&Objects.equals(a.bank,b.bank)&&(a.bank!=null||a.panel.equals(b.panel));
    }

    private static boolean servesPickup(ElevatorDriveEntity drive,Scope scope){
        if(drive==null||drive.model()==null)return false;
        if(drive.model().servesLanding(scope.floor,scope.side.getOpposite()))return true;
        return scope.bank!=null&&drive.model().servesStop(scope.floor);
    }
    private boolean eligible(ElevatorDriveEntity d,Call call){
        if(!servesPickup(d,call.scope)||!d.model().active())return false;
        if(call.destination!=null)return d.model().servesStop(call.destination);
        return d.floorList().stream().anyMatch(f->Integer.compare(f.y(),call.scope.floor)==call.direction&&d.model().servesStop(f));
    }

    public ElevatorDriveEntity menuDrive(ServerLevel level,BlockPos panel,boolean setup){
        var scope=resolve(level,panel);
        if(scope==null||!setup&&scope.bank==null)return null;
        return cars(level,scope).stream().map(c->drive(level,c)).filter(Objects::nonNull).findFirst().orElse(null);
    }
    public boolean hasService(ServerLevel level,BlockPos panel){return resolve(level,panel)!=null;}
    public boolean boarding(UUID car,CabinMotionController.Snapshot motion){
        if(motion.state()!=State.DOORS_OPEN)return false;
        for(var job:jobs)if(job.car!=null&&job.car.id.equals(car)&&!job.departing
                &&job.call.destination!=null&&Math.abs(motion.y()-job.call.scope.floor)<.001
                &&!motion.queue().contains(job.call.scope.floor))return true;
        return false;
    }
    public boolean claimAmbientLaunch(long now){
        if(now<nextAmbientLaunch)return false;
        nextAmbientLaunch=now+40;
        return true;
    }
    public boolean hasPassengerWork(UUID car){
        var bank=membership.get(car);
        for(var job:jobs)if(job.car!=null&&job.car.id.equals(car)
                ||bank!=null&&bank.id.equals(job.call.scope.bank))return true;
        return false;
    }
    public int directions(ServerLevel level,BlockPos panel){
        var scope=resolve(level,panel);
        if(scope==null)return 0;
        int mask=0;
        for(var c:cars(level,scope)){
            var d=drive(level,c);
            if(eligible(d,new Call(scope,1,null)))mask|=1;
            if(eligible(d,new Call(scope,-1,null)))mask|=2;
        }
        return mask;
    }
    public int pending(ServerLevel level,BlockPos panel){
        var scope=resolve(level,panel);
        if(scope==null)return 0;
        int mask=0;
        for(var job:jobs)if(job.call.destination==null&&sameLanding(job.call.scope,scope))mask|=job.call.direction>0?1:2;
        return mask;
    }
    public boolean request(ServerLevel level,BlockPos panel,int direction,Integer destination,ServerPlayer rider){
        var scope=resolve(level,panel);
        if(scope==null||Math.abs(direction)!=1||destination!=null&&(scope.bank==null||Integer.compare(destination,scope.floor)!=direction))return false;
        var call=new Call(scope,direction,destination);
        if(cars(level,scope).stream().noneMatch(c->eligible(drive(level,c),call)))return false;
        for(var job:jobs)if(!job.departing&&sameLanding(job.call.scope,scope)&&job.call.direction==direction&&Objects.equals(job.call.destination,destination)){
            if(rider!=null){job.riders.add(rider.getUUID());notifyRider(level,job,rider);}
            setDirty();return true;
        }
        var job=new Job(call);
        if(rider!=null){job.riders.add(rider.getUUID());notifyRider(level,job,rider);}
        jobs.add(job);setDirty();
        return true;
    }

    private void notifyRider(ServerLevel level,Job job,ServerPlayer rider){
        if(job.call.destination==null)return;
        rider.displayClientMessage(Component.literal(job.car==null?"Finding a car…":"Car "+letter(job.car.id)+" · "+destinationLabel(level,job)),true);
    }
    private String destinationLabel(ServerLevel level,Job job){
        var d=job.car==null?null:drive(level,job.car);
        return d==null?"Y "+job.call.destination:d.floorLabel(job.call.destination)+" · "+d.floorDisplayName(job.call.destination);
    }
    private void announce(ServerLevel level,Job job){
        for(var id:job.riders){
            var rider=level.getServer().getPlayerList().getPlayer(id);
            if(rider!=null&&rider.level()==level)notifyRider(level,job,rider);
        }
    }

    public String letter(UUID car){
        var bank=membership.get(car);
        if(bank==null)return "";
        for(int i=0;i<bank.cars.size();i++)if(bank.cars.get(i).id.equals(car))return letterAt(i);
        return "";
    }
    private static String letterAt(int index){
        var name=new StringBuilder();
        for(int n=index+1;n>0;n=(n-1)/26)name.append((char)('A'+(n-1)%26));
        return name.reverse().toString();
    }

    public int arrivalDirection(UUID car,int floor){
        for(var job:jobs)if(job.car!=null&&job.car.id.equals(car)&&!job.departing&&job.call.scope.floor==floor)return job.call.direction;
        return 0;
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event){
        if(event.getLevel() instanceof ServerLevel level&&level.getGameTime()%5==0)get(level).dispatch(level);
    }

    private void dispatch(ServerLevel level){
        var reserved=new HashSet<UUID>();
        for(var job:jobs)if(job.car!=null)reserved.add(job.car.id);
        for(var iterator=jobs.iterator();iterator.hasNext();){
            var job=iterator.next();
            var call=job.call;
            if(job.car!=null){
                if(!level.hasChunkAt(job.car.drive))continue;
                var d=drive(level,job.car);
                var model=d==null?null:d.model();
                var motion=model==null?null:model.state();
                if(model==null||!model.active()||motion!=null&&motion.state()==State.FAULT){
                    if(model!=null&&!job.departing)model.cancelFailedBankPickup(call.scope.floor);
                    reserved.remove(job.car.id);
                    if(job.departing){iterator.remove();setDirty();continue;}
                    job.car=null;job.departAt=0;setDirty();announce(level,job);
                }else if(motion!=null&&motion.state()==State.DOORS_OPEN){
                    int y=(int)Math.rint(motion.y());
                    if(job.departing&&Objects.equals(call.destination,y)){
                        reserved.remove(job.car.id);iterator.remove();setDirty();continue;
                    }
                    if(!job.departing&&y==call.scope.floor&&!model.pending(y)){
                        if(job.departAt==0&&model.entity()!=null)model.entity().arrival(call.direction,level.getGameTime());
                        if(call.destination==null){reserved.remove(job.car.id);iterator.remove();setDirty();continue;}
                        if(job.departAt==0){job.departAt=level.getGameTime()+60;setDirty();}
                        if(level.getGameTime()>=job.departAt){
                            if(model.requestBankDestination(call.destination)){job.departing=true;d.setChanged();setDirty();}
                            else{
                                for(var id:job.riders){
                                    var player=level.getServer().getPlayerList().getPlayer(id);
                                    if(player!=null)ElevatorFeedback.error(player,"The assigned car can no longer serve this destination.");
                                }
                                reserved.remove(job.car.id);iterator.remove();setDirty();continue;
                            }
                        }
                    }
                }
            }
            if(job.car!=null)continue;
            if(call.scope.bank==null&&level.hasChunkAt(call.scope.panel)&&!(level.getBlockState(call.scope.panel).getBlock() instanceof ElevatorCallBlock)){
                iterator.remove();setDirty();continue;
            }
            ElevatorDriveEntity best=null;
            double distance=Double.POSITIVE_INFINITY;
            for(var c:cars(level,call.scope)){
                if(reserved.contains(c.id))continue;
                var d=drive(level,c);
                if(!eligible(d,call))continue;
                var state=d.model().state();
                if(state==null||d.model().entity()==null||(!state.ambient()&&state.state()!=State.IDLE&&state.state()!=State.DOORS_OPEN)||!state.queue().isEmpty())continue;
                double delta=Math.abs(state.y()-call.scope.floor)+(state.ambient()?10000:0);
                if(delta<distance){best=d;distance=delta;}
            }
            if(best!=null&&((best.model().state().state()==State.DOORS_OPEN
                    &&Math.abs(best.model().state().y()-call.scope.floor)<.001
                    &&best.model().allows(best.model().landingPosition(call.scope.floor,
                        best.model().landingFacing(true)==call.scope.side.getOpposite()))
                    &&best.model().doorsOpened())||best.model().request(call.scope.floor))){
                job.car=car(best);job.assignedAt=level.getGameTime();reserved.add(job.car.id);best.setChanged();setDirty();announce(level,job);
            }
        }
    }

    public String recentAssignment(ServerLevel level,BlockPos panel){
        for(int i=jobs.size()-1;i>=0;i--){
            var job=jobs.get(i);
            if(job.call.destination!=null&&job.call.scope.panel.equals(panel)&&job.car!=null
                    &&level.getGameTime()-job.assignedAt<80)return letter(job.car.id)+"\n"+destinationLabel(level,job);
        }
        return "";
    }

    public List<ElevatorFloor> destinations(ServerLevel level,BlockPos panel){
        var scope=resolve(level,panel);
        if(scope==null||scope.bank==null)return List.of();
        var result=new TreeMap<Integer,ElevatorFloor>();
        for(var c:cars(level,scope)){
            var d=drive(level,c);
            if(!servesPickup(d,scope))continue;
            for(var floor:d.floorList())if(floor.y()!=scope.floor){
                boolean available=d.model().active()&&d.model().servesStop(floor);
                var entry=new ElevatorFloor(floor.marker(),floor.y(),floor.id(),floor.name(),available,floor.reachable(),floor.warning(),floor.rail());
                result.merge(floor.y(),entry,(previous,current)->current.enabled()?current:previous);
            }
        }
        return List.copyOf(result.values());
    }

    public boolean destination(ServerLevel level,BlockPos panel,int y,ServerPlayer player){
        var scope=resolve(level,panel);
        return scope!=null&&request(level,panel,Integer.compare(y,scope.floor),y,player);
    }

    public String assignment(ServerLevel level,BlockPos panel,UUID rider){
        var scope=resolve(level,panel);
        if(scope==null)return "";
        for(int i=jobs.size()-1;i>=0;i--){
            var job=jobs.get(i);
            if(job.call.destination==null||!sameLanding(job.call.scope,scope)||!job.riders.contains(rider))continue;
            return job.car==null?"Finding a car…":level.getGameTime()-job.assignedAt<80?
                "Car "+letter(job.car.id)+" · "+destinationLabel(level,job):"";
        }
        return "";
    }

    public List<String> preview(ServerLevel level,BlockPos panel){
        var side=level.getBlockState(panel).getValue(ElevatorPartBlock.FACING);
        var scope=resolve(level,panel);
        if(scope!=null&&scope.bank!=null)return cars(level,scope).stream().map(c->letter(c.id)+" · "+c.drive.toShortString()).toList();
        return pair(neighboring(level,panel,side),panel,side).stream().map(l->l.drive.elevatorName()+" · "+l.base.toShortString()).toList();
    }

    public String link(ServerLevel level,BlockPos panel,ServerPlayer player,boolean create){
        if(!(level.getBlockEntity(panel) instanceof ElevatorPartEntity part))return "Call panel missing";
        var side=level.getBlockState(panel).getValue(ElevatorPartBlock.FACING);
        var adjacent=pair(neighboring(level,panel,side),panel,side);
        if(adjacent.size()!=2)return "Place the panel between two linked landings";
        int floor=adjacent.getFirst().floor.y();
        var a=adjacent.getFirst().drive;var b=adjacent.getLast().drive;
        int lowest=a.floorList().stream().filter(f->a.model().servesLanding(f.y(),side.getOpposite())&&b.model().servesLanding(f.y(),side.getOpposite())).mapToInt(ElevatorFloor::y).min().orElse(floor);
        if(floor!=lowest)return "Link bank panels at the lowest shared floor";
        var clipboard=player.getPersistentData();
        Bank bank;
        if(create){
            if(membership.containsKey(a.identifier())||membership.containsKey(b.identifier()))return "A shaft already belongs to a bank";
            bank=new Bank(UUID.randomUUID(),floor,side);
            bank.paint=part.paint();bank.accent=part.accent();
            bank.button=a.model().config().sideIndicatorStyle();bank.header=a.model().config().indicatorStyle();
            banks.put(bank.id,bank);
        }else{
            bank=clipboard.hasUUID(SELECTION)?banks.get(clipboard.getUUID(SELECTION)):null;
            if(bank==null||bank.rootFloor!=floor||bank.side!=side)return "Select a bank on this hallway's lowest floor first";
            if(membership.get(a.identifier())!=bank&&membership.get(b.identifier())!=bank)return "At least one neighboring car must belong to the selected bank";
            for(var d:List.of(a,b))if(membership.containsKey(d.identifier())&&membership.get(d.identifier())!=bank)return "A shaft belongs to another bank";
        }
        adjacent.stream().map(Landing::drive).sorted(Comparator.comparingInt(d->axis(d.getBlockPos(),side.getOpposite().getClockWise()))).forEach(d->{
            if(!membership.containsKey(d.identifier())){bank.cars.add(car(d));membership.put(d.identifier(),bank);applyStyle(d);}
        });
        panelResolutions.clear();
        clipboard.putUUID(SELECTION,bank.id);
        setDirty();return "";
    }
    public boolean select(ServerLevel level,BlockPos panel,ServerPlayer player){
        var scope=resolve(level,panel);
        if(scope==null||scope.bank==null||banks.get(scope.bank).rootFloor!=scope.floor)return false;
        player.getPersistentData().putUUID(SELECTION,scope.bank);return true;
    }

    private Bank activeBank(UUID id){
        if(id==null)return null;
        var direct=banks.get(id);
        if(direct!=null)return direct;
        for(var bank:banks.values())if(containsBank(bank,id))return bank;
        return null;
    }
    private static boolean containsBank(Bank bank,UUID id){
        return bank.id.equals(id)||bank.merged.stream().anyMatch(child->containsBank(child,id));
    }
    public boolean associateSelector(ServerLevel level,BlockPos panel,ServerPlayer player){
        var selected=player.getPersistentData();
        var bank=selected.hasUUID(SELECTION)?activeBank(selected.getUUID(SELECTION)):null;
        var side=level.getBlockState(panel).getValue(ElevatorPartBlock.FACING);
        if(bank==null||neighboring(level,panel,side).stream().noneMatch(l->membership.get(l.drive.identifier())==bank))return false;
        selectorOverrides.put(panel.immutable(),bank.id);
        panelResolutions.clear();
        setDirty();
        return true;
    }
    public boolean showFloorNames(ServerLevel level,BlockPos panel){
        var scope=resolve(level,panel);
        return scope!=null&&scope.bank!=null&&banks.get(scope.bank).showFloorNames;
    }
    public String mergePreview(ServerLevel level,BlockPos panel,ServerPlayer player){
        var data=player.getPersistentData();
        var scope=resolve(level,panel);
        if(!data.hasUUID(PRIMARY)||scope==null||scope.bank==null)return "";
        var primary=banks.get(data.getUUID(PRIMARY));
        if(primary==null)return "";
        if(!data.hasUUID(SECONDARY))return "Primary selected ("+primary.cars.size()+" cars). Open the secondary bank.";
        var secondary=banks.get(data.getUUID(SECONDARY));
        if(secondary==null||!secondary.id.equals(scope.bank))return "Open the selected secondary bank to confirm.";
        var labels=new ArrayList<String>();
        for(var car:primary.cars)labels.add(letter(car.id)+" · "+car.drive.toShortString());
        for(int i=0;i<secondary.cars.size();i++)labels.add(letterAt(primary.cars.size()+i)+" · "+secondary.cars.get(i).drive.toShortString());
        return String.join("   |   ",labels);
    }
    private boolean hasActiveJobs(Bank bank){
        return jobs.stream().anyMatch(job->bank.id.equals(job.call.scope.bank)
            ||job.car!=null&&membership.get(job.car.id)==bank);
    }
    public String mergeAction(ServerLevel level,BlockPos panel,ServerPlayer player,int operation){
        var scope=resolve(level,panel);
        if(scope==null||scope.bank==null)return "Open a linked bank panel";
        var bank=banks.get(scope.bank);
        var data=player.getPersistentData();
        if(operation==24){
            data.putUUID(PRIMARY,bank.id);
            data.remove(SECONDARY);
            return "";
        }
        if(operation==27){
            if(bank.merged.isEmpty())return "This bank has no merger to undo";
            if(hasActiveJobs(bank))return "Wait for active bank dispatches to finish";
            var previous=bank.merged.removeLast();
            bank.cars.removeAll(previous.cars);
            banks.put(previous.id,previous);
            previous.cars.forEach(car->membership.put(car.id,previous));
        }else{
            var primary=data.hasUUID(PRIMARY)?banks.get(data.getUUID(PRIMARY)):null;
            if(primary==null||primary==bank)return "Select a different primary bank first";
            if(operation==25){
                data.putUUID(SECONDARY,bank.id);
                return "";
            }
            if(!data.hasUUID(SECONDARY)||!bank.id.equals(data.getUUID(SECONDARY)))return "Preview this secondary bank first";
            if(hasActiveJobs(primary)||hasActiveJobs(bank))return "Wait for both banks' active dispatches to finish";
            primary.merged.add(bank);
            primary.cars.addAll(bank.cars);
            bank.cars.forEach(car->membership.put(car.id,primary));
            banks.remove(bank.id);
            data.putUUID(SELECTION,primary.id);
            data.remove(PRIMARY);
            data.remove(SECONDARY);
        }
        panelResolutions.clear();
        setDirty();
        return "";
    }

    private static Bank appearanceBank(Bank bank,UUID id){
        for(var child:bank.merged)
            if(child.cars.stream().anyMatch(car->car.id.equals(id)))return appearanceBank(child,id);
        return bank;
    }
    public void applyStyle(ElevatorDriveEntity drive){
        var bank=membership.get(drive.identifier());
        if(bank==null||drive.model()==null)return;
        bank=appearanceBank(bank,drive.identifier());
        var config=drive.model().config();
        if(config.indicatorStyle()!=bank.header)drive.configureModel("indicatorStyle",bank.header.name());
        if(config.sideIndicatorStyle()!=bank.button)drive.configureModel("sideIndicatorStyle",bank.button.name());
    }

    public boolean customize(ServerLevel level,BlockPos panel,int action){
        var scope=resolve(level,panel);
        if(scope==null)return false;
        if(scope.bank==null){
            if(!(level.getBlockEntity(panel) instanceof ElevatorPartEntity part))return false;
            switch(action){
                case 29 -> part.callFace=nextFace(part.callFace);
                case 30 -> part.directionalCallLights=!part.directionalCallLights;
                case 31 -> part.callVolume=nextVolume(part.callVolume);
                default -> {return false;}
            }
            part.sync();return true;
        }
        var bank=banks.get(scope.bank);
        var styles=IndicatorStyle.values();
        switch(action){
            case 20 -> bank.button=styles[(bank.button.ordinal()+1)%styles.length];
            case 21 -> bank.header=styles[(bank.header.ordinal()+1)%styles.length];
            case 22 -> bank.paint=(bank.paint+1)%16;
            case 23 -> bank.accent=(bank.accent+1)%16;
            case 28 -> bank.showFloorNames=!bank.showFloorNames;
            case 29 -> bank.callFace=nextFace(bank.callFace);
            case 30 -> bank.directionalLights=!bank.directionalLights;
            case 31 -> bank.callVolume=nextVolume(bank.callVolume);
            default -> {return false;}
        }
        for(var c:bank.cars){var d=drive(level,c);if(d!=null)applyStyle(d);}
        setDirty();return true;
    }

    private static ElevatorPartEntity.CallFace nextFace(ElevatorPartEntity.CallFace current){
        var values=ElevatorPartEntity.CallFace.values();
        return values[(current.ordinal()+1)%values.length];
    }
    private static int nextVolume(int volume){
        return switch(volume){case 0->25;case 25->50;case 50->70;case 70->100;default->0;};
    }
    public CompoundTag callAppearance(ServerLevel level,BlockPos panel){
        var out=new CompoundTag();
        var scope=resolve(level,panel);
        var bank=scope==null||scope.bank==null?null:banks.get(scope.bank);
        if(level.getBlockEntity(panel) instanceof ElevatorPartEntity part){
            out.putString("Face",(bank==null?part.callFace:bank.callFace).name());
            out.putBoolean("Directional",bank==null?part.directionalCallLights:bank.directionalLights);
            out.putInt("Volume",bank==null?part.callVolume:bank.callVolume);
        }
        return out;
    }
    public CompoundTag appearance(ServerLevel level,BlockPos panel){
        var scope=resolve(level,panel);
        var out=new CompoundTag();
        if(scope==null||scope.bank==null)return out;
        var bank=banks.get(scope.bank);
        out.putString("Button",bank.button.name());out.putString("Header",bank.header.name());
        out.putInt("Paint",bank.paint);out.putInt("Accent",bank.accent);out.putBoolean("ShowFloorNames",bank.showFloorNames);out.putBoolean("CanUnmerge",!bank.merged.isEmpty());return out;
    }

    private static CompoundTag writeCar(Car car){var t=new CompoundTag();t.putUUID("Id",car.id);t.putLong("Drive",car.drive.asLong());return t;}
    private static Car readCar(CompoundTag t){return new Car(t.getUUID("Id"),BlockPos.of(t.getLong("Drive")));}
    private static CompoundTag writeBank(Bank bank){
        var t=new CompoundTag();
        t.putUUID("Id",bank.id);t.putInt("Y",bank.rootFloor);t.putString("Side",bank.side.getName());
        t.putInt("Paint",bank.paint);t.putInt("Accent",bank.accent);
        t.putString("Button",bank.button.name());t.putString("Header",bank.header.name());
        t.putBoolean("ShowFloorNames",bank.showFloorNames);
        t.putString("CallFace",bank.callFace.name());
        t.putBoolean("DirectionalLights",bank.directionalLights);
        t.putInt("CallVolume",bank.callVolume);
        var members=new ListTag();bank.cars.forEach(c->members.add(writeCar(c)));t.put("Cars",members);
        var merged=new ListTag();bank.merged.forEach(child->merged.add(writeBank(child)));t.put("Merged",merged);
        return t;
    }
    private static Bank readBank(CompoundTag t){
        var side=Direction.byName(t.getString("Side"));
        if(!t.hasUUID("Id")||side==null||!side.getAxis().isHorizontal())return null;
        var bank=new Bank(t.getUUID("Id"),t.getInt("Y"),side);
        bank.paint=t.getInt("Paint");bank.accent=t.getInt("Accent");
        bank.button=IndicatorStyle.valueOf(t.getString("Button"));bank.header=IndicatorStyle.valueOf(t.getString("Header"));
        bank.showFloorNames=!t.contains("ShowFloorNames")||t.getBoolean("ShowFloorNames");
        try{bank.callFace=ElevatorPartEntity.CallFace.valueOf(t.getString("CallFace"));}catch(IllegalArgumentException ignored){}
        bank.directionalLights=t.getBoolean("DirectionalLights");
        if(t.contains("CallVolume"))bank.callVolume=Math.clamp(t.getInt("CallVolume"),0,100);
        for(var member:t.getList("Cars",10))if(member instanceof CompoundTag m&&m.hasUUID("Id"))bank.cars.add(readCar(m));
        for(var entry:t.getList("Merged",10))if(entry instanceof CompoundTag child){
            var merged=readBank(child);
            if(merged!=null)bank.merged.add(merged);
        }
        return bank;
    }
    @Override public CompoundTag save(CompoundTag out,HolderLookup.Provider provider){
        var list=new ListTag();
        banks.values().forEach(bank->list.add(writeBank(bank)));
        out.put("Banks",list);
        var selectors=new ListTag();
        selectorOverrides.forEach((pos,id)->{
            var tag=new CompoundTag();tag.putLong("Pos",pos.asLong());tag.putUUID("Bank",id);selectors.add(tag);
        });
        out.put("SelectorOverrides",selectors);
        var pending=new ListTag();
        for(var job:jobs){
            var c=job.call;var t=new CompoundTag();
            if(c.scope.bank!=null)t.putUUID("Bank",c.scope.bank);
            t.putLong("Panel",c.scope.panel.asLong());t.putInt("Y",c.scope.floor);t.putString("Side",c.scope.side.getName());
            t.putInt("Direction",c.direction);
            if(c.destination!=null)t.putInt("Destination",c.destination);
            if(job.car!=null)t.put("Car",writeCar(job.car));
            t.putLong("AssignedAt",job.assignedAt);t.putBoolean("Departing",job.departing);t.putLong("DepartAt",job.departAt);
            var riders=new ListTag();
            job.riders.forEach(id->riders.add(net.minecraft.nbt.NbtUtils.createUUID(id)));
            t.put("Riders",riders);pending.add(t);
        }
        out.put("Pending",pending);return out;
    }
    private static ElevatorBanks load(CompoundTag data,HolderLookup.Provider provider){
        var result=new ElevatorBanks();
        for(var entry:data.getList("Banks",10))if(entry instanceof CompoundTag t){
            var bank=readBank(t);
            if(bank==null)continue;
            bank.cars.removeIf(car->result.membership.putIfAbsent(car.id,bank)!=null);
            result.banks.put(bank.id,bank);
        }
        for(var entry:data.getList("SelectorOverrides",10))if(entry instanceof CompoundTag t&&t.hasUUID("Bank"))
            result.selectorOverrides.put(BlockPos.of(t.getLong("Pos")),t.getUUID("Bank"));
        for(var entry:data.getList("Pending",10))if(entry instanceof CompoundTag t){
            var side=Direction.byName(t.getString("Side"));
            if(side==null||!side.getAxis().isHorizontal())continue;
            UUID id=t.hasUUID("Bank")?t.getUUID("Bank"):null;
            if(id!=null&&!result.banks.containsKey(id))continue;
            var job=new Job(new Call(new Scope(id,BlockPos.of(t.getLong("Panel")),t.getInt("Y"),side),t.getInt("Direction"),t.contains("Destination")?t.getInt("Destination"):null));
            var assigned=t.getCompound("Car");
            if(assigned.hasUUID("Id"))job.car=readCar(assigned);
            job.assignedAt=t.getLong("AssignedAt");job.departing=t.getBoolean("Departing");job.departAt=t.getLong("DepartAt");
            for(var rider:t.getList("Riders",11))job.riders.add(net.minecraft.nbt.NbtUtils.loadUUID(rider));
            result.jobs.add(job);
        }
        return result;
    }
}
