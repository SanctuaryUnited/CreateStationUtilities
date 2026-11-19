package com.sanctuarygames.createtransit.client;
/** Generated from tools/gondola_geometry.py. */
final class GondolaMesh {
record Cube(double x1,double y1,double z1,double x2,double y2,double z2,String texture,int tint,int faces) {}
static final Cube[] LEAF={
new Cube(1,0,1.5,15,1,2.5,"gondola_metal",0,63),
new Cube(1,1,1.5,15,4,2.5,"gondola_accent",1,63),
new Cube(1,4,1.5,15,4.8,2.5,"gondola_metal",0,63),
new Cube(1,4.8,1.5,1.7,31,2.5,"gondola_metal",0,63),
new Cube(14.3,4.8,1.5,15,31,2.5,"gondola_metal",0,63),
new Cube(1.7,29.5,1.5,14.3,31,2.5,"gondola_metal",0,63),
new Cube(1.7,4.8,1.7,2,29.5,2.3,"gondola_seal",-1,63),
new Cube(14,4.8,1.7,14.3,29.5,2.3,"gondola_seal",-1,63),
new Cube(2,4.8,1.9,14,29.5,2.1,"gondola_glass",-1,3),
};
static final Cube[] DOOR_LIGHTS={
new Cube(0.2,4,-0.045,0.8,30.4,0,"gondola_led",1,1),
new Cube(0.2,4,1.1,0.8,30.4,1.145,"gondola_led",1,2),
new Cube(15.2,4,-0.045,15.799999999999999,30.4,0,"gondola_led",1,1),
new Cube(15.2,4,1.1,15.799999999999999,30.4,1.145,"gondola_led",1,2),
};
static final Cube[] STATUS={
new Cube(5,31.2,-0.045,11,31.8,0,"led_white",-1,1),
new Cube(5,31.2,3,11,31.8,3.045,"led_white",-1,2),
};
static final Cube[] PILLAR_LIGHTS={
new Cube(0.8,0,-0.045,2.2,16,0,"gondola_led",1,1),
new Cube(0.8,0,3,2.2,16,3.045,"gondola_led",1,2),
};
static final Cube[] LEGACY_WIDE_LEFT={
new Cube(8,0,0.19999999999999996,24,1,1.2,"gondola_metal",0,63),
new Cube(8,1,0.19999999999999996,24,4,1.2,"gondola_accent",1,63),
new Cube(8,4,0.19999999999999996,24,4.8,1.2,"gondola_metal",0,63),
new Cube(8,4.8,0.19999999999999996,8.7,31,1.2,"gondola_metal",0,63),
new Cube(23.3,4.8,0.19999999999999996,24,31,1.2,"gondola_metal",0,63),
new Cube(8.7,29.5,0.19999999999999996,23.3,31,1.2,"gondola_metal",0,63),
new Cube(8.7,4.8,0.3999999999999999,9,29.5,0.9999999999999998,"gondola_seal",-1,63),
new Cube(23,4.8,0.3999999999999999,23.3,29.5,0.9999999999999998,"gondola_seal",-1,63),
new Cube(9,4.8,0.5999999999999999,23,29.5,0.8,"gondola_glass",-1,3),
new Cube(23.05,15.0,0.08,24,17.0,0.2,"gondola_seal",-1,61),
new Cube(23.05,15.0,1.2,24,17.0,1.32,"gondola_seal",-1,62),
};
static final Cube[] LEGACY_WIDE_RIGHT={
new Cube(24,0,0.19999999999999996,40,1,1.2,"gondola_metal",0,63),
new Cube(24,1,0.19999999999999996,40,4,1.2,"gondola_accent",1,63),
new Cube(24,4,0.19999999999999996,40,4.8,1.2,"gondola_metal",0,63),
new Cube(24,4.8,0.19999999999999996,24.7,31,1.2,"gondola_metal",0,63),
new Cube(39.3,4.8,0.19999999999999996,40,31,1.2,"gondola_metal",0,63),
new Cube(24.7,29.5,0.19999999999999996,39.3,31,1.2,"gondola_metal",0,63),
new Cube(24.7,4.8,0.3999999999999999,25,29.5,0.9999999999999998,"gondola_seal",-1,63),
new Cube(39,4.8,0.3999999999999999,39.3,29.5,0.9999999999999998,"gondola_seal",-1,63),
new Cube(25,4.8,0.5999999999999999,39,29.5,0.8,"gondola_glass",-1,3),
new Cube(24,15.0,0.08,24.95,17.0,0.2,"gondola_seal",-1,61),
new Cube(24,15.0,1.2,24.95,17.0,1.32,"gondola_seal",-1,62),
};
static final Cube[] LEGACY_WIDE_LIGHTS={
new Cube(6.6,4,-0.045,7.3999999999999995,30.4,0,"gondola_led",1,1),
new Cube(6.6,4,1.1,7.3999999999999995,30.4,1.145,"gondola_led",1,2),
new Cube(40.6,4,-0.045,41.4,30.4,0,"gondola_led",1,1),
new Cube(40.6,4,1.1,41.4,30.4,1.145,"gondola_led",1,2),
};
static final Cube[] LEGACY_WIDE_STATUS={
new Cube(19,31.2,-0.045,29,31.8,0,"led_white",-1,1),
new Cube(19,31.2,3,29,31.8,3.045,"led_white",-1,2),
};
static final Cube[] LEGACY_WIDE_CENTER_LEFT={
new Cube(23.2,15.35,0.025,23.9,16.65,0.08,"led_white",-1,1),
new Cube(23.2,15.35,1.32,23.9,16.65,1.375,"led_white",-1,2),
};
static final Cube[] LEGACY_WIDE_CENTER_RIGHT={
new Cube(24.1,15.35,0.025,24.8,16.65,0.08,"led_white",-1,1),
new Cube(24.1,15.35,1.32,24.8,16.65,1.375,"led_white",-1,2),
};
static final Cube[] WIDE_LEFT={
new Cube(8,0,0.19999999999999996,24,1,1.2,"gondola_metal",0,63),
new Cube(8,1,0.19999999999999996,24,4,1.2,"gondola_accent",1,63),
new Cube(8,4,0.19999999999999996,24,4.8,1.2,"gondola_metal",0,63),
new Cube(8,4.8,0.19999999999999996,8.7,47,1.2,"gondola_metal",0,63),
new Cube(23.3,4.8,0.19999999999999996,24,47,1.2,"gondola_metal",0,63),
new Cube(8.7,45.5,0.19999999999999996,23.3,47,1.2,"gondola_metal",0,63),
new Cube(8.7,4.8,0.3999999999999999,9,45.5,0.9999999999999998,"gondola_seal",-1,63),
new Cube(23,4.8,0.3999999999999999,23.3,45.5,0.9999999999999998,"gondola_seal",-1,63),
new Cube(9,4.8,0.5999999999999999,23,45.5,0.8,"gondola_glass",-1,3),
new Cube(23.05,23.0,0.08,24,25.0,0.2,"gondola_seal",-1,61),
new Cube(23.05,23.0,1.2,24,25.0,1.32,"gondola_seal",-1,62),
};
static final Cube[] WIDE_RIGHT={
new Cube(24,0,0.19999999999999996,40,1,1.2,"gondola_metal",0,63),
new Cube(24,1,0.19999999999999996,40,4,1.2,"gondola_accent",1,63),
new Cube(24,4,0.19999999999999996,40,4.8,1.2,"gondola_metal",0,63),
new Cube(24,4.8,0.19999999999999996,24.7,47,1.2,"gondola_metal",0,63),
new Cube(39.3,4.8,0.19999999999999996,40,47,1.2,"gondola_metal",0,63),
new Cube(24.7,45.5,0.19999999999999996,39.3,47,1.2,"gondola_metal",0,63),
new Cube(24.7,4.8,0.3999999999999999,25,45.5,0.9999999999999998,"gondola_seal",-1,63),
new Cube(39,4.8,0.3999999999999999,39.3,45.5,0.9999999999999998,"gondola_seal",-1,63),
new Cube(25,4.8,0.5999999999999999,39,45.5,0.8,"gondola_glass",-1,3),
new Cube(24,23.0,0.08,24.95,25.0,0.2,"gondola_seal",-1,61),
new Cube(24,23.0,1.2,24.95,25.0,1.32,"gondola_seal",-1,62),
};
static final Cube[] WIDE_LIGHTS={
new Cube(6.6,4,-0.045,7.3999999999999995,46.4,0,"gondola_led",1,1),
new Cube(6.6,4,1.1,7.3999999999999995,46.4,1.145,"gondola_led",1,2),
new Cube(40.6,4,-0.045,41.4,46.4,0,"gondola_led",1,1),
new Cube(40.6,4,1.1,41.4,46.4,1.145,"gondola_led",1,2),
};
static final Cube[] WIDE_STATUS={
new Cube(19,47.2,-0.045,29,47.8,0,"led_white",-1,1),
new Cube(19,47.2,3,29,47.8,3.045,"led_white",-1,2),
};
static final Cube[] WIDE_CENTER_LEFT={
new Cube(23.2,23.35,0.025,23.9,24.65,0.08,"led_white",-1,1),
new Cube(23.2,23.35,1.32,23.9,24.65,1.375,"led_white",-1,2),
};
static final Cube[] WIDE_CENTER_RIGHT={
new Cube(24.1,23.35,0.025,24.8,24.65,0.08,"led_white",-1,1),
new Cube(24.1,23.35,1.32,24.8,24.65,1.375,"led_white",-1,2),
};
static final Cube[] LED_PILLAR_BOTTOM={
new Cube(0.8,0,-0.045,2.2,16,0,"gondola_led",1,1),
new Cube(0.8,0,3,2.2,16,3.045,"gondola_led",1,2),
};
static final Cube[] LED_PILLAR_TOP={
new Cube(0.8,0,-0.045,2.2,16,0,"gondola_led",1,1),
new Cube(0.8,0,3,2.2,16,3.045,"gondola_led",1,2),
};
static final Cube[] LED_VERTICAL_BOTTOM={
new Cube(7,0,-0.045,9,16,0,"gondola_led",1,1),
new Cube(7,0,1.5,9,16,1.545,"gondola_led",1,2),
};
static final Cube[] LED_VERTICAL_TOP={
new Cube(7,0,-0.045,9,16,0,"gondola_led",1,1),
new Cube(7,0,1.5,9,16,1.545,"gondola_led",1,2),
};
static final Cube[] LED_VERTICAL_DOUBLE_BOTTOM={
new Cube(1.8,0,-0.045,3.2,16,0,"gondola_led",1,1),
new Cube(12.8,0,-0.045,14.2,16,0,"gondola_led",1,1),
new Cube(1.8,0,1.5,3.2,16,1.545,"gondola_led",1,2),
new Cube(12.8,0,1.5,14.2,16,1.545,"gondola_led",1,2),
};
static final Cube[] LED_VERTICAL_DOUBLE_TOP={
new Cube(1.8,0,-0.045,3.2,16,0,"gondola_led",1,1),
new Cube(12.8,0,-0.045,14.2,16,0,"gondola_led",1,1),
new Cube(1.8,0,1.5,3.2,16,1.545,"gondola_led",1,2),
new Cube(12.8,0,1.5,14.2,16,1.545,"gondola_led",1,2),
};
static final Cube[] LED_VERTICAL_CORNER_BOTTOM={
new Cube(0.8,0,-0.045,2.2,16,0,"gondola_led",1,1),
new Cube(-0.045,0,0.8,0,16,2.2,"gondola_led",1,4),
new Cube(0.8,0,3,2.2,16,3.045,"gondola_led",1,2),
new Cube(3,0,0.8,3.045,16,2.2,"gondola_led",1,8),
};
static final Cube[] LED_VERTICAL_CORNER_TOP={
new Cube(0.8,0,-0.045,2.2,16,0,"gondola_led",1,1),
new Cube(-0.045,0,0.8,0,16,2.2,"gondola_led",1,4),
new Cube(0.8,0,3,2.2,16,3.045,"gondola_led",1,2),
new Cube(3,0,0.8,3.045,16,2.2,"gondola_led",1,8),
};
static final Cube[] LED_HORIZONTAL_BOTTOM={
new Cube(0,0.55,-0.045,16,1.45,0,"gondola_led",1,1),
new Cube(0,0.55,3,16,1.45,3.045,"gondola_led",1,2),
};
static final Cube[] LED_HORIZONTAL_TOP={
new Cube(0,14.55,-0.045,16,15.45,0,"gondola_led",1,1),
new Cube(0,14.55,3,16,15.45,3.045,"gondola_led",1,2),
};
static final Cube[] LED_HORIZONTAL_DOUBLE_BOTTOM={
new Cube(0,0.55,-0.045,16,1.45,0,"gondola_led",1,1),
new Cube(0,2.55,-0.045,16,3.45,0,"gondola_led",1,1),
new Cube(0,0.55,3,16,1.45,3.045,"gondola_led",1,2),
new Cube(0,2.55,3,16,3.45,3.045,"gondola_led",1,2),
};
static final Cube[] LED_HORIZONTAL_DOUBLE_TOP={
new Cube(0,14.55,-0.045,16,15.45,0,"gondola_led",1,1),
new Cube(0,12.55,-0.045,16,13.45,0,"gondola_led",1,1),
new Cube(0,14.55,3,16,15.45,3.045,"gondola_led",1,2),
new Cube(0,12.55,3,16,13.45,3.045,"gondola_led",1,2),
};
static final Cube[] LED_HORIZONTAL_CORNER_BOTTOM={
new Cube(0,0.55,-0.045,16,1.45,0,"gondola_led",1,1),
new Cube(-0.045,0.55,0,0,1.45,16,"gondola_led",1,4),
new Cube(3,0.55,3,16,1.45,3.045,"gondola_led",1,2),
new Cube(3,0.55,3,3.045,1.45,16,"gondola_led",1,8),
};
static final Cube[] LED_HORIZONTAL_CORNER_TOP={
new Cube(0,14.55,-0.045,16,15.45,0,"gondola_led",1,1),
new Cube(-0.045,14.55,0,0,15.45,16,"gondola_led",1,4),
new Cube(3,14.55,3,16,15.45,3.045,"gondola_led",1,2),
new Cube(3,14.55,3,3.045,15.45,16,"gondola_led",1,8),
};
static final Cube[] LED_VERTICAL_OUTER_CORNER_BOTTOM={
new Cube(14.55,0,16,16.045,16,16.045,"gondola_led",1,2),
new Cube(16,0,14.55,16.045,16,16.045,"gondola_led",1,8),
};
static final Cube[] LED_VERTICAL_OUTER_CORNER_TOP={
new Cube(14.55,0,16,16.045,16,16.045,"gondola_led",1,2),
new Cube(16,0,14.55,16.045,16,16.045,"gondola_led",1,8),
};
static final Cube[] LED_HORIZONTAL_OUTER_CORNER_BOTTOM={
new Cube(14.55,0,16,16.045,1.45,16.045,"gondola_led",1,2),
new Cube(16,0,14.55,16.045,1.45,16.045,"gondola_led",1,8),
new Cube(13,0.55,16,14.55,1.45,16.045,"gondola_led",1,2),
new Cube(16,0.55,13,16.045,1.45,14.55,"gondola_led",1,8),
};
static final Cube[] LED_HORIZONTAL_OUTER_CORNER_TOP={
new Cube(14.55,14.55,16,16.045,16,16.045,"gondola_led",1,2),
new Cube(16,14.55,14.55,16.045,16,16.045,"gondola_led",1,8),
new Cube(13,14.55,16,14.55,15.45,16.045,"gondola_led",1,2),
new Cube(16,14.55,13,16.045,15.45,14.55,"gondola_led",1,8),
};
}
