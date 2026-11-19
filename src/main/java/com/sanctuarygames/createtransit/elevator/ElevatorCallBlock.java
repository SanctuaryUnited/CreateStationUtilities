package com.sanctuarygames.createtransit.elevator;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class ElevatorCallBlock extends ElevatorPartBlock {
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED,REQUESTED=BooleanProperty.create("requested");
    private final boolean destinationSelector;
    public ElevatorCallBlock(Properties p){this(p,false);}
    public ElevatorCallBlock(Properties p,boolean destinationSelector){super(p,Kind.CALL);this.destinationSelector=destinationSelector;registerDefaultState(defaultBlockState().setValue(POWERED,false).setValue(REQUESTED,false));}
    public static final int DESTINATION_ROWS=6;
    public static double screenX(BlockState state,BlockHitResult hit){
        var point=hit.getLocation();var pos=hit.getBlockPos();
        double x=point.x-pos.getX(),z=point.z-pos.getZ();
        return switch(state.getValue(FACING)){case EAST->z;case SOUTH->1-x;case WEST->1-z;default->x;};
    }
    public static int destinationSlot(double x,double y){
        int column=x>=.23&&x<=.48?0:x>=.52&&x<=.77?1:-1;
        int row=-1;
        for(int i=0;i<3;i++)if(Math.abs(y-destinationCenter(i*2))<=.054){row=i;break;}
        return row<0||column<0?-1:row*2+column;
    }
    public static int callDirection(BlockState state,BlockHitResult hit,ElevatorPartEntity panel){
        if(hit.getDirection()!=state.getValue(FACING))return 0;
        double x=screenX(state,hit),y=hit.getLocation().y-hit.getBlockPos().getY();
        boolean single=panel.callLayout!=0;
        int direction=single?(panel.callLayout==1?1:-1):y>.53125?1:-1;
        double center=single?.5:direction>0?.668:.394;
        if(panel.callFace==ElevatorPartEntity.CallFace.DIGITAL_TOUCH_PANEL)return direction;
        double dx=Math.abs(x-.5),dy=Math.abs(y-center);
        if(panel.callFace.round())return dx*dx+dy*dy<=.117*.117?direction:0;
        return dx<=.137&&dy<=.104?direction:0;
    }
    public static double destinationCenter(int slot){return .658-(slot/2)*.127;}
    public static boolean screenHit(BlockState state,BlockHitResult hit){
        double x=screenX(state,hit),y=hit.getLocation().y-hit.getBlockPos().getY();
        return hit.getDirection()==state.getValue(FACING)&&x>=.20&&x<=.80&&y>=.20&&y<=.86;
    }
    public static void scrollDestination(net.minecraft.server.level.ServerLevel level,BlockPos pos,int delta){
        if(Math.abs(delta)!=1||!(level.getBlockEntity(pos) instanceof ElevatorPartEntity panel)||!panel.destinationDisplay.isEmpty())return;
        updateDestinations(level,pos,panel,delta);
        panel.sync();
    }
    private static void updateDestinations(net.minecraft.server.level.ServerLevel level,BlockPos pos,ElevatorPartEntity panel,int delta){
        var banks=ElevatorBanks.get(level);
        var stops=banks.destinations(level,pos);
        panel.destinationPages=Math.max(1,(stops.size()+DESTINATION_ROWS-1)/DESTINATION_ROWS);
        panel.destinationPage=Math.clamp(panel.destinationPage+delta,0,panel.destinationPages-1);
        var page=stops.stream().skip((long)panel.destinationPage*DESTINATION_ROWS).limit(DESTINATION_ROWS).toList();
        boolean names=banks.showFloorNames(level,pos);
        panel.destinationPreview=String.join("\n",page.stream().map(f->(f.enabled()?"":"\u0001")+f.id()+(names?"\t"+f.name():"")).toList());
        panel.destinationFloors=page.stream().mapToInt(ElevatorFloor::y).toArray();
    }
    public boolean destinationSelector(){return destinationSelector;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(POWERED,REQUESTED);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){var face=c.getClickedFace();return face.getAxis().isHorizontal()?defaultBlockState().setValue(FACING,face):null;}
    private static Map<ElevatorDriveEntity,Integer> targets(Level level,BlockPos pos){
        var result=new LinkedHashMap<ElevatorDriveEntity,Integer>();
        if(!(level.getBlockEntity(pos) instanceof ElevatorPartEntity button))return result;
        if(!button.callMarkers().isEmpty()){
            for(var markerPos:button.callMarkers()){
                if(!level.hasChunkAt(markerPos))continue;
                var state=level.getBlockState(markerPos);ElevatorDriveEntity drive=null;
                if(state.getBlock() instanceof ElevatorFloorMarkerRailBlock)drive=ElevatorGuideRailBlock.drive(level,markerPos);
                else if(state.getBlock() instanceof ElevatorFloorMarkerBlock&&level.getBlockEntity(markerPos) instanceof ElevatorPartEntity marker&&ElevatorLinking.validDrive(level,markerPos,marker.markerDrive()))drive=(ElevatorDriveEntity)level.getBlockEntity(marker.markerDrive());
                if(drive!=null){drive.ensureFloors();int y=markerPos.getY()-1;if(drive.floorList().stream().anyMatch(f->f.y()==y&&f.enabled()&&f.reachable()))result.putIfAbsent(drive,y);}
            }
            return result; // Configured targets never fall back to a nearby shaft.
        }
        if(button.markerDrive()==null){var automatic=ElevatorNetwork.automaticCall(level,pos);if(automatic!=null){if(automatic.floor().enabled()&&automatic.floor().reachable())result.put(automatic.drive(),automatic.floor().y());return result;}}
        ElevatorDriveEntity drive;
        if(button.markerDrive()!=null)drive=level.hasChunkAt(button.markerDrive())&&level.getBlockEntity(button.markerDrive()) instanceof ElevatorDriveEntity d?d:null;
        else drive=ElevatorNetwork.forCall(level,pos);
        if(drive!=null&&!(button.markerDrive()==null&&drive.model()!=null)){var floor=ElevatorNetwork.callFloor(drive,pos);if(floor!=null&&floor.enabled()&&floor.reachable())result.put(drive,floor.y());}
        return result;
    }
    /** Re-evaluate only linked shafts. The compact shape is shared by selection and rendering. */
    public static void refreshPanel(Level level,BlockPos pos,ElevatorPartEntity be){
        if(level instanceof net.minecraft.server.level.ServerLevel server){
            var banks=ElevatorBanks.get(server);
            boolean destination=level.getBlockState(pos).getBlock() instanceof ElevatorCallBlock call&&call.destinationSelector();
            if(destination||banks.hasService(server,pos)){
                int directions=destination?0:banks.directions(server,pos), pending=destination?0:banks.pending(server,pos);
                var style=banks.appearance(server,pos);
                var hall=banks.callAppearance(server,pos);
                var face=ElevatorPartEntity.CallFace.valueOf(hall.getString("Face"));
                boolean callOptionsChanged=be.callFace!=face||be.directionalCallLights!=hall.getBoolean("Directional")||be.callVolume!=hall.getInt("Volume");
                be.callFace=face;be.directionalCallLights=hall.getBoolean("Directional");be.callVolume=hall.getInt("Volume");
                int layout=directions==1?1:directions==2?2:0;
                boolean disabled=destination?banks.menuDrive(server,pos,false)==null:directions==0;
                var displayStyle=style.isEmpty()?be.callStyle:com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.IndicatorStyle.valueOf(style.getString("Button"));
                boolean up=(pending&1)!=0,down=(pending&2)!=0;
                boolean changed=callOptionsChanged||be.callLayout!=layout||be.callDisabled!=disabled||be.callFault||be.callStyle!=displayStyle||be.callUp!=up||be.callDown!=down;
                if(destination){
                    String display=banks.recentAssignment(server,pos);
                    String preview=be.destinationPreview;
                    int pages=be.destinationPages,page=be.destinationPage;
                    int[] floors=be.destinationFloors;
                    if(level.getGameTime()%20==0||disabled!=be.callDisabled)updateDestinations(server,pos,be,0);
                    changed|=!display.equals(be.destinationDisplay)||!preview.equals(be.destinationPreview)
                        ||pages!=be.destinationPages||page!=be.destinationPage||!Arrays.equals(floors,be.destinationFloors);
                    be.destinationDisplay=display;
                }
                be.callLayout=layout;be.callDisabled=disabled;be.callFault=false;be.callStyle=displayStyle;be.callUp=up;be.callDown=down;
                if(!style.isEmpty()&&(be.paint()!=style.getInt("Paint")||be.accent()!=style.getInt("Accent")))
                    be.setColors(style.getInt("Paint"),style.getInt("Accent"));
                else if(changed)be.sync();
                return;
            }
        }
        var ts=targets(level,pos);boolean up=false,down=false,fault=false;var appearance=ts.keySet().stream().filter(d->d.model()!=null).findFirst().map(d->d.model().config().sideIndicatorStyle()).orElse(com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.IndicatorStyle.PIXEL_MATRIX);
        for(var e:ts.entrySet()){int y=e.getValue();var floors=e.getKey().floorList().stream().filter(f->f.enabled()&&f.reachable()).toList();up|=floors.stream().anyMatch(f->f.y()>y);down|=floors.stream().anyMatch(f->f.y()<y);var state=e.getKey().model()==null?null:e.getKey().model().state();fault|=state!=null&&state.state()==com.sanctuarygames.createtransit.elevator.model.CabinMotionController.State.FAULT;}
        int layout=up&&!down?1:down&&!up?2:0;boolean disabled=ts.isEmpty()||!up&&!down;
        boolean queued=pending(level,pos),cu=queued&&be.callUp,cd=queued&&be.callDown;
        if(be.callStyle!=appearance||be.callLayout!=layout||be.callDisabled!=disabled||be.callFault!=fault||be.callUp!=cu||be.callDown!=cd){be.callStyle=appearance;be.callLayout=layout;be.callDisabled=disabled;be.callFault=fault;be.callUp=cu;be.callDown=cd;be.sync();}
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){if(destinationSelector)return turn(box(3,3,15,13,14,16),s.getValue(FACING));int layout=l.getBlockEntity(p) instanceof ElevatorPartEntity be?be.callLayout:0;return turn(box(5,layout==0?4:5,15,11,layout==0?13:11,16),s.getValue(FACING));}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    public static int call(Level level,BlockPos pos){
        return call(level,pos,0);
    }
    private static int call(Level level,BlockPos pos,int direction){
        if(!(level instanceof net.minecraft.server.level.ServerLevel server))return 0;
        if(level.getBlockState(pos).getBlock() instanceof ElevatorCallBlock call&&call.destinationSelector())return 0;
        var banks=ElevatorBanks.get(server);
        if(banks.hasService(server,pos))return bankCall(server,pos,(banks.directions(server,pos)&1)!=0?1:-1)?1:0;
        int count=0;
        for(var entry:targets(level,pos).entrySet()){
            var drive=entry.getKey();if(!drive.assembled()&&!drive.assemble())continue;
            var model=drive.model();
            if(drive.pending(entry.getValue())&&(model==null||model.state()==null||!model.state().ambient()))continue;
            if(drive.requestFloor(entry.getValue()))count++;
        }
        if(count>0&&level.getBlockEntity(pos) instanceof ElevatorPartEntity be){int pressed=direction==0?(be.callLayout==2?-1:1):direction;
            be.callUp|=pressed>0;be.callDown|=pressed<0;be.callPressedDirection=pressed;
            be.callPressedUntil=level.getGameTime()+4;be.sync();playPress(level,pos,be);}
        if(count>0){var state=level.getBlockState(pos);if(state.getBlock() instanceof ElevatorCallBlock)level.setBlock(pos,state.setValue(REQUESTED,true),Block.UPDATE_CLIENTS);}
        return count;
    }
    private static boolean bankCall(net.minecraft.server.level.ServerLevel level,BlockPos pos,int direction){
        var banks=ElevatorBanks.get(level);
        int mask=direction>0?1:2;
        if((banks.pending(level,pos)&mask)!=0)return true;
        if(!banks.request(level,pos,direction,null,null))return false;
        if(level.getBlockEntity(pos) instanceof ElevatorPartEntity be){
            be.callPressedDirection=direction;be.callPressedUntil=level.getGameTime()+4;refreshPanel(level,pos,be);be.sync();
            playPress(level,pos,be);
        }
        return true;
    }
    private static void playPress(Level level,BlockPos pos,ElevatorPartEntity panel){
        if(panel.callVolume==0)return;
        var sound=switch(panel.callFace){
            case DIGITAL_TOUCH_PANEL->net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value();
            case ROUND_STAINLESS,SQUARE_METAL,ROUND_FULL_METAL,ROUND_RING_LIGHT,SQUARE_FULL_METAL,SQUARE_RING_LIGHT->net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE;
            case SQUARE_ARROW,MODERN_INDUSTRIAL->net.minecraft.sounds.SoundEvents.IRON_DOOR_CLOSE;
            case CLASSIC_BRASS->net.minecraft.sounds.SoundEvents.COPPER_TRAPDOOR_CLOSE;
        };
        float pitch=switch(panel.callFace){case DIGITAL_TOUCH_PANEL->1.55f;case ROUND_STAINLESS,ROUND_FULL_METAL,ROUND_RING_LIGHT->1.3f;case SQUARE_METAL,SQUARE_FULL_METAL,SQUARE_RING_LIGHT->1.65f;case SQUARE_ARROW->.75f;case CLASSIC_BRASS->1.25f;case MODERN_INDUSTRIAL->1.1f;};
        level.playSound(null,pos,sound,net.minecraft.sounds.SoundSource.BLOCKS,panel.callVolume*.006f,pitch);
    }
    public static boolean pending(Level level,BlockPos pos){
        if(level instanceof net.minecraft.server.level.ServerLevel server){
            if(level.getBlockState(pos).getBlock() instanceof ElevatorCallBlock call&&call.destinationSelector())return false;
            var banks=ElevatorBanks.get(server);
            if(banks.hasService(server,pos))return banks.pending(server,pos)!=0;
        }
        return targets(level,pos).entrySet().stream().anyMatch(e->e.getKey().pending(e.getValue()));
    }
    @Override protected ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(ElevatorLinking.wrench(stack)){if(level instanceof net.minecraft.server.level.ServerLevel server){
            var banks=ElevatorBanks.get(server);
            if(destinationSelector&&player.isShiftKeyDown()&&player instanceof net.minecraft.server.level.ServerPlayer editor
                &&player.mayBuild()&&level.mayInteract(player,pos)){
                if(banks.associateSelector(server,pos,editor))player.displayClientMessage(Component.literal("Selector linked to the selected bank"),true);
                else ElevatorFeedback.error(player,"Select a bank with a registered landing beside this selector.");
                return ItemInteractionResult.CONSUME;
            }
            var d=banks.menuDrive(server,pos,!destinationSelector);
            if(d!=null&&(!player.isShiftKeyDown()||destinationSelector))d.openPanel(player,!destinationSelector,pos);
            else if(!destinationSelector)ElevatorLinking.button(level,pos,player);
        }return ItemInteractionResult.sidedSuccess(level.isClientSide);}return super.useItemOn(stack,state,level,pos,player,hand,hit);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(level instanceof net.minecraft.server.level.ServerLevel server){
            var banks=ElevatorBanks.get(server);
            if(destinationSelector){
                var drive=banks.menuDrive(server,pos,false);
                if(drive==null)ElevatorFeedback.error(player,"Place the selector beside a linked bank landing.");
                else if(player.isShiftKeyDown())drive.openPanel(player,false,pos);
                else if(screenHit(state,hit)&&player instanceof net.minecraft.server.level.ServerPlayer rider
                        &&level.getBlockEntity(pos) instanceof ElevatorPartEntity panel&&panel.destinationDisplay.isEmpty()){
                    double y=hit.getLocation().y-pos.getY(),x=screenX(state,hit);
                    int row=destinationSlot(x,y);
                    if(y<=.30&&(x<.33||x>.67))scrollDestination(server,pos,x<.33?1:-1);
                    else if(row>=0&&row<panel.destinationFloors.length&&!banks.destination(server,pos,panel.destinationFloors[row],rider)){
                        panel.deniedDestinationSlot=row;
                        panel.deniedDestinationUntil=server.getGameTime()+12;
                        panel.sync();
                        ElevatorFeedback.error(player,"No bank car serves that destination.");
                    }
                }
                return InteractionResult.CONSUME;
            }
            if(banks.hasService(server,pos)){
                int dir=level.getBlockEntity(pos) instanceof ElevatorPartEntity panel?callDirection(state,hit,panel):0;
                if(dir==0)return InteractionResult.CONSUME;
                if(!bankCall(server,pos,dir))ElevatorFeedback.error(player,"No bank car serves this direction.");
                return InteractionResult.CONSUME;
            }
            if(player.isShiftKeyDown()&&player.getMainHandItem().isEmpty()&&level.getBlockEntity(pos) instanceof ElevatorPartEntity button&&(!button.callMarkers().isEmpty()||button.markerDrive()!=null)){button.clearMarkers();button.bindMarker(null);player.displayClientMessage(Component.translatable("message.createtransit.elevator.links_cleared"),false);}
            else if(level.getBlockEntity(pos) instanceof ElevatorPartEntity panel){
                refreshPanel(level,pos,panel);
                int direction=callDirection(state,hit,panel);
                if(direction==0||direction>0&&panel.callUp||direction<0&&panel.callDown)return InteractionResult.CONSUME;
                int count=call(level,pos,direction);
                if(count==0)ElevatorFeedback.error(player,"No available linked floor. Check marker rails and service settings.");
                else player.displayClientMessage(Component.translatable("message.createtransit.elevator.called",count),false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos neighbor,boolean moving){if(level.isClientSide)return;boolean power=level.hasNeighborSignal(pos);if(power!=state.getValue(POWERED)){level.setBlock(pos,state.setValue(POWERED,power),Block.UPDATE_CLIENTS);if(power)call(level,pos);}}
}
