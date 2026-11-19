package com.sanctuarygames.createtransit.elevator.model;
import static com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.*;
import static com.sanctuarygames.createtransit.elevator.model.CabinGeometry.Material;
/** V13 elevator sprites in the current skin slots. Legacy signature retained for source-compatible addons. */
public final class DoorStyleTextures {
 private DoorStyleTextures(){}
 public static boolean waveOne(DoorStyle ignored){return true;}
 public static String sprite(DoorStyle ignored,Material material){return sprite(Skin.BRUSHED_STEEL,material,"high");}
 public static String sprite(Skin skin,Material material,String quality){
  String tile=switch(material){case FRAME->"shell";case DOOR_FRAME->"frame";case WALL->"panel";case COPY_PRIMARY->"door_leaf";case CEILING->"ceiling";case FLOOR->"floor";case HANDRAIL->"silver";case RAIL_MATCH->"panel";case RAIL_DARK->"dark";case BASE_TRIM->"base_trim";case METAL,DOOR_TRIM->"edge";case GLASS->"glass";case DOOR_GLASS->"door_glass";case DARK,DOOR_GASKET,DOOR_DISPLAY->"dark";case LIGHT,ACCENT,DOOR_LED->"led";case BODY_ACCENT,FOAM->"white";default->null;};
  return tile==null?null:"createtransit:block/elevator3/"+quality+"/"+skin.name().toLowerCase(java.util.Locale.ROOT)+"/"+tile;
 }
}
