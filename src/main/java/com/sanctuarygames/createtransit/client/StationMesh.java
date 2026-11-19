package com.sanctuarygames.createtransit.client;
/** Generated from tools/station_geometry.py. */
final class StationMesh {
record Cube(double x1,double y1,double z1,double x2,double y2,double z2,String texture,int tint,int faces) {}
static final Cube[] GATE_LIGHTS = {
new Cube(2,12,-0.04,6,16,0,"fare_arrow",-1,1),
new Cube(2,12,16,6,16,16.04,"fare_stop",-1,2),
new Cube(10,12,-0.04,14,16,0,"fare_arrow",-1,1),
new Cube(10,12,16,14,16,16.04,"fare_stop",-1,2),
new Cube(7.5,5,-0.04,8.5,10.5,0,"led_green",-1,1),
new Cube(7.5,5,16,8.5,10.5,16.04,"led_red",-1,2),
new Cube(6,17,-0.04,10,19,0,"kiosk_reader",-1,1),
new Cube(0.8,3,-0.035,1.4,19,0.0,"led_accent",1,1),
new Cube(14.6,3,-0.035,15.2,19,0.0,"led_accent",1,1),
new Cube(0.8,3,16,1.4,19,16.035,"led_accent",1,2),
new Cube(14.6,3,16,15.2,19,16.035,"led_accent",1,2),
};
static final Cube[] KIOSK_LIGHTS = {
new Cube(1.2,2,2.96,1.8,29,3,"led_accent",1,1),
new Cube(14.2,2,2.96,14.8,29,3,"led_accent",1,1),
new Cube(2,2,2.96,14,2.6,3,"led_accent",1,1),
};
static final Cube[] LEAF = {
new Cube(0,3,7.5,0.6,18.7,8.5,"frame",0,63),
new Cube(15.3,3,7.5,15.9,18.7,8.5,"frame",0,63),
new Cube(0.6,3,7.5,15.3,3.7,8.5,"frame",0,63),
new Cube(0.6,18,7.5,15.3,18.7,8.5,"frame",0,63),
new Cube(0.6,3.7,7.9,15.3,18,8.1,"glass",-1,3),
};
}
