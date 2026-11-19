package com.sanctuarygames.createtransit.elevator;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** The server owns all settings; passenger panels expose the same stop list read-only. */
public final class ElevatorMenu extends AbstractContainerMenu {
    public final BlockPos drivePos,accessPos;
    public final boolean configuration;
    public int[] floors;
    private final ElevatorDriveEntity drive;
    private static final String CONFIG_CLIPBOARD="CsuElevatorConfig";
    private final Player player;
    private CompoundTag state,lastSent;
    private String lastWarning="";
    private long lastActionTick=Long.MIN_VALUE;
    public ElevatorMenu(int id,Inventory inv,RegistryFriendlyByteBuf buf){this(id,inv,buf.readBlockPos(),buf.readBlockPos(),buf.readBoolean(),buf.readNbt(),null);}
    public ElevatorMenu(int id,Inventory inv,ElevatorDriveEntity drive,boolean configuration,BlockPos access){this(id,inv,drive.getBlockPos(),access,configuration,snapshot(drive,inv.player,configuration,access),drive);}
    private ElevatorMenu(int id,Inventory inv,BlockPos pos,BlockPos access,boolean configuration,CompoundTag snapshot,ElevatorDriveEntity drive){
        super(ElevatorRegistry.MENU.get(),id);drivePos=pos;accessPos=access;this.configuration=configuration;this.drive=drive;player=inv.player;accept(snapshot==null?new CompoundTag():snapshot);
    }
    static CompoundTag snapshot(ElevatorDriveEntity drive,Player player,boolean setup,BlockPos access){
        if(!(player.level() instanceof net.minecraft.server.level.ServerLevel level)
                ||!(level.getBlockState(access).getBlock() instanceof ElevatorCallBlock call))return drive.snapshot();
        var banks=ElevatorBanks.get(level);
        var result=new CompoundTag();
        result.putInt("BankMode",call.destinationSelector()?2:1);
        if(call.destinationSelector()){
            var floors=new net.minecraft.nbt.ListTag();
            banks.destinations(level,access).forEach(f->floors.add(f.save()));
            result.put("Stops",floors);
            result.putString("Assignment",banks.assignment(level,access,player.getUUID()));
            result.putBoolean("ShowFloorNames",banks.showFloorNames(level,access));
        }else{
            result.put("BankAppearance",banks.appearance(level,access));
            result.put("HallCallAppearance",banks.callAppearance(level,access));
            result.putString("BankCars",String.join("   |   ",banks.preview(level,access)));
            if(player instanceof ServerPlayer server)result.putString("MergePreview",banks.mergePreview(level,access,server));
        }
        return result;
    }
    public CompoundTag state(){return state;}
    public int bankMode(){return state.getInt("BankMode");}
    private List<ElevatorFloor> cachedStops=List.of();
    public List<ElevatorFloor> stops(){return cachedStops;}
    public void accept(CompoundTag tag){state=tag.copy();var list=state.getList("Stops",10);var result=new ArrayList<ElevatorFloor>();for(int i=0;i<list.size();i++)result.add(ElevatorFloor.load(list.getCompound(i)));cachedStops=List.copyOf(result);floors=cachedStops.stream().mapToInt(ElevatorFloor::y).toArray();}
    public int status(int i){return state.getInt(i==0?"Current":i==1?"Target":"Phase");}
    @Override public void broadcastChanges(){
        super.broadcastChanges();if(bankMode()!=0&&player.level().getGameTime()%5!=0)return;if(drive!=null&&player instanceof ServerPlayer server){var snapshot=snapshot(drive,player,configuration,accessPos);if(configuration&&bankMode()==0)snapshot.put("ConfigClipboard",player.getPersistentData().getCompound(CONFIG_CLIPBOARD).copy());String warning=snapshot.getString("RailWarning");if(snapshot.getInt("Phase")==ElevatorDriveEntity.FAULT)warning=snapshot.getString("Message");if(!warning.equals(lastWarning)){lastWarning=warning;if(!warning.isBlank())ElevatorFeedback.error(player,warning);}if(!snapshot.equals(lastSent)){accept(snapshot);PacketDistributor.sendToPlayer(server,new ElevatorPackets.Snapshot(containerId,snapshot));lastSent=snapshot.copy();}}
    }
    public boolean action(Player sender,ElevatorPackets.Action action){
        if(drive==null||sender!=player||sender.containerMenu!=this||action.menuId()!=containerId||!stillValid(sender))return false;
        long tick=sender.level().getGameTime();if(tick==lastActionTick)return false;lastActionTick=tick;
        if(bankMode()!=0&&sender instanceof ServerPlayer server){
            var level=(net.minecraft.server.level.ServerLevel)sender.level();
            var banks=ElevatorBanks.get(level);
            if(bankMode()==2){
                boolean accepted=action.operation()==0&&banks.destination(level,accessPos,action.floorY(),server);
                if(!accepted)ElevatorFeedback.error(sender,"No bank car serves that destination.");
                return accepted;
            }
            if(!configuration||!sender.mayBuild()||!level.mayInteract(sender,accessPos))return false;
            int operation=action.operation();
            if(operation>=17&&operation<=19){
                String error=operation==19?(banks.select(level,accessPos,server)?"":"No linked bank at this panel"):banks.link(level,accessPos,server,operation==17);
                if(!error.isEmpty())ElevatorFeedback.error(sender,error);
                else sender.displayClientMessage(net.minecraft.network.chat.Component.literal("Bank selection saved"),true);
                return error.isEmpty();
            }
            if(operation>=24&&operation<=27){
                String error=banks.mergeAction(level,accessPos,server,operation);
                if(!error.isEmpty())ElevatorFeedback.error(sender,error);
                return error.isEmpty();
            }
            return banks.customize(level,accessPos,operation);
        }
        if(action.operation()==0){boolean ok=drive.requestFloor(action.floorY());if(!ok)chatError();return ok;}
        if(!configuration||!sender.mayBuild()||!sender.level().mayInteract(sender,drivePos))return false;
        boolean result=switch(action.operation()){
            case 1 -> drive.configureFloor(action.floorY(),action.first(),action.second(),action.enabled());
            case 2 -> drive.configureController(action.first(),action.opening(),action.closing(),action.dwell());
            case 3 -> {drive.rescan();yield true;}
            case 4 -> drive.assemble();
            case 5 -> drive.retry();
            case 6 -> {drive.returnHome();yield true;}
            case 7 -> drive.configureModel(action.first(),action.second());
            case 8 -> drive.activateModel();
            case 9 -> drive.installModelLanding(sender,action.floorY());
            case 10 -> drive.modelPolicy(action.floorY(),action.enabled(),action.opening()!=0,action.closing(),action.dwell());
            case 11 -> drive.deactivateModel();
            case 12,13,14 -> {try{var stations=com.sanctuarygames.createtransit.radio.RadioStations.get((net.minecraft.server.level.ServerLevel)sender.level());if(action.operation()==12)stations.add(action.first(),action.second());else if(action.operation()==14)stations.remove(action.second());else if(drive.model()!=null){if(action.first().equals("station")&&!action.second().isEmpty()&&stations.station(action.second())==null)throw new IllegalArgumentException("Station does not exist");drive.model().radio.configure(action.first(),action.second());drive.setChanged();}else throw new IllegalArgumentException("Configure a model cabin first");yield true;}catch(IllegalArgumentException ex){ElevatorFeedback.error(sender,ex.getMessage());yield false;}}
            case 15 -> {
                if(drive.model()==null)yield false;
                sender.getPersistentData().put(CONFIG_CLIPBOARD,drive.model().copySettings());
                sender.displayClientMessage(net.minecraft.network.chat.Component.literal("Elevator configuration copied"),true);
                yield true;
            }
            case 16 -> drive.pasteModelSettings(sender.getPersistentData().getCompound(CONFIG_CLIPBOARD));
            default -> false;
        };if(!result)chatError();broadcastChanges();return result;
    }
    private void chatError(){var t=drive.snapshot();String message=t.getString("Message");if(message.isBlank())message="Request rejected. Check the selected floor and elevator setup.";ElevatorFeedback.error(player,message);lastWarning=message;}
    @Override public boolean clickMenuButton(Player sender,int index){return bankMode()==0&&drive!=null&&stillValid(sender)&&index>=0&&index<floors.length&&drive.requestFloor(floors[index]);}
    @Override public boolean stillValid(Player p){
        if(drive==null)return true;
        if(bankMode()!=0)return drive.getLevel()==p.level()&&p.distanceToSqr(Vec3.atCenterOf(accessPos))<=64
                &&p.level().hasChunkAt(accessPos)&&p.level().getBlockState(accessPos).getBlock() instanceof ElevatorCallBlock call
                &&call.destinationSelector()==(bankMode()==2);
        if(drive.isRemoved()||drive.getLevel()!=p.level())return false;
        if(p.distanceToSqr(Vec3.atCenterOf(accessPos))<=64){
            if(accessPos.equals(drivePos))return true;
            return p.level().hasChunkAt(accessPos)&&((p.level().getBlockState(accessPos).getBlock() instanceof ElevatorFloorMarkerRailBlock&&ElevatorGuideRailBlock.drive(p.level(),accessPos)==drive)||(p.level().getBlockEntity(accessPos) instanceof com.sanctuarygames.createtransit.elevator.model.ModelLandingEntity e&&drivePos.equals(e.markerDrive())));
        }
        if(drive.model()!=null&&drive.model().entity()!=null&&drive.model().entity().getBoundingBox().inflate(1).contains(p.position()))return true;
        return drive.cabin()!=null&&drive.cabin().getBoundingBox().inflate(1).contains(p.position());
    }
    @Override public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}
}
