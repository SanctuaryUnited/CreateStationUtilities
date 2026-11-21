package com.sanctuarygames.createtransit.elevator.model;
import com.sanctuarygames.createtransit.elevator.*;import net.minecraft.core.*;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.nbt.CompoundTag;import net.minecraft.world.entity.player.Player;import net.minecraft.network.chat.Component;
public final class ModelLandingEntity extends ElevatorPartEntity {
 public CabinConfiguration renderConfiguration;public BlockState renderState;
 public CabinConfiguration.Skin skin=CabinConfiguration.Skin.BRUSHED_STEEL;public int ledColor=9;
 public CabinConfiguration.IndicatorStyle sideIndicatorStyle=CabinConfiguration.IndicatorStyle.PIXEL_MATRIX;public String landingLabel="--",landingName="";
 public String shaftLetter="";
 public String header(long tick){
  String floor=!landingName.isBlank()&&Math.floorMod(tick,120)>=60?landingName:landingLabel;
  return shaftLetter.isEmpty()?floor:shaftLetter+" · "+floor;
 }
 public CabinConfiguration.IndicatorStyle indicatorStyle=CabinConfiguration.IndicatorStyle.PIXEL_MATRIX;
 public CabinConfiguration.FrameType frameType=CabinConfiguration.FrameType.FLUSH;public CabinConfiguration.Mechanism mechanism=CabinConfiguration.Mechanism.CENTER;public boolean twoSpeed,indicators=true;
 public void applyDoorConfig(CabinConfiguration c){sideIndicatorStyle=c.sideIndicatorStyle();skin=c.skin();ledColor=c.ledColor();indicatorStyle=c.indicatorStyle();frameType=c.frameType();mechanism=c.mechanism();twoSpeed=c.twoSpeed();indicators=c.indicators();primaryMaterial=c.materialPrimary();secondaryMaterial=c.materialSecondary();}
 public CabinConfiguration.DoorStyle style=CabinConfiguration.DoorStyle.SOLID;public CabinConfiguration.Glazing glazing=CabinConfiguration.Glazing.SOLID;public String primaryMaterial="minecraft:iron_block",secondaryMaterial="minecraft:polished_andesite",label="--";public String woodMaterial="minecraft:oak_planks";public double offsetAcross,offsetDepth;public boolean mirrorIndicators;public int direction;public int serviceDirection;public long arrivalTick=-100;public boolean arriving,aligned,animatedArrows,directionalLanterns,displayPowered;private BlockPos marker;
 public ModelLandingEntity(BlockPos p,BlockState s){super(ElevatorRegistry.MODEL_LANDING_ENTITY.get(),p,s);style=CabinConfiguration.DoorStyle.CLASSIC_BRUSHED_STEEL;}
 public void appearanceChanged(){renderConfiguration=null;sync();}
 public boolean autoLink(){
  if(level==null||level.isClientSide||markerDrive()!=null)return false;var s=getBlockState();var base=ModelLandingBlock.base(worldPosition,s);if(!base.equals(worldPosition))return false;
  ElevatorDriveEntity match=null;
  for(var d:ElevatorNetwork.controllers(level))if(d.model()!=null&&d.model().matchesLanding(base,s.getValue(ElevatorPartBlock.FACING),s.getValue(ModelLandingBlock.SIZE))){d.ensureFloors();if(d.floorList().stream().noneMatch(f->f.marker().getY()==base.getY()))continue;if(match!=null)return false;match=d;}
  if(match==null)return false;marker=match.getBlockPos().atY(base.getY());bindMarker(match.getBlockPos());match.model().alignLanding(this);mirrorIndicators=match.model().mirrorIndicators();var c=match.model().config();applyDoorConfig(c);style=c.doorStyle();glazing=c.glazing();woodMaterial=c.wood();primaryMaterial=c.materialPrimary();secondaryMaterial=c.materialSecondary();spread();return true;
 }
 public void cycleStyle(){var models=CabinConfiguration.DOOR_MODELS;int next=0;for(int i=0;i<models.length;i++)if(models[i]==style)next=(i+1)%models.length;if(markerDrive()!=null&&level.getBlockEntity(markerDrive()) instanceof ElevatorDriveEntity d&&d.model()!=null){d.configureModel("doorStyle",models[next].name());return;}if(!getBlockState().getValue(ModelLandingBlock.OPEN)){style=models[next];spread();}}
 public void cycleGlazing(){
  if(style==CabinConfiguration.DoorStyle.CLASSIC_WOOD)return;
  var next=switch(glazing){case SOLID->CabinConfiguration.Glazing.FULL_GLASS;case FULL_GLASS->CabinConfiguration.Glazing.SLIM_GLASS;default->CabinConfiguration.Glazing.SOLID;};
  if(markerDrive()!=null&&level.getBlockEntity(markerDrive()) instanceof ElevatorDriveEntity d&&d.model()!=null){
   d.configureModel("glazing",next.name());
  }else if(!getBlockState().getValue(ModelLandingBlock.OPEN)){
   if(style==CabinConfiguration.DoorStyle.SOLID||style==CabinConfiguration.DoorStyle.GLASS)style=CabinConfiguration.DoorStyle.OG_STEEL;
   glazing=next;
   var state=getBlockState();var facing=state.getValue(ElevatorPartBlock.FACING);
   var base=ModelLandingBlock.base(worldPosition,state);
   for(int x=0;x<state.getValue(ModelLandingBlock.SIZE);x++)for(int y=0;y<(state.getValue(ModelLandingBlock.KIT3)?3:2);y++){
    var pos=ModelLandingBlock.part(base,facing,x,y);
    if(level.getBlockState(pos).getBlock() instanceof ModelLandingBlock){
     var cell=level.getBlockState(pos);
     level.setBlock(pos,cell.setValue(ModelLandingBlock.GLASS,next!=CabinConfiguration.Glazing.SOLID),Block.UPDATE_CLIENTS);
    }
   }
   spread();
  }
 }
 public void material(Block b,boolean second){String id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).toString();if(second)secondaryMaterial=id;else primaryMaterial=id;spread();}
 private void spread(){var s=getBlockState();var base=ModelLandingBlock.base(worldPosition,s);for(int x=0;x<s.getValue(ModelLandingBlock.SIZE);x++)for(int y=0;y<(s.getValue(ModelLandingBlock.KIT3)?3:2);y++)if(level.getBlockEntity(ModelLandingBlock.part(base,s.getValue(ElevatorPartBlock.FACING),x,y)) instanceof ModelLandingEntity e){e.sideIndicatorStyle=sideIndicatorStyle;e.skin=skin;e.ledColor=ledColor;e.offsetAcross=offsetAcross;e.offsetDepth=offsetDepth;e.indicatorStyle=indicatorStyle;e.frameType=frameType;e.mechanism=mechanism;e.twoSpeed=twoSpeed;e.indicators=indicators;e.mirrorIndicators=mirrorIndicators;e.style=style;e.glazing=glazing;e.woodMaterial=woodMaterial;e.primaryMaterial=primaryMaterial;e.secondaryMaterial=secondaryMaterial;e.marker=marker;e.bindMarker(markerDrive());e.sync();}sync();}
 public void link(Player player){var pos=ElevatorLinking.selectedMarker(player,level);if(pos==null||!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof ElevatorPartEntity source)||source.markerDrive()==null||!(level.getBlockEntity(source.markerDrive()) instanceof ElevatorDriveEntity d)){player.displayClientMessage(com.sanctuarygames.createtransit.TransitText.tr("message.createtransit.elevator.with_empty_hands_sneak_break_a"),false);return;}
  var base=ModelLandingBlock.base(worldPosition,getBlockState());if(pos.getY()!=base.getY()||(d.model()==null||!d.model().matchesLanding(base,getBlockState().getValue(ElevatorPartBlock.FACING),getBlockState().getValue(ModelLandingBlock.SIZE)))){player.displayClientMessage(com.sanctuarygames.createtransit.TransitText.tr("message.createtransit.elevator.door_must_align_with_this_cabin"),false);return;}marker=pos;bindMarker(source.markerDrive());d.model().alignLanding(this);applyDoorConfig(d.model().config());style=d.model().config().doorStyle();glazing=d.model().config().glazing();woodMaterial=d.model().config().wood();mirrorIndicators=d.model().mirrorIndicators();spread();player.displayClientMessage(com.sanctuarygames.createtransit.TransitText.tr("message.createtransit.elevator.landing_linked_to_s",source.floorId()),false);
 }
 @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider p){super.saveAdditional(t,p);t.putString("SideIndicatorStyle",sideIndicatorStyle.name());t.putString("LandingLabel",landingLabel);t.putString("LandingName",landingName);t.putString("ShaftLetter",shaftLetter);t.putInt("AppearanceSchema",2);t.putString("Skin",skin.name());t.putInt("LedColor",ledColor);t.putDouble("OffsetAcross",offsetAcross);t.putDouble("OffsetDepth",offsetDepth);t.putString("IndicatorStyle",indicatorStyle.name());t.putString("FrameType",frameType.name());t.putString("Mechanism",mechanism.name());t.putBoolean("TwoSpeed",twoSpeed);t.putBoolean("Indicators",indicators);t.putBoolean("MirrorIndicators",mirrorIndicators);t.putString("DoorStyle",style.name());t.putString("WoodMaterial",woodMaterial);t.putString("Glazing",glazing.name());t.putString("PrimaryMaterial",primaryMaterial);t.putString("SecondaryMaterial",secondaryMaterial);t.putString("DisplayLabel",label);t.putInt("Direction",direction);t.putInt("ServiceDirection",serviceDirection);t.putLong("ArrivalTick",arrivalTick);t.putBoolean("Arriving",arriving);t.putBoolean("DisplayPowered",displayPowered);t.putBoolean("AnimatedArrows",animatedArrows);t.putBoolean("DirectionalLanterns",directionalLanterns);t.putBoolean("Aligned",aligned);if(marker!=null)t.putLong("LinkedMarker",marker.asLong());}
 @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider p){super.loadAdditional(t,p);renderConfiguration=null;shaftLetter=t.getString("ShaftLetter");landingName=t.getString("LandingName");landingLabel=t.contains("LandingLabel")?t.getString("LandingLabel"):"--";try{sideIndicatorStyle=CabinConfiguration.IndicatorStyle.valueOf(t.getString("SideIndicatorStyle"));}catch(IllegalArgumentException ignored){}try{indicatorStyle=CabinConfiguration.IndicatorStyle.valueOf(t.getString("IndicatorStyle"));}catch(IllegalArgumentException ignored){}if(!t.contains("SideIndicatorStyle"))sideIndicatorStyle=indicatorStyle;offsetAcross=0;offsetDepth=0;frameType=CabinConfiguration.FrameType.STANDARD;try{frameType=CabinConfiguration.FrameType.valueOf(t.getString("FrameType"));mechanism=CabinConfiguration.Mechanism.valueOf(t.getString("Mechanism"));}catch(IllegalArgumentException ignored){}twoSpeed=t.getBoolean("TwoSpeed");indicators=!t.contains("Indicators")||t.getBoolean("Indicators");mirrorIndicators=t.getBoolean("MirrorIndicators");if(t.contains("WoodMaterial"))woodMaterial=t.getString("WoodMaterial");try{style=CabinConfiguration.DoorStyle.valueOf(t.getString("DoorStyle"));glazing=CabinConfiguration.Glazing.valueOf(t.getString("Glazing"));}catch(IllegalArgumentException ignored){}if(t.contains("PrimaryMaterial"))primaryMaterial=t.getString("PrimaryMaterial");if(t.contains("SecondaryMaterial"))secondaryMaterial=t.getString("SecondaryMaterial");if(t.contains("Skin"))try{skin=CabinConfiguration.approvedSkin(t.getString("Skin"));}catch(IllegalArgumentException ignored){}else skin=CabinConfiguration.legacySkin(CabinConfiguration.Variant.SOLID,style);ledColor=t.contains("LedColor")?Math.clamp(t.getInt("LedColor"),0,15):9;if(!t.contains("DoorStyle"))style=getBlockState().getValue(ModelLandingBlock.GLASS)?CabinConfiguration.DoorStyle.GLASS:CabinConfiguration.DoorStyle.SOLID;
 if(t.getInt("AppearanceSchema")<2&&style==CabinConfiguration.DoorStyle.SOLID&&getBlockState().getValue(ModelLandingBlock.GLASS))style=CabinConfiguration.DoorStyle.GLASS;style=CabinConfiguration.doorModel(style,CabinConfiguration.Door.SOLID,glazing);if(style==CabinConfiguration.DoorStyle.GLASS)glazing=CabinConfiguration.Glazing.FULL_GLASS;label=t.getString("DisplayLabel");direction=t.getInt("Direction");serviceDirection=t.getInt("ServiceDirection");arrivalTick=t.contains("ArrivalTick")?t.getLong("ArrivalTick"):-100;arriving=t.getBoolean("Arriving");displayPowered=t.getBoolean("DisplayPowered");animatedArrows=t.getBoolean("AnimatedArrows");directionalLanterns=t.getBoolean("DirectionalLanterns");aligned=t.getBoolean("Aligned");marker=t.contains("LinkedMarker")?BlockPos.of(t.getLong("LinkedMarker")):null;}
 public static void tick(Level l,BlockPos p,BlockState s,ModelLandingEntity e){
  if(!l.isClientSide&&e.markerDrive()==null&&l.getGameTime()%20==0)e.autoLink();
  if(!l.isClientSide&&s.getValue(ModelLandingBlock.OPEN)&&!ElevatorNetwork.landingAllowed(l,ModelLandingBlock.base(p,s))){ModelLandingBlock.open(l,ModelLandingBlock.base(p,s),false);s=l.getBlockState(p);}e.advance(s.getValue(ModelLandingBlock.OPEN));
  if(!(l instanceof net.minecraft.server.level.ServerLevel server)||l.getGameTime()%5!=0
      ||s.getValue(ModelLandingBlock.ROW)!=0||s.getValue(ModelLandingBlock.COLUMN)!=0)return;
  String label="--",letter="",fixed=e.landingLabel,name=e.landingName;
  int direction=0,service=0;
  boolean powered=false;int accent=e.ledColor;
  boolean aligned=false,arriving=false,animated=e.animatedArrows, directional=e.directionalLanterns;
  int floor=p.getY()-1;
  var owner=e.markerDrive();
  if(owner!=null&&l.hasChunkAt(owner)&&l.getBlockEntity(owner) instanceof ElevatorDriveEntity d){
   letter=ElevatorBanks.get(server).letter(d.identifier());
   fixed=d.floorLabel(floor);
   name=d.floorDisplayName(floor);
   var model=d.model();
   if(model!=null){
    accent=model.config().ledColor();powered=model.active()&&model.servesLanding(floor,s.getValue(ElevatorPartBlock.FACING));
    animated=model.landingAnimated();directional=model.directionalLanterns();
    var motion=model.state();
    var cabin=model.entity();
    if(motion!=null&&cabin!=null){
     var phase=motion.state();
     boolean moving=phase==CabinMotionController.State.MOVING||phase==CabinMotionController.State.LEVELING;
     label=moving?"Y "+(int)Math.rint(motion.y()):d.floorLabel(model.nearestFloor(motion.y()));
     direction=moving?motion.serviceDirection():0;
     aligned=Math.abs(motion.y()-floor)<.001;
     boolean servesDoor=model.servesLanding(floor,s.getValue(ElevatorPartBlock.FACING));
     arriving=aligned&&servesDoor&&(phase==CabinMotionController.State.DOORS_OPENING
         ||phase==CabinMotionController.State.DOORS_OPEN
         ||phase==CabinMotionController.State.DOORS_CLOSING&&(e.arriving||e.linearProgress(1)>0));
     if(arriving){
      service=cabin.serviceDirection();
      if(phase==CabinMotionController.State.DOORS_CLOSING&&e.arriving)service=e.serviceDirection;
     }
    }
   }
  }
  if(!letter.equals(e.shaftLetter)||!fixed.equals(e.landingLabel)||!name.equals(e.landingName)
      ||!label.equals(e.label)||direction!=e.direction||service!=e.serviceDirection
      ||aligned!=e.aligned||arriving!=e.arriving||powered!=e.displayPowered||accent!=e.ledColor||animated!=e.animatedArrows||directional!=e.directionalLanterns){
   e.shaftLetter=letter;e.landingLabel=fixed;e.landingName=name;e.label=label;
   e.direction=direction;e.serviceDirection=service;e.aligned=aligned;e.arriving=arriving;e.displayPowered=powered;e.ledColor=accent;e.animatedArrows=animated;e.directionalLanterns=directional;
   e.sync();
  }
 }
}
