package com.sanctuarygames.createtransit.elevator;

import java.util.*;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.*;

/** The existing independent Create controller, with rail stops and explicit interlocks. */
public final class ElevatorDriveEntity extends ElevatorPartEntity implements IControlContraption {
    // Preserve the original persisted phase numbers and public names.
    public static final int IDLE=0, CLOSING=1, MOVING=2, OPENING=3, FAULT=4, LEVELING=5, DOORS_OPEN=6;
    public static final int DOORS_CLOSING=CLOSING, DOORS_OPENING=OPENING;
    private ControlledContraptionEntity cabin;
    private com.sanctuarygames.createtransit.elevator.model.ModelCabinController model;
    public com.sanctuarygames.createtransit.elevator.model.ModelCabinController model(){return model;}
    public boolean pasteModelSettings(CompoundTag settings){
        if(model==null)return reject(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.configure_the_target_cabin_first"));
        boolean applied=model.pasteSettings(settings);
        message=model.warningText();sync();
        return applied;
    }
    public boolean configureModel(String key,String value){if(cabin!=null||cabinId!=null)return reject(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.return_and_disassemble_the_legacy_cabin"));if(model==null)model=new com.sanctuarygames.createtransit.elevator.model.ModelCabinController(this);boolean ok=model.configure(key,value);message=model.warningText();sync();return ok;}
    public boolean activateModel(){if(model==null)model=new com.sanctuarygames.createtransit.elevator.model.ModelCabinController(this);if(cabin!=null||cabinId!=null)return reject(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.disassemble_the_legacy_cabin_first"));boolean ok=model.activate();message=model.warningText();sync();return ok;}
    public boolean installModelLanding(Player p,int y){if(model==null)model=new com.sanctuarygames.createtransit.elevator.model.ModelCabinController(this);boolean ok=model.install(p,y);message=model.warningText();sync();return ok;}
    public boolean modelPolicy(int y,boolean front,boolean rear,int fs,int rs){if(model==null)model=new com.sanctuarygames.createtransit.elevator.model.ModelCabinController(this);boolean ok=model.policy(y,new com.sanctuarygames.createtransit.elevator.model.ModelCabinController.Policy(front,rear,fs,rs));message=model.warningText();sync();return ok;}
    public boolean deactivateModel(){if(model==null)return false;boolean ok=model.deactivate();message=model.warningText();sync();return ok;}
    private String lastModelFault="";
    private com.sanctuarygames.createtransit.elevator.model.CabinMotionController.Snapshot lastSyncedModelState;
    private String lastSyncedModelMessage="";
    private void tickModel(){model.tick();var s=model.state();message=model.warningText();String fault=s!=null&&s.state()==com.sanctuarygames.createtransit.elevator.model.CabinMotionController.State.FAULT?s.fault():"";if(!fault.equals(lastModelFault)){lastModelFault=fault;if(!fault.isEmpty())for(var player:level.players())if(player.distanceToSqr(Vec3.atCenterOf(worldPosition))<=1024||model.entity()!=null&&model.entity().getBoundingBox().inflate(2).contains(player.position()))ElevatorFeedback.error(player,fault);}if(s!=null){offset=s.y()-anchor().getY();target=s.target()==null?offset:s.target()-anchor().getY();queue.clear();queue.addAll(s.queue());phase=switch(s.state()){case IDLE->IDLE;case DOORS_CLOSING->CLOSING;case MOVING->MOVING;case LEVELING->LEVELING;case DOORS_OPENING->OPENING;case DOORS_OPEN->DOORS_OPEN;case FAULT->FAULT;};}if(!java.util.Objects.equals(lastSyncedModelState,s)||!lastSyncedModelMessage.equals(message)){setChanged();if(level.getGameTime()%5==0){sync();lastSyncedModelState=s;lastSyncedModelMessage=message;}}}
    private UUID cabinId, identifier=UUID.randomUUID();
    private double offset, previous, start, target;
    private int elapsed, duration, phase;
    private long motionTick=Long.MIN_VALUE;
    private boolean returnAndDisassemble, railMode, railsDirty=true, scanning;
    private String message="Ready", elevatorName="Elevator", railWarning="";
    private int railTop, openingDelay=5, closingDelay=5, dwellTicks=40;
    private final LinkedHashSet<Integer> queue=new LinkedHashSet<>();
    private List<ElevatorFloor> floors=List.of();
    private final Map<Integer,ElevatorFloor> legacySettings=new HashMap<>();

    public ElevatorDriveEntity(BlockPos pos,BlockState state){super(ElevatorRegistry.DRIVE_ENTITY.get(),pos,state);}
    public BlockPos anchor(){return worldPosition.relative(getBlockState().getValue(ElevatorPartBlock.FACING));}
    public double offset(){return offset;} public double previousOffset(){return previous;}
    public long motionTick(){return motionTick;} public int phase(){return phase;}
    public String message(){return com.sanctuarygames.createtransit.TransitText.plain(message);} public String railWarning(){return com.sanctuarygames.createtransit.TransitText.plain(railWarning);} public String railWarningText(){return railWarning;}
    public String elevatorName(){return elevatorName;} public UUID identifier(){return identifier;}
    public ControlledContraptionEntity cabin(){return cabin;}
    public boolean assembled(){return model!=null&&model.active()||cabin!=null||cabinId!=null;}
    public boolean railMode(){return railMode;} public List<ElevatorFloor> floorList(){return floors;}
    public List<Integer> requests(){return List.copyOf(queue);}
    public int currentFloor(){return anchor().getY()+(int)Math.round(offset);}
    public int targetFloor(){return anchor().getY()+(int)Math.round(target);}
    public boolean pending(int y){if(model!=null)return model.pending(y);return queue.contains(y)||((phase==CLOSING||phase==MOVING||phase==LEVELING)&&targetFloor()==y);}
    public void invalidateRails(){railsDirty=true;}
    public void ensureFloors(){if(level!=null&&!level.isClientSide&&!scanning&&(railsDirty||!railMode))rescan();}
    public String floorDisplayName(int y){
        for(var floor:floors)if(floor.y()==y)return floor.name().isBlank()?floor.id():floor.name();
        return "Y "+y;
    }
    public String floorLabel(int y){return floors.stream().filter(f->f.y()==y).map(ElevatorFloor::id).findFirst().orElse("Y "+y);}
    public static String stateName(int state){return switch(state){case CLOSING->"DOORS_CLOSING";case MOVING->"MOVING";case LEVELING->"LEVELING";case OPENING->"DOORS_OPENING";case DOORS_OPEN->"DOORS_OPEN";case FAULT->"FAULT";default->"IDLE";};}

    public void rescan(){
        if(level==null||level.isClientSide||scanning)return;
        scanning=true;
        try{
            var result=ElevatorRailDiscovery.scan(level,worldPosition);
            railTop=result.top();railWarning=result.warning();railMode|=result.sawMarker();
            var discovered=new ArrayList<ElevatorFloor>();
            if(railMode){
                int ordinal=0;
                var usedIds=new HashSet<String>();for(var p:result.markers())if(level.getBlockEntity(p) instanceof ElevatorPartEntity marker&&!marker.floorId().isBlank())usedIds.add(marker.floorId().toLowerCase(Locale.ROOT));
                for(var p:result.markers()){
                    if(!(level.getBlockEntity(p) instanceof ElevatorPartEntity marker))continue;
                    ordinal++;
                    if(marker.floorId().isBlank()){int label=ordinal;while(usedIds.contains("f"+label))label++;marker.configureFloor("F"+label,"Floor "+label,true);usedIds.add("f"+label);}
                    if(!worldPosition.equals(marker.markerDrive()))marker.bindMarker(worldPosition);
                    int y=p.getY()-1;String problem="";
                    if(cabin!=null&&!hasLandingAt(y))problem="No complete matching landing doorway at this stop";
                    discovered.add(new ElevatorFloor(p,y,marker.floorId(),marker.floorName(),marker.floorEnabled(),problem.isEmpty(),problem,true));
                }
                if(discovered.isEmpty()&&railWarning.isEmpty())railWarning="No connected floor marker rails. Install a marker at every stop.";
                var ids=new HashSet<String>();
                for(var f:discovered)if(!ids.add(f.id().toLowerCase(Locale.ROOT)))railWarning="Duplicate floor ID: "+f.id();
            }else{
                int ordinal=0;
                for(int y:ElevatorNetwork.legacyFloors(this)){
                    ordinal++;var old=legacySettings.get(y);
                    discovered.add(new ElevatorFloor(worldPosition.atY(y+1),y,old==null?"F"+ordinal:old.id(),old==null?"Floor "+ordinal:old.name(),old==null||old.enabled(),true,"",false));
                }
            }
            discovered.sort(Comparator.comparingInt(ElevatorFloor::y));
            var next=List.copyOf(discovered);
            boolean changed=!next.equals(floors);floors=next;
            queue.removeIf(y->!serviceable(y)&&!(returnAndDisassemble&&y==anchor().getY()));railsDirty=false;
            if(changed)sync();
        }finally{scanning=false;}
    }
    private boolean serviceable(int y){return floors.stream().anyMatch(f->f.y()==y&&f.enabled()&&f.reachable());}
    public boolean configureFloor(int y,String id,String name,boolean enabled){
        ensureFloors();id=id.strip();name=name.strip();
        if(id.isEmpty()||id.length()>12||name.isEmpty()||name.length()>32||id.chars().anyMatch(Character::isISOControl)||name.chars().anyMatch(Character::isISOControl))return reject(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.use_a_floor_id_of_1"));
        final String label=id;
        if(floors.stream().anyMatch(f->f.y()!=y&&f.id().equalsIgnoreCase(label)))return reject(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.floor_ids_must_be_unique"));
        var floor=floors.stream().filter(f->f.y()==y).findFirst().orElse(null);if(floor==null)return reject(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.floor_marker_is_no_longer_connected"));
        if(floor.rail()){
            if(!level.hasChunkAt(floor.marker())||!(level.getBlockState(floor.marker()).getBlock() instanceof ElevatorFloorMarkerRailBlock)||!(level.getBlockEntity(floor.marker()) instanceof ElevatorPartEntity marker))return reject(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.floor_marker_is_unloaded_or_missing"));
            marker.configureFloor(id,name,enabled);
        }else legacySettings.put(y,new ElevatorFloor(floor.marker(),y,id,name,enabled,true,"",false));
        rescan();message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.floor_configuration_saved");sync();return true;
    }
    public boolean configureController(String name,int opening,int closing,int dwell){
        name=name.strip();if(name.isEmpty()||name.length()>32||name.chars().anyMatch(Character::isISOControl)||opening<0||opening>200||closing<0||closing>200||dwell<0||dwell>600)return reject(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.invalid_name_or_delay_open_close"));
        elevatorName=name;openingDelay=opening;closingDelay=closing;dwellTicks=dwell;message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.auto_door_settings_saved");sync();return true;
    }
    private boolean reject(String reason){message=reason;sync();return false;}
    private void fault(String reason){phase=FAULT;message=reason;doors(false);if(cabin!=null)cabin.setContraptionMotion(Vec3.ZERO);sync();}

    public boolean assemble(){
        if(model!=null)return activateModel();
        if(level==null||level.isClientSide||assembled())return false;
        rescan();if(railMode&&(floors.isEmpty()||!railWarning.isEmpty()))return reject(railWarning.isEmpty()?"No connected stops":railWarning);
        var contraption=new ElevatorContraption();
        if(!contraption.assemble(level,anchor()))return reject(contraption.failure());
        contraption.removeBlocksFromWorld(level,BlockPos.ZERO);
        cabin=ElevatorCabinEntity.create(level,this,contraption);
        cabin.setPos(anchor().getX(),anchor().getY(),anchor().getZ());level.addFreshEntity(cabin);
        cabinId=cabin.getUUID();offset=previous=start=target=0;phase=OPENING;elapsed=-openingDelay;
        rescan();doors(true);message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.opening");sync();return true;
    }
    public boolean requestFloor(int y){
        if(model!=null){boolean ok=model.request(y);message=model.warningText();sync();return ok;}
        if(level==null||level.isClientSide)return false;
        ensureFloors();
        if(!assembled())return reject(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.assemble_the_cabin_first"));
        if(railMode&&!railWarning.isEmpty())return reject(railWarning);
        if(!serviceable(y))return reject(floors.stream().filter(f->f.y()==y).map(f->f.enabled()?(f.warning().isEmpty()?"Floor is inaccessible":f.warning()):"Floor is disabled").findFirst().orElse("No connected marker for that floor"));
        if(pending(y))return true;
        if(phase!=MOVING&&phase!=CLOSING&&phase!=LEVELING&&Math.abs(offset-(y-anchor().getY()))<.001){phase=OPENING;elapsed=0;doors(true);message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.opening");}
        else{
            queue.add(y);if(phase==FAULT){phase=IDLE;elapsed=0;}message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.requested_s",floorLabel(y));
        }
        sync();return true;
    }
    public void returnHome(){
        if(model!=null){model.returnHome();sync();return;}
        if(!assembled())return;queue.clear();returnAndDisassemble=true;
        // Maintenance return is permitted even when the home floor is disabled.
        queue.add(anchor().getY());if(phase==FAULT)phase=IDLE;sync();
    }
    public boolean retry(){if(model!=null)return model.retry();if(phase!=FAULT)return reject(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.retry_is_available_only_after_a"));rescan();if(!railWarning.isEmpty()&&railMode)return reject(railWarning);if(cabin==null)return reject(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.cabin_is_unloaded_load_its_chunk"));phase=IDLE;elapsed=0;message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.retry_select_a_floor");sync();return true;}

    private boolean validPair(BlockPos p,Direction face){
        if(!level.hasChunkAt(p))return false;
        var lower=level.getBlockState(p);var upper=level.getBlockState(p.above());
        return lower.getBlock() instanceof ElevatorDoorBlock door&&!door.cabin()&&lower.getValue(ElevatorDoorBlock.ROW)==0&&lower.getValue(ElevatorPartBlock.FACING)==face.getOpposite()
            &&upper.is(lower.getBlock())&&upper.getValue(ElevatorDoorBlock.ROW)==1&&upper.getValue(ElevatorPartBlock.FACING)==face.getOpposite();
    }
    private BlockPos landingAt(StructureBlockInfo info,int y){
        var state=info.state();if(!(state.getBlock() instanceof ElevatorDoorBlock door)||!door.cabin())return null;
        var bottom=info.pos().below(state.getValue(ElevatorDoorBlock.ROW));var face=state.getValue(ElevatorPartBlock.FACING);
        var p=anchor().atY(y).offset(bottom).relative(face);return validPair(p,face)?p:null;
    }
    private BlockPos landingForDoor(StructureBlockInfo info){return cabin==null||Math.abs(offset-Math.rint(offset))>=.001?null:landingAt(info,currentFloor());}
    private boolean hasLandingAt(int y){
        boolean hasDoor=false;
        for(var info:cabin.getContraption().getBlocks().values())if(info.state().getBlock() instanceof ElevatorDoorBlock){hasDoor=true;if(landingAt(info,y)!=null)return true;}
        return !hasDoor; // Freight platforms without doors remain supported.
    }
    private boolean canOpenDoors(){return cabin!=null&&(phase==IDLE||phase==OPENING||phase==DOORS_OPEN)&&Math.abs(offset-Math.rint(offset))<.001;}
    public boolean allowsLanding(BlockPos pos){
        if(model!=null)return model.allows(pos);
        if(!canOpenDoors()||cabin.isRemoved())return false;
        for(var info:cabin.getContraption().getBlocks().values())if(info.state().getBlock() instanceof ElevatorDoorBlock&&info.state().getValue(ElevatorDoorBlock.OPEN)&&pos.equals(landingForDoor(info)))return true;
        return false;
    }
    private void doors(boolean open){
        boolean changed=false,soundOpen=false,solidSound=false;
        if(cabin!=null){var c=cabin.getContraption();
            for(var info:new ArrayList<>(c.getBlocks().values()))if(info.state().getBlock() instanceof ElevatorDoorBlock door){
                boolean selected=open&&canOpenDoors()&&landingForDoor(info)!=null;
                if(info.state().getValue(ElevatorDoorBlock.OPEN)!=selected){
                    cabin.setBlock(info.pos(),new StructureBlockInfo(info.pos(),info.state().setValue(ElevatorDoorBlock.OPEN,selected),info.nbt()));
                    changed=true;soundOpen|=selected;if(selected&&door.solid())solidSound=true;
                }
            }
            if(changed)c.invalidateColliders();
        }
        if(changed)ElevatorSounds.play(level,BlockPos.containing(anchor().getX(),anchor().getY()+offset+1,anchor().getZ()),soundOpen,solidSound);
        landings();
    }
    private void landings(){
        Set<BlockPos> selected=new HashSet<>();
        if(canOpenDoors())for(var info:cabin.getContraption().getBlocks().values())if(info.state().getBlock() instanceof ElevatorDoorBlock&&info.state().getValue(ElevatorDoorBlock.ROW)==0&&info.state().getValue(ElevatorDoorBlock.OPEN)){
            var p=landingForDoor(info);if(p!=null)selected.add(p);
        }
        for(var p:ElevatorNetwork.landings(this))ElevatorDoorBlock.setOpen(level,p,selected.contains(p),false);
    }
    private boolean doorsClosed(){
        for(var info:cabin.getContraption().getBlocks().values())if(info.state().getBlock() instanceof ElevatorDoorBlock&&info.state().getValue(ElevatorDoorBlock.OPEN))return false;
        for(var p:ElevatorNetwork.landings(this)){
            var state=level.getBlockState(p);if(state.getValue(ElevatorDoorBlock.OPEN))return false;
            if(level.getBlockEntity(p) instanceof ElevatorPartEntity part&&!part.fullyClosed())return false;
        }
        return true;
    }
    private boolean doorwayObstructed(){
        if(cabin==null||Math.abs(offset-Math.rint(offset))>=.001)return false;
        for(var info:cabin.getContraption().getBlocks().values())if(info.state().getBlock() instanceof ElevatorDoorBlock&&info.state().getValue(ElevatorDoorBlock.ROW)==0){
            var p=landingForDoor(info);if(p==null)continue;
            var closed=info.state().setValue(ElevatorDoorBlock.OPEN,false);
            AABB box=closed.getShape(level,anchor()).bounds().move(anchor().getX()+info.pos().getX(),anchor().getY()+offset+info.pos().getY(),anchor().getZ()+info.pos().getZ()).expandTowards(0,1,0).inflate(.08);
            var landing=level.getBlockState(p).setValue(ElevatorDoorBlock.OPEN,false);
            AABB outer=landing.getShape(level,p).bounds().move(p).expandTowards(0,1,0).inflate(.08);
            if(!level.getEntitiesOfClass(LivingEntity.class,box,e->e.isAlive()&&!e.isSpectator()).isEmpty()||!level.getEntitiesOfClass(LivingEntity.class,outer,e->e.isAlive()&&!e.isSpectator()).isEmpty())return true;
        }
        return false;
    }
    public static int travelDuration(double distance){return Math.max(15,(int)Math.ceil(Math.abs(distance)/.13));}
    private boolean clearPath(double destination){
        if(cabin==null)return false;
        AABB bounds=cabin.getContraption().bounds;BlockPos a=anchor();
        int low=(int)Math.floor(Math.min(offset,destination)),high=(int)Math.ceil(Math.max(offset,destination));
        BlockState first=null;
        for(int y=1;y<=high+1;y++){
            var p=worldPosition.above(y);
            if(!level.hasChunkAt(p)){message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.guide_rail_chunk_unloaded");return false;}
            var state=level.getBlockState(p);
            if(!ElevatorGuideRailBlock.rail(state)){message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.guide_rail_missing_at_y_s",p.getY());return false;}
            if(first==null)first=state;
            if(railMode&&!ElevatorGuideRailBlock.compatible(first,state)){message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.the_guide_rail_at_y_s",p.getY());return false;}
        }
        for(int x=(int)bounds.minX;x<bounds.maxX;x++)for(int z=(int)bounds.minZ;z<bounds.maxZ;z++)for(int y=low+(int)bounds.minY;y<high+bounds.maxY;y++){
            var p=a.offset(x,y,z);
            if(!level.hasChunkAt(p)){message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.cabin_shaft_chunk_unloaded");return false;}
            if(!level.getBlockState(p).getCollisionShape(level,p).isEmpty()){message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.shaft_obstructed_at_s_clear_the",p.toShortString());return false;}
        }
        return true;
    }
    private void requeueFirst(int y){var old=new ArrayList<>(queue);queue.clear();queue.add(y);queue.addAll(old);}
    public static void tick(Level level,BlockPos pos,BlockState state,ElevatorDriveEntity d){
        d.previous=d.offset;
        if(d.model!=null){if(!level.isClientSide){if(d.railsDirty||level.getGameTime()%20==0)d.rescan();
            if(level.getGameTime()%20==0)ElevatorBanks.get((net.minecraft.server.level.ServerLevel)level).applyStyle(d);
            d.tickModel();}return;}
        if(d.cabin!=null&&d.cabin.isRemoved())d.cabin=null;
        if(!level.isClientSide&&(d.railsDirty||level.getGameTime()%20==0))d.rescan();
        if(d.cabin==null&&d.cabinId!=null&&!level.isClientSide&&level instanceof ServerLevel server&&server.getEntity(d.cabinId) instanceof ControlledContraptionEntity entity)d.attach(entity);
        if(d.cabin==null)return;
        if(d.phase==MOVING){
            if(!level.isClientSide&&d.railMode&&(!d.railWarning.isEmpty()||!d.serviceable(d.targetFloor())&&!(d.returnAndDisassemble&&d.targetFloor()==d.anchor().getY()))){d.fault(d.railWarning.isEmpty()?"Destination marker removed, disabled or inaccessible":d.railWarning);return;}
            double t=Math.min(1,(++d.elapsed)/(double)Math.max(1,d.duration));double next=d.start+(d.target-d.start)*t*t*(3-2*t);
            if(!level.isClientSide&&!d.doorsClosed()){d.fault(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.door_interlock_interrupted_travel"));return;}
            if(!level.isClientSide&&!d.clearPath(next)){d.fault(d.message);return;}
            d.offset=next;
            if(t>=1){d.offset=d.target;d.phase=LEVELING;d.elapsed=0;if(!level.isClientSide){d.message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.leveling_at_s",d.floorLabel(d.targetFloor()));d.sync();}}
        }else if(!level.isClientSide){
            switch(d.phase){
                case LEVELING -> {
                    if(!d.clearPath(d.offset)||d.railMode&&!d.hasLandingAt(d.currentFloor())){d.fault(com.sanctuarygames.createtransit.TransitText.plain(d.message).startsWith("Shaft")?d.message:com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.landing_doorway_invalid_at_destination"));break;}
                    if(++d.elapsed>=Math.max(5,d.openingDelay)){d.phase=OPENING;d.elapsed=0;d.message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.opening");d.doors(true);d.sync();}
                }
                case OPENING -> {if(++d.elapsed>=40){d.phase=DOORS_OPEN;d.elapsed=0;d.message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.doors_open");d.sync();}}
                case DOORS_OPEN -> {if(++d.elapsed>=d.dwellTicks){d.phase=IDLE;d.elapsed=0;d.message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.ready");d.sync();}}
                case CLOSING -> {
                    if(d.doorwayObstructed()){d.requeueFirst(d.targetFloor());d.phase=OPENING;d.elapsed=0;d.message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.doorway_obstructed_reopening");d.doors(true);d.sync();break;}
                    if(++d.elapsed>=40+d.closingDelay){
                        if(!d.doorsClosed()){d.fault(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.doors_did_not_confirm_closed"));break;}
                        if(d.clearPath(d.target)){d.start=d.offset;d.duration=travelDuration(d.target-d.start);d.elapsed=0;d.phase=MOVING;d.message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.travelling");d.sync();}
                        else d.fault(d.message);
                    }
                }
                case IDLE -> {
                    if(d.returnAndDisassemble&&Math.abs(d.offset)<.001){d.disassemble();return;}
                    if(!d.queue.isEmpty()){
                        int y=d.queue.iterator().next();
                        if(!d.serviceable(y)&&!(d.returnAndDisassemble&&y==d.anchor().getY())){d.queue.remove(y);break;}
                        if(d.doorwayObstructed()){d.message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.doorway_obstructed_clear_the_entrance");break;}
                        d.queue.remove(y);d.target=y-d.anchor().getY();
                        if(Math.abs(d.offset-d.target)>.001){d.phase=CLOSING;d.elapsed=0;d.message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.closing");d.doors(false);d.sync();}
                    }
                    if(level.getGameTime()%20==0)d.doors(true);
                }
                case FAULT -> d.doors(false);
                default -> {}
            }
        }
        BlockPos a=d.anchor();d.motionTick=level.getGameTime();d.cabin.xo=a.getX();d.cabin.yo=a.getY()+d.previous;d.cabin.zo=a.getZ();
        d.cabin.setContraptionMotion(new Vec3(0,d.offset-d.previous,0));d.cabin.setPos(a.getX(),a.getY()+d.offset,a.getZ());
        d.setChanged();if(!level.isClientSide&&d.phase==MOVING&&level.getGameTime()%5==0)d.sync();
    }
    public void disassemble(){
        if(model!=null){model.deactivate();return;}
        if(level==null||level.isClientSide||cabin==null)return;
        offset=Math.rint(offset);previous=offset;phase=CLOSING;doors(false);var a=anchor();
        cabin.setPos(a.getX(),a.getY()+offset,a.getZ());cabin.disassemble();cabin=null;cabinId=null;
        queue.clear();returnAndDisassemble=false;phase=IDLE;message=com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.disassembled");sync();
    }
    public void openPanel(Player player){openPanel(player,true,worldPosition);}
    public void openPassengerPanel(Player player){openPanel(player,false,worldPosition);}
    public void openPanel(Player p,boolean configuration,BlockPos access){
        if(p instanceof ServerPlayer player){rescan();var snapshot=ElevatorMenu.snapshot(this,player,configuration,access);player.openMenu(new SimpleMenuProvider((id,inv,who)->new ElevatorMenu(id,inv,this,configuration,access),com.sanctuarygames.createtransit.TransitText.tr("container.createtransit.elevator_controller")),buf->{buf.writeBlockPos(worldPosition);buf.writeBlockPos(access);buf.writeBoolean(configuration);buf.writeNbt(snapshot);});}
    }
    public CompoundTag snapshot(){
        var t=new CompoundTag();t.putString("ElevatorName",elevatorName);t.putUUID("Identifier",identifier);
        t.putInt("Current",currentFloor());t.putInt("Target",targetFloor());t.putInt("Phase",phase);t.putBoolean("Assembled",assembled());
        t.putString("DoorStatus",model!=null&&model.entity()!=null?model.entity().doorStatus():phase==DOORS_OPEN?"Open":phase==OPENING?"Opening":phase==CLOSING?"Closing":"Closed");t.putDouble("CabinY",anchor().getY()+offset);t.putBoolean("Aligned",Math.abs(offset-Math.rint(offset))<.001);
        /* Message/RailWarning stay plain English for clients on older versions; the *Key fields translate. */t.putString("Message",com.sanctuarygames.createtransit.TransitText.plain(message));t.putString("MessageKey",message);t.putString("RailWarning",com.sanctuarygames.createtransit.TransitText.plain(railWarning));t.putString("RailWarningKey",railWarning);t.putBoolean("RailMode",railMode);t.putInt("RailTop",railTop);
        t.putInt("OpenDelay",openingDelay);t.putInt("CloseDelay",closingDelay);t.putInt("Dwell",dwellTicks);
        if(level instanceof net.minecraft.server.level.ServerLevel sl)t.put("RadioStations",com.sanctuarygames.createtransit.radio.RadioStations.get(sl).snapshot());if(model!=null)t.put("ModelCabin",model.save());t.putIntArray("Queue",queue.stream().mapToInt(Integer::intValue).toArray());var list=new ListTag();floors.forEach(f->list.add(f.save()));t.put("Stops",list);return t;
    }
    public boolean isAttachedTo(AbstractContraptionEntity entity){return entity==cabin;}
    public void attach(ControlledContraptionEntity entity){if(entity.getContraption() instanceof ElevatorContraption){cabin=entity;cabinId=entity.getUUID();entity.setRotationAxis(Direction.Axis.Y);railsDirty=true;}}
    public void onStall(){fault(com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.travel_interrupted"));} public boolean isValid(){return !isRemoved();} public BlockPos getBlockPosition(){return worldPosition;}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){
        super.saveAdditional(t,r);if(model!=null)t.put("ModelCabin",model.save());if(cabinId!=null)t.putUUID("Cabin",cabinId);
        t.putDouble("Offset",offset);t.putDouble("Start",start);t.putDouble("Target",target);t.putInt("Elapsed",elapsed);t.putInt("Duration",duration);t.putInt("Phase",phase);
        t.putIntArray("Queue",queue.stream().mapToInt(Integer::intValue).toArray());t.putBoolean("Return",returnAndDisassemble);t.putString("Message",message);
        t.putInt("ElevatorSystem",2);t.putUUID("Identifier",identifier);t.putString("ElevatorName",elevatorName);t.putBoolean("RailMode",railMode);t.putInt("RailTop",railTop);t.putString("RailWarning",railWarning);
        t.putInt("OpenDelay",openingDelay);t.putInt("CloseDelay",closingDelay);t.putInt("Dwell",dwellTicks);
        var stops=new ListTag();floors.forEach(f->stops.add(f.save()));t.put("DiscoveredStops",stops);
        var legacy=new ListTag();legacySettings.values().forEach(f->legacy.add(f.save()));t.put("LegacySettings",legacy);
    }
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){
        super.loadAdditional(t,r);if(t.contains("ModelCabin")){if(model==null)model=new com.sanctuarygames.createtransit.elevator.model.ModelCabinController(this);model.load(t.getCompound("ModelCabin"));}UUID id=t.hasUUID("Cabin")?t.getUUID("Cabin"):null;if(!Objects.equals(cabinId,id))cabin=null;cabinId=id;
        offset=previous=t.getDouble("Offset");start=t.getDouble("Start");target=t.getDouble("Target");elapsed=t.getInt("Elapsed");duration=Math.max(1,t.getInt("Duration"));phase=Math.clamp(t.getInt("Phase"),0,6);
        queue.clear();for(int f:t.getIntArray("Queue")){queue.add(f);}returnAndDisassemble=t.getBoolean("Return");message=t.getString("Message");
        if(t.hasUUID("Identifier"))identifier=t.getUUID("Identifier");if(t.contains("ElevatorName"))elevatorName=t.getString("ElevatorName");
        railMode=t.getBoolean("RailMode");railTop=t.getInt("RailTop");railWarning=t.getString("RailWarning");
        if(t.contains("OpenDelay"))openingDelay=Math.clamp(t.getInt("OpenDelay"),0,200);if(t.contains("CloseDelay"))closingDelay=Math.clamp(t.getInt("CloseDelay"),0,200);if(t.contains("Dwell"))dwellTicks=Math.clamp(t.getInt("Dwell"),0,600);
        var stops=t.getList("DiscoveredStops",10);var restored=new ArrayList<ElevatorFloor>();for(int i=0;i<stops.size();i++)restored.add(ElevatorFloor.load(stops.getCompound(i)));floors=List.copyOf(restored);
        legacySettings.clear();var legacy=t.getList("LegacySettings",10);for(int i=0;i<legacy.size();i++){var f=ElevatorFloor.load(legacy.getCompound(i));legacySettings.put(f.y(),f);}railsDirty=true;
    }
}
