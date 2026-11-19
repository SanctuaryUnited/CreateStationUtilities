package com.sanctuarygames.createtransit.elevator;
import com.sanctuarygames.createtransit.block.CabinColors;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public class ElevatorPartEntity extends BlockEntity implements CabinColors {
 public com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.IndicatorStyle callStyle=com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.IndicatorStyle.PIXEL_MATRIX;public int callPressedDirection;
 public enum CallFace {
  DIGITAL_TOUCH_PANEL, ROUND_STAINLESS, SQUARE_METAL, SQUARE_ARROW, CLASSIC_BRASS, MODERN_INDUSTRIAL, ROUND_FULL_METAL, ROUND_RING_LIGHT, SQUARE_FULL_METAL, SQUARE_RING_LIGHT;
  public boolean round(){return this==ROUND_STAINLESS||this==ROUND_FULL_METAL||this==ROUND_RING_LIGHT;}
  public boolean ring(){return this==ROUND_RING_LIGHT||this==SQUARE_RING_LIGHT;}
  public String label(){
   return switch(this){
    case DIGITAL_TOUCH_PANEL->"Digital touch panel";case ROUND_STAINLESS->"Round stainless";
    case SQUARE_METAL->"Square metal";case SQUARE_ARROW->"Square arrow buttons";
    case ROUND_FULL_METAL->"Round full metal";case ROUND_RING_LIGHT->"Round ring light";
    case SQUARE_FULL_METAL->"Square full metal";case SQUARE_RING_LIGHT->"Square ring light";
    case CLASSIC_BRASS->"Classic brass";case MODERN_INDUSTRIAL->"Modern industrial";
   };
  }
 }
 public CallFace callFace=CallFace.DIGITAL_TOUCH_PANEL;
 public boolean directionalCallLights;
 public int callVolume=70;
 public String destinationDisplay="",destinationPreview="";
 public int destinationPage,destinationPages=1;
 public int deniedDestinationSlot=-1;public long deniedDestinationUntil;
 public int[] destinationFloors=new int[0];
 public int callLayout;public boolean callUp,callDown,callDisabled=true,callFault;public long callPressedUntil;
 private int paint,accent;private int travel,previous;
 private BlockPos markerDrive;
 private final java.util.LinkedHashSet<BlockPos> callMarkers=new java.util.LinkedHashSet<>();
 private String floorId="",floorName="";private boolean floorEnabled=true;
 public String floorId(){return floorId;}public String floorName(){return floorName;}public boolean floorEnabled(){return floorEnabled;}
 public void configureFloor(String id,String name,boolean enabled){floorId=id;floorName=name;floorEnabled=enabled;sync();if(level!=null)ElevatorNetwork.railsChanged(level,worldPosition);}
 public boolean fullyClosed(){return travel==0;}
 public BlockPos markerDrive(){return markerDrive;}
 public void bindMarker(BlockPos drive){markerDrive=drive==null?null:drive.immutable();sync();}
 public java.util.List<BlockPos> callMarkers(){return java.util.List.copyOf(callMarkers);}
 public boolean toggleMarker(BlockPos marker){if(!callMarkers.remove(marker)){callMarkers.add(marker.immutable());}sync();return true;}
 public void clearMarkers(){callMarkers.clear();sync();}
 public ElevatorPartEntity(BlockPos p,BlockState s){this(ElevatorRegistry.PART_ENTITY.get(),p,s);}
 protected ElevatorPartEntity(BlockEntityType<?> t,BlockPos p,BlockState s){super(t,p,s);travel=previous=s.hasProperty(ElevatorDoorBlock.OPEN)&&s.getValue(ElevatorDoorBlock.OPEN)?40:0;}
 public int paint(){return paint;}public int accent(){return accent;}
 public void setColors(int p,int a){paint=Math.clamp(p,0,15);accent=Math.clamp(a,0,15);sync();}
 protected void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
 public void advance(boolean open){previous=travel;travel=Math.clamp(travel+(open?1:-1),0,40);}
 public float linearProgress(float partial){return (previous+(travel-previous)*partial)/40f;}
 public float progress(float partial){float t=linearProgress(partial);return t*t*(3-2*t);}
 public int statusColor(float partial,long time){float p=progress(partial);boolean open=getBlockState().hasProperty(ElevatorDoorBlock.OPEN)&&getBlockState().getValue(ElevatorDoorBlock.OPEN);if(open)return p>=.999f?0x32ff69:time%12<6?0x32ff69:0x073313;if(p>.001f)return time%12<6?0xffd22a:0x362b04;return 0x111820;}
 public static void tick(Level l,BlockPos p,BlockState s,ElevatorPartEntity be){if(s.getBlock() instanceof ElevatorDoorBlock door&&s.getValue(ElevatorDoorBlock.ROW)==0){if(!l.isClientSide&&!door.cabin()&&s.getValue(ElevatorDoorBlock.OPEN)&&!ElevatorNetwork.landingAllowed(l,p)){ElevatorDoorBlock.setOpen(l,p,false,false);s=l.getBlockState(p);}be.advance(s.getValue(ElevatorDoorBlock.OPEN));}if(!l.isClientSide&&s.getBlock() instanceof ElevatorCallBlock&&l.getGameTime()%5==0){ElevatorCallBlock.refreshPanel(l,p,be);boolean requested=ElevatorCallBlock.pending(l,p);if(s.getValue(ElevatorCallBlock.REQUESTED)!=requested)l.setBlock(p,s.setValue(ElevatorCallBlock.REQUESTED,requested),Block.UPDATE_CLIENTS);}}
 @Override public void onLoad(){super.onLoad();if(level!=null){ElevatorNetwork.register(level,worldPosition,getBlockState());if(getBlockState().getBlock() instanceof ElevatorGuideRailBlock){ElevatorGuideRailBlock.refresh(level,worldPosition);ElevatorNetwork.railsChanged(level,worldPosition);}if(getBlockState().getBlock() instanceof ElevatorSolidBlock&&level.getBlockState(worldPosition).getBlock()==getBlockState().getBlock())ElevatorSolidBlock.refresh(level,worldPosition);}}
 @Override public void setRemoved(){if(level!=null)ElevatorNetwork.unregister(level,worldPosition);super.setRemoved();}
 @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putString("DestinationDisplay",destinationDisplay);t.putString("DestinationPreview",destinationPreview);t.putInt("DeniedDestinationSlot",deniedDestinationSlot);t.putLong("DeniedDestinationUntil",deniedDestinationUntil);t.putInt("DestinationPage",destinationPage);t.putInt("DestinationPages",destinationPages);t.putIntArray("DestinationFloors",destinationFloors);t.putString("CallStyle",callStyle.name());t.putString("CallFace",callFace.name());t.putBoolean("DirectionalCallLights",directionalCallLights);t.putInt("CallVolume",callVolume);t.putInt("CallPressedDirection",callPressedDirection);t.putInt("CallLayout",callLayout);t.putBoolean("CallUp",callUp);t.putBoolean("CallDown",callDown);t.putBoolean("CallDisabled",callDisabled);t.putBoolean("CallFault",callFault);t.putLong("CallPressedUntil",callPressedUntil);t.putInt("paint",paint);t.putInt("accent",accent);if(markerDrive!=null)t.putLong("MarkerDrive",markerDrive.asLong());t.putLongArray("FloorMarkers",callMarkers.stream().mapToLong(BlockPos::asLong).toArray());t.putString("FloorId",floorId);t.putString("FloorName",floorName);t.putBoolean("FloorEnabled",floorEnabled);}
 @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);destinationDisplay=t.getString("DestinationDisplay");destinationPreview=t.getString("DestinationPreview");deniedDestinationSlot=t.getInt("DeniedDestinationSlot");deniedDestinationUntil=t.getLong("DeniedDestinationUntil");destinationPage=Math.max(0,t.getInt("DestinationPage"));destinationPages=Math.max(1,t.getInt("DestinationPages"));destinationFloors=t.getIntArray("DestinationFloors");try{callStyle=com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.IndicatorStyle.valueOf(t.getString("CallStyle"));}catch(IllegalArgumentException ignored){}try{callFace=CallFace.valueOf(t.getString("CallFace"));}catch(IllegalArgumentException ignored){callFace=CallFace.DIGITAL_TOUCH_PANEL;}
 directionalCallLights=t.getBoolean("DirectionalCallLights");callVolume=t.contains("CallVolume")?Math.clamp(t.getInt("CallVolume"),0,100):70;
 callPressedDirection=Integer.signum(t.getInt("CallPressedDirection"));callLayout=Math.clamp(t.getInt("CallLayout"),0,2);callUp=t.getBoolean("CallUp");callDown=t.getBoolean("CallDown");callDisabled=!t.contains("CallDisabled")||t.getBoolean("CallDisabled");callFault=t.getBoolean("CallFault");callPressedUntil=t.getLong("CallPressedUntil");paint=Math.clamp(t.getInt("paint"),0,15);accent=Math.clamp(t.getInt("accent"),0,15);markerDrive=t.contains("MarkerDrive")?BlockPos.of(t.getLong("MarkerDrive")):null;callMarkers.clear();for(long p:t.getLongArray("FloorMarkers")){callMarkers.add(BlockPos.of(p));}floorId=t.getString("FloorId");floorName=t.getString("FloorName");floorEnabled=!t.contains("FloorEnabled")||t.getBoolean("FloorEnabled");}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
 @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
