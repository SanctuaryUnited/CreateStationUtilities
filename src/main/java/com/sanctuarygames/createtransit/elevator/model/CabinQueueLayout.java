package com.sanctuarygames.createtransit.elevator.model;

/** Mount a queue panel on the straight cabin wall, clear of the doorway and rear return. */
public record CabinQueueLayout(double x,double z,double width) {
 public static CabinQueueLayout mount(CabinConfiguration cabin,boolean right,boolean vertical){
  double wall=cabin.v13Geometry()?CabinGeometry.INSET:2*CabinGeometry.INSET;
  double start=cabin.v13Geometry()?.54:CabinGeometry.passageEnd(cabin,false)+.06;
  double end=cabin.variant()==CabinConfiguration.Variant.CYLINDRICAL
      ?cabin.size()/2-CabinGeometry.INSET-.025: cabin.size()-CabinGeometry.PASSAGE_DEPTH-.06;
  double width=Math.min(vertical?.37:.72,end-start);
  return new CabinQueueLayout(right?cabin.size()-wall-.009:wall+.009,(start+end)/2,width);
 }
}
