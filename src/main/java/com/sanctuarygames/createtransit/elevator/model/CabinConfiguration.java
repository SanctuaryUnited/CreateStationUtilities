package com.sanctuarygames.createtransit.elevator.model;

import java.util.*;

/** Immutable, validated cabin settings. Independent of client classes and registry loading. */
public record CabinConfiguration(int size, Variant variant, int primary, int secondary,
        Door front, boolean rearEnabled, Door rear, boolean leftGlass, boolean rightGlass,
        boolean curvedGlass, String wood, String floor, Lighting lighting, Chime chime, Handrail handrail, DoorStyle doorStyle, Glazing glazing, String materialPrimary, String materialSecondary, FrameType frameType, Mechanism mechanism, boolean twoSpeed, boolean indicators, boolean kit3, boolean compact, boolean voxelCurve, IndicatorStyle indicatorStyle, Skin skin, int ledColor, boolean mainLights, IndicatorStyle sideIndicatorStyle, List<Integer> ceilingColors, int brightness, boolean legacyGeometry, boolean v13Geometry) {
 public CabinConfiguration(int size, Variant variant, int primary, int secondary,
        Door front, boolean rearEnabled, Door rear, boolean leftGlass, boolean rightGlass,
        boolean curvedGlass, String wood, String floor, Lighting lighting, Chime chime, Handrail handrail, DoorStyle doorStyle, Glazing glazing, String materialPrimary, String materialSecondary, FrameType frameType, Mechanism mechanism, boolean twoSpeed, boolean indicators, boolean kit3, boolean compact, boolean voxelCurve, IndicatorStyle indicatorStyle, Skin skin, int ledColor, boolean mainLights, IndicatorStyle sideIndicatorStyle, List<Integer> ceilingColors, int brightness) {
  this(size,variant,primary,secondary,front,rearEnabled,rear,leftGlass,rightGlass,curvedGlass,wood,floor,lighting,chime,handrail,doorStyle,glazing,materialPrimary,materialSecondary,frameType,mechanism,twoSpeed,indicators,kit3,compact,voxelCurve,indicatorStyle,skin,ledColor,mainLights,sideIndicatorStyle,ceilingColors,brightness,false,false);
 }
 public CabinConfiguration(int size, Variant variant, int primary, int secondary,
        Door front, boolean rearEnabled, Door rear, boolean leftGlass, boolean rightGlass,
        boolean curvedGlass, String wood, String floor, Lighting lighting, Chime chime, Handrail handrail, DoorStyle doorStyle, Glazing glazing, String materialPrimary, String materialSecondary, FrameType frameType, Mechanism mechanism, boolean twoSpeed, boolean indicators, boolean kit3, boolean compact, boolean voxelCurve, IndicatorStyle indicatorStyle, Skin skin, int ledColor, boolean mainLights, IndicatorStyle sideIndicatorStyle) {
  this(size,variant,primary,secondary,front,rearEnabled,rear,leftGlass,rightGlass,curvedGlass,wood,floor,lighting,chime,handrail,doorStyle,glazing,materialPrimary,materialSecondary,frameType,mechanism,twoSpeed,indicators,kit3,compact,voxelCurve,indicatorStyle,skin,ledColor,mainLights,sideIndicatorStyle,Collections.nCopies(5,0),15);
 }
 public static final Variant[] CABIN_MODELS={Variant.SOLID,Variant.REAR_PANORAMIC,Variant.SCENIC,Variant.CYLINDRICAL};
 public static final Skin[] APPROVED_SKINS={Skin.MODERN_DARK,Skin.BRUSHED_STEEL};
 public static final Handrail[] HANDRAIL_FINISHES={Handrail.MATCH,Handrail.BRUSHED,Handrail.DARK};
 public int ceilingColor(){return ceilingColors.get(Arrays.asList(CEILING_LAYOUTS).indexOf(lighting));}
 public static Skin approvedSkin(String name){try{var skin=Skin.valueOf(name);return skin==Skin.BRUSHED_STEEL?skin:Skin.MODERN_DARK;}catch(IllegalArgumentException ex){return Skin.MODERN_DARK;}}
 private static List<Integer> loadColors(Properties p){var colors=new ArrayList<Integer>();for(var value:p.getProperty("ceilingColors","0,0,0,0,0").split(",",-1))colors.add(Integer.parseInt(value));if(colors.size()!=5)throw new IllegalArgumentException("Five ceiling colors required");if(p.containsKey("ceilingColor")){var style=Lighting.valueOf(p.getProperty("lighting"));int index=Arrays.asList(CEILING_LAYOUTS).indexOf(normalizeLighting(style));colors.set(index,Integer.parseInt(p.getProperty("ceilingColor")));}return colors;}
 private static Lighting normalizeLighting(Lighting l){return l==Lighting.RING?Lighting.STRIPS:l==Lighting.BULBS?Lighting.RECESSED:l;}
 public enum Skin { BRUSHED_STEEL, MODERN_DARK, WOOD_TRIM, LIGHT_TRANSIT }
 public CabinConfiguration(int size, Variant variant, int primary, int secondary,
        Door front, boolean rearEnabled, Door rear, boolean leftGlass, boolean rightGlass,
        boolean curvedGlass, String wood, String floor, Lighting lighting, Chime chime, Handrail handrail, DoorStyle doorStyle, Glazing glazing, String materialPrimary, String materialSecondary, FrameType frameType, Mechanism mechanism, boolean twoSpeed, boolean indicators, boolean kit3, boolean compact, boolean voxelCurve, IndicatorStyle indicatorStyle, Skin skin, int ledColor, boolean mainLights){this(size,variant,primary,secondary,front,rearEnabled,rear,leftGlass,rightGlass,curvedGlass,wood,floor,lighting,chime,handrail,doorStyle,glazing,materialPrimary,materialSecondary,frameType,mechanism,twoSpeed,indicators,kit3,compact,voxelCurve,indicatorStyle,skin,ledColor,mainLights,indicatorStyle);}
 public static final DoorStyle[] DOOR_MODELS={DoorStyle.CLASSIC_BRUSHED_STEEL,DoorStyle.OG_STEEL,DoorStyle.CLASSIC_WOOD};
 public static final Lighting[] CEILING_LAYOUTS={Lighting.PANEL,Lighting.RECESSED,Lighting.STRIPS,Lighting.LINEAR,Lighting.GRID};
 public CabinConfiguration(int size, Variant variant, int primary, int secondary,
        Door front, boolean rearEnabled, Door rear, boolean leftGlass, boolean rightGlass,
        boolean curvedGlass, String wood, String floor, Lighting lighting, Chime chime, Handrail handrail, DoorStyle doorStyle, Glazing glazing, String materialPrimary, String materialSecondary, FrameType frameType, Mechanism mechanism, boolean twoSpeed, boolean indicators, boolean kit3, boolean compact, boolean voxelCurve, IndicatorStyle indicatorStyle){this(size,variant,primary,secondary,front,rearEnabled,rear,leftGlass,rightGlass,curvedGlass,wood,floor,lighting,chime,handrail,doorStyle,glazing,materialPrimary,materialSecondary,frameType,mechanism,twoSpeed,indicators,kit3,compact,voxelCurve,indicatorStyle,legacySkin(variant,doorStyle),9,true);}
 public CabinConfiguration(int size, Variant variant, int primary, int secondary,
        Door front, boolean rearEnabled, Door rear, boolean leftGlass, boolean rightGlass,
        boolean curvedGlass, String wood, String floor, Lighting lighting, Chime chime, Handrail handrail, DoorStyle doorStyle, Glazing glazing, String materialPrimary, String materialSecondary, FrameType frameType, Mechanism mechanism, boolean twoSpeed, boolean indicators, boolean kit3, boolean compact, boolean voxelCurve){this(size,variant,primary,secondary,front,rearEnabled,rear,leftGlass,rightGlass,curvedGlass,wood,floor,lighting,chime,handrail,doorStyle,glazing,materialPrimary,materialSecondary,frameType,mechanism,twoSpeed,indicators,kit3,compact,voxelCurve,IndicatorStyle.PIXEL_MATRIX);}
 public enum IndicatorStyle { PIXEL_MATRIX, SLIMLINE, CLASSIC_LED, RECESSED_GLASS, TRANSIT_SIGN, BRUSHED_METAL }
 public CabinConfiguration(int size, Variant variant, int primary, int secondary,
        Door front, boolean rearEnabled, Door rear, boolean leftGlass, boolean rightGlass,
        boolean curvedGlass, String wood, String floor, Lighting lighting, Chime chime, Handrail handrail, DoorStyle doorStyle, Glazing glazing, String materialPrimary, String materialSecondary, FrameType frameType, Mechanism mechanism, boolean twoSpeed, boolean indicators, boolean kit3, boolean compact){this(size,variant,primary,secondary,front,rearEnabled,rear,leftGlass,rightGlass,curvedGlass,wood,floor,lighting,chime,handrail,doorStyle,glazing,materialPrimary,materialSecondary,frameType,mechanism,twoSpeed,indicators,kit3,compact,false);}

 public CabinConfiguration(int size, Variant variant, int primary, int secondary,
        Door front, boolean rearEnabled, Door rear, boolean leftGlass, boolean rightGlass,
        boolean curvedGlass, String wood, String floor, Lighting lighting, Chime chime, Handrail handrail, DoorStyle doorStyle, Glazing glazing, String materialPrimary, String materialSecondary, FrameType frameType, Mechanism mechanism, boolean twoSpeed, boolean indicators, boolean kit3){this(size,variant,primary,secondary,front,rearEnabled,rear,leftGlass,rightGlass,curvedGlass,wood,floor,lighting,chime,handrail,doorStyle,glazing,materialPrimary,materialSecondary,frameType,mechanism,twoSpeed,indicators,kit3,false);}

    public enum FrameType { STANDARD, FLUSH, RECESSED, PROTRUDING }
    public enum Mechanism { CENTER, LEFT, RIGHT }
    /** Existing schema-1 saves and addon source constructors keep their old defaults. */
    public CabinConfiguration(int size,Variant variant,int primary,int secondary,Door front,boolean rearEnabled,Door rear,boolean leftGlass,boolean rightGlass,boolean curvedGlass,String wood,String floor,Lighting lighting,Chime chime,Handrail handrail,DoorStyle doorStyle,Glazing glazing,String materialPrimary,String materialSecondary){this(size,variant,primary,secondary,front,rearEnabled,rear,leftGlass,rightGlass,curvedGlass,wood,floor,lighting,chime,handrail,doorStyle,glazing,materialPrimary,materialSecondary,FrameType.STANDARD,Mechanism.CENTER,false,true,false);}
    public enum Variant { REAR_PANORAMIC, SOLID, CYLINDRICAL, WOOD, SCENIC }
    public enum DoorStyle { SOLID, GLASS, WOOD_TRIM, SLIM_WINDOW, DIVIDED_GLASS, INDUSTRIAL, RETRO, DARK_MODERN, ART_DECO, CLINICAL, TRANSIT, EXECUTIVE, HERITAGE, HIGH_TECH, COASTAL, MINIMAL, COPYCAT, CLASSIC_BRUSHED_STEEL, OG_STEEL, CLASSIC_WOOD }
    public enum Glazing { SOLID, HALF_GLASS, FULL_GLASS, SLIM_GLASS }
    public CabinConfiguration(int size,Variant variant,int primary,int secondary,Door front,boolean rearEnabled,Door rear,boolean leftGlass,boolean rightGlass,boolean curvedGlass,String wood,String floor,Lighting lighting,Chime chime,Handrail handrail){this(size,variant,primary,secondary,front,rearEnabled,rear,leftGlass,rightGlass,curvedGlass,wood,floor,lighting,chime,handrail,front==Door.GLASS?DoorStyle.GLASS:DoorStyle.SOLID,Glazing.SOLID,"minecraft:iron_block","minecraft:polished_andesite");}
    public enum Door { SOLID, GLASS }
    public enum Lighting { PANEL, STRIPS, RING, BULBS, RECESSED, LINEAR, GRID }
    public enum Handrail { MATCH, BRUSHED, DARK, ACCENT, NONE }
    public enum Chime {
        OTIS, SCHINDLER_HT, ARRIVAL_DING, ELEVATOR_BELL, JAPAN_ARRIVAL,
        CLASSIC_BELL_DOORS, FM_BELL, SYNTH_DING, NONE;
        public String translationKey(){return "gui.createtransit.elevator.chime."+name().toLowerCase(java.util.Locale.ROOT);}
    }
    public CabinConfiguration(int size,Variant variant,int primary,int secondary,Door front,boolean rearEnabled,Door rear,boolean leftGlass,boolean rightGlass,boolean curvedGlass,String wood,String floor,Lighting lighting,Chime chime){this(size,variant,primary,secondary,front,rearEnabled,rear,leftGlass,rightGlass,curvedGlass,wood,floor,lighting,chime,Handrail.MATCH);}
    public CabinConfiguration {
        if (size < 2 || size > 5) throw new IllegalArgumentException("Cabin size must be 2–5");
        if(!legacyGeometry && size!=3) legacyGeometry=true;
        v13Geometry=v13Geometry&&legacyGeometry&&size==3;
        kit3=size>=3;compact=true;voxelCurve=true;if(frameType==FrameType.PROTRUDING)frameType=FrameType.STANDARD;mechanism=Mechanism.CENTER;twoSpeed=false;mainLights=true;Objects.requireNonNull(frameType);Objects.requireNonNull(mechanism);Objects.requireNonNull(variant); Objects.requireNonNull(front); Objects.requireNonNull(rear);
        Objects.requireNonNull(skin);if(ledColor<0||ledColor>15)throw new IllegalArgumentException("LED dye must be 0–15");
        if(variant==Variant.WOOD)variant=Variant.SOLID;skin=approvedSkin(skin.name());
        lighting=normalizeLighting(lighting);ceilingColors=List.copyOf(ceilingColors);if(ceilingColors.size()!=5||ceilingColors.stream().anyMatch(v->v==null||v<0||v>15))throw new IllegalArgumentException("Invalid ceiling colors");if(brightness<0||brightness>15)throw new IllegalArgumentException("Brightness must be 0–15");if(handrail==Handrail.NONE||handrail==Handrail.ACCENT)handrail=Handrail.MATCH;
        doorStyle=doorModel(doorStyle,front,glazing);
        if(doorStyle==DoorStyle.GLASS)glazing=Glazing.FULL_GLASS;
        if(doorStyle==DoorStyle.SOLID||doorStyle==DoorStyle.CLASSIC_WOOD)glazing=Glazing.SOLID;
        if(glazing==Glazing.HALF_GLASS)glazing=Glazing.SLIM_GLASS;
        front=glazing==Glazing.SOLID?Door.SOLID:Door.GLASS;
        Objects.requireNonNull(sideIndicatorStyle);Objects.requireNonNull(indicatorStyle);Objects.requireNonNull(lighting); Objects.requireNonNull(chime); Objects.requireNonNull(handrail);
        if (primary < 0 || primary > 15 || secondary < 0 || secondary > 15)
            throw new IllegalArgumentException("Dye index must be 0–15");
        if (variant == Variant.CYLINDRICAL && rearEnabled)
            throw new IllegalArgumentException("Cylindrical cabins cannot have rear doors");
        validateMaterial(wood); validateMaterial(floor); validateMaterial(materialPrimary); validateMaterial(materialSecondary); Objects.requireNonNull(doorStyle);Objects.requireNonNull(glazing);
    }
    private static void validateMaterial(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || id.length() > 256)
            throw new IllegalArgumentException("Invalid block material: " + id);
    }
    public static CabinConfiguration defaults(int size, Variant variant) {
        return new CabinConfiguration(size,variant,0,11,Door.SOLID,false,Door.SOLID,
            false,false,variant==Variant.CYLINDRICAL,"minecraft:oak_planks","createtransit:default_cabin_floor",
            Lighting.PANEL,Chime.OTIS,Handrail.MATCH,DoorStyle.CLASSIC_BRUSHED_STEEL,Glazing.SOLID,
            "minecraft:iron_block","minecraft:polished_andesite",FrameType.FLUSH,Mechanism.CENTER,
            false,true,true,true,true,IndicatorStyle.PIXEL_MATRIX,Skin.BRUSHED_STEEL,9,true,
            IndicatorStyle.PIXEL_MATRIX,Collections.nCopies(5,0),6);
    }
    public boolean rearGlass() { return variant == Variant.REAR_PANORAMIC && !rearEnabled || variant == Variant.SCENIC && !rearEnabled; }
    public boolean leftGlazed() { return variant == Variant.SCENIC || variant == Variant.REAR_PANORAMIC && leftGlass; }
    public boolean rightGlazed() { return variant == Variant.SCENIC || variant == Variant.REAR_PANORAMIC && rightGlass; }
    public boolean supportsRearDoors() { return variant != Variant.CYLINDRICAL; }
    public Properties save() {
        var p = new Properties();
        p.setProperty("schema", "5");p.setProperty("v13Geometry",Boolean.toString(v13Geometry));p.setProperty("legacyGeometry",Boolean.toString(legacyGeometry));p.setProperty("ceilingColors",ceilingColors.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));p.setProperty("ceilingColor",""+ceilingColor());p.setProperty("brightness",""+brightness);p.setProperty("skin",skin.name());p.setProperty("ledColor",""+ledColor);p.setProperty("mainLights",Boolean.toString(mainLights)); p.setProperty("size", "" + size); p.setProperty("variant", variant.name());
        p.setProperty("primary", "" + primary); p.setProperty("secondary", "" + secondary);
        p.setProperty("front", front.name()); p.setProperty("rearEnabled", "" + rearEnabled); p.setProperty("rear", rear.name());
        p.setProperty("leftGlass", "" + leftGlass); p.setProperty("rightGlass", "" + rightGlass); p.setProperty("curvedGlass", "" + curvedGlass);
        p.setProperty("wood", wood); p.setProperty("floor", floor); p.setProperty("lighting", lighting.name()); p.setProperty("chime", chime.name());p.setProperty("handrail",handrail.name());
        p.setProperty("doorStyle",doorStyle.name());p.setProperty("glazing",glazing.name());p.setProperty("materialPrimary",materialPrimary);p.setProperty("materialSecondary",materialSecondary);
        p.setProperty("sideIndicatorStyle",sideIndicatorStyle.name());p.setProperty("indicatorStyle",indicatorStyle.name());p.setProperty("voxelCurve",Boolean.toString(voxelCurve));p.setProperty("compact",Boolean.toString(compact));p.setProperty("kit3",Boolean.toString(kit3));p.setProperty("frameType",frameType.name());p.setProperty("mechanism",mechanism.name());p.setProperty("twoSpeed",Boolean.toString(twoSpeed));p.setProperty("indicators",Boolean.toString(indicators));return p;
    }
    public static CabinConfiguration load(Properties p) {
        if (!Set.of("1","2","3","4","5").contains(p.getProperty("schema"))) throw new IllegalArgumentException("Unsupported cabin schema");
        if("1".equals(p.getProperty("schema"))&&p.getProperty("doorStyle","SOLID").equals("SOLID")&&p.getProperty("front","SOLID").equals("GLASS")){var copy=new Properties();copy.putAll(p);copy.setProperty("doorStyle","GLASS");p=copy;}
        return new CabinConfiguration(Integer.parseInt(p.getProperty("size")), Variant.valueOf(p.getProperty("variant")),
            Integer.parseInt(p.getProperty("primary")), Integer.parseInt(p.getProperty("secondary")),
            Door.valueOf(p.getProperty("front")), bool(p,"rearEnabled"), Door.valueOf(p.getProperty("rear")),
            bool(p,"leftGlass"), bool(p,"rightGlass"), bool(p,"curvedGlass"), p.getProperty("wood"), p.getProperty("floor"),
            Lighting.valueOf(p.getProperty("lighting")), Chime.valueOf(p.getProperty("chime")),Handrail.valueOf(p.getProperty("handrail","BRUSHED")),DoorStyle.valueOf(p.getProperty("doorStyle","SOLID")),Glazing.valueOf(p.getProperty("glazing","SOLID")),p.getProperty("materialPrimary","minecraft:iron_block"),p.getProperty("materialSecondary","minecraft:polished_andesite"),FrameType.valueOf(p.getProperty("frameType","STANDARD")),Mechanism.valueOf(p.getProperty("mechanism","CENTER")),optionalBool(p,"twoSpeed",false),optionalBool(p,"indicators",true),optionalBool(p,"kit3",false),optionalBool(p,"compact",false),optionalBool(p,"voxelCurve",false),IndicatorStyle.valueOf(p.getProperty("indicatorStyle","PIXEL_MATRIX")),approvedSkin(p.getProperty("skin","MODERN_DARK")),Integer.parseInt(p.getProperty("ledColor", "9")),optionalBool(p,"mainLights",true),IndicatorStyle.valueOf(p.getProperty("sideIndicatorStyle",p.getProperty("indicatorStyle","PIXEL_MATRIX"))),loadColors(p),Integer.parseInt(p.getProperty("brightness","15")),optionalBool(p,"legacyGeometry",true),optionalBool(p,"v13Geometry",false));
    }
    public static Skin legacySkin(Variant v,DoorStyle s){return Skin.MODERN_DARK;}
    public static DoorStyle doorModel(DoorStyle s,Door door,Glazing glass){return switch(s){case GLASS,DIVIDED_GLASS->DoorStyle.GLASS;case SLIM_WINDOW,INDUSTRIAL,DARK_MODERN,CLINICAL,TRANSIT,HERITAGE,COASTAL,MINIMAL->DoorStyle.GLASS;case COPYCAT->glass==Glazing.FULL_GLASS?DoorStyle.GLASS:glass==Glazing.HALF_GLASS?DoorStyle.GLASS:DoorStyle.SOLID;case CLASSIC_BRUSHED_STEEL,OG_STEEL,CLASSIC_WOOD->s;case SOLID->DoorStyle.SOLID;default->DoorStyle.SOLID;};}
    private static boolean optionalBool(Properties p,String key,boolean fallback){return p.containsKey(key)?bool(p,key):fallback;}
    private static boolean bool(Properties p, String key) {
        var value = p.getProperty(key);
        if (!"true".equals(value) && !"false".equals(value)) throw new IllegalArgumentException("Invalid boolean: " + key);
        return Boolean.parseBoolean(value);
    }
}
