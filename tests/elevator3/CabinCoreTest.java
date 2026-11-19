import com.sanctuarygames.createtransit.elevator.model.*;
import static com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.*;
import static com.sanctuarygames.createtransit.elevator.model.CabinMotionController.*;
import java.util.*;
public class CabinCoreTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 static void rejects(Runnable r){try{r.run();throw new AssertionError("Expected invalid input rejection");}catch(IllegalArgumentException expected){}}
 static final Settings SETTINGS=new Settings(.2,.01,4,5);
 static final List<Stop> STOPS=List.of(new Stop(0,"G",true,true,false),new Stop(8,"F1",true,false,true),new Stop(16,"F2",true,true,true));
 static class World implements CabinMotionController.World {
  boolean loaded=true,closed=true,obstruction=false,clear=true,front,rear;double actual;
  public boolean pathLoaded(double from,double to){return loaded;}
  public boolean doorsClosed(){return closed;}
  public boolean doorwayObstructed(){return obstruction;}
  public boolean canMove(double from,double to){return clear;}
  public void move(double from,double to){check(from==actual,"Collision and controller start aligned");check(Math.abs(to-from)<=.20000001,"Speed bounded");actual=to;}
  public void doors(boolean f,boolean r){front=f;rear=r;closed=!(f||r);}
 }
 static CabinMotionController engine(){var e=new CabinMotionController(0,SETTINGS,true);e.floors(STOPS);return e;}
 static void tickUntil(CabinMotionController e,World w,java.util.function.BooleanSupplier condition){for(int i=0;i<2000;i++){e.tick(w);if(condition.getAsBoolean())return;}throw new AssertionError("Transition timed out: "+e.snapshot());}
 public static void main(String[] args)throws Exception{
 for(var style:IndicatorStyle.values())for(int size=2;size<=5;size++)for(var light:Lighting.values()){
 var p=CabinConfiguration.defaults(size,Variant.SOLID).save();p.setProperty("compact","true");p.setProperty("indicatorStyle",style.name());p.setProperty("lighting",light.name());var c=CabinConfiguration.load(p);check(CabinConfiguration.load(c.save()).equals(c),"Indicator styles persist independently of doors");check(!DoorGeometry.telescoping(c),"Compact cabins do not force telescoping");for(var face:CabinGeometry.body(c))if(face.material()==CabinGeometry.Material.LIGHT&&face.a().y()>2.2)for(var vertex:List.of(face.a(),face.b(),face.c(),face.d()))check(vertex.z()>=.58-1e-9,"Ceiling lights stay clear of front pocket");for(double progress:new double[]{0,.5,1})for(var box:DoorGeometry.panelBoxes(c,.055,.125,progress))check(box.x0()>=0&&box.x1()<=size&&box.z0()==.055,"Regular leaves stay in pocket envelope and move sideways");p.setProperty("twoSpeed","true");check(!DoorGeometry.telescoping(CabinConfiguration.load(p)),"Legacy two-speed migrates to standard center doors");}
 System.out.println("PASS: 168 indicator/size/lighting combinations; light-pocket clearance; standard center doors and legacy mechanism migration; persisted styles.");

  int combinations=0;
  for(int size=2;size<=5;size++)for(var variant:Variant.values())for(var front:Door.values())for(var rear:Door.values())for(boolean back:List.of(false,true)){
   if(back&&variant==Variant.CYLINDRICAL){int selectedSize=size;rejects(()->new CabinConfiguration(selectedSize,variant,0,3,front,true,rear,true,false,true,"minecraft:oak_planks","minecraft:stone",Lighting.RING,Chime.OTIS));continue;}
   var c=new CabinConfiguration(size,variant,0,3,front,back,rear,true,false,true,"example:custom_planks","minecraft:stone",Lighting.RING,Chime.OTIS);
   check(CabinConfiguration.load(c.save()).equals(c),"Every permitted combination round-trips");
   for(var collection:List.of(CabinGeometry.body(c),CabinGeometry.doors(c,0,0),CabinGeometry.doors(c,1,1)))for(var face:collection)for(var point:List.of(face.a(),face.b(),face.c(),face.d()))check(Double.isFinite(point.x())&&Double.isFinite(point.y())&&Double.isFinite(point.z())&&point.x()>=-1e-6&&point.z()>=-1e-6&&point.x()<=size+1e-6&&point.z()<=size+1e-6&&point.y()>=-1e-6&&point.y()<=3.25+1e-6,"Open and closed geometry stays inside the external footprint");
   check(!back||!c.rearGlass(),"Rear window never overlaps rear doors");
   if(variant==Variant.SOLID)check(!c.leftGlazed()&&!c.rightGlazed(),"Glass doors do not turn solid walls into glass");combinations++;
  }
  var bad=defaults(3,Variant.WOOD).save();bad.setProperty("rearEnabled","oops");rejects(()->CabinConfiguration.load(bad));
  for(var chime:Chime.values()){var selection=defaults(3,Variant.SOLID).save();selection.setProperty("chime",chime.name());check(CabinConfiguration.load(selection).chime()==chime,"Every chime, including legacy presets and Off, survives saved configuration");}
  var e=engine();var w=new World();check(e.request(8)&&e.request(8)&&e.request(16),"Requests accepted");check(e.snapshot().queue().size()==2,"Duplicate requests coalesce");
  w.obstruction=true;for(int i=0;i<30;i++)e.tick(w);check(w.actual==0&&w.front,"Obstructed doors reopen without movement");w.obstruction=false;
  tickUntil(e,w,()->e.snapshot().state()==State.MOVING&&w.actual>2);
  var before=e.snapshot();w.loaded=false;for(int i=0;i<30;i++)e.tick(w);check(e.snapshot().equals(before),"Unloaded path freezes movement and timers");
  var serialized=new java.io.StringWriter();CabinMotionController.saveSnapshot(before).store(serialized,"test");var props=new Properties();props.load(new java.io.StringReader(serialized.toString()));
  var loaded=CabinMotionController.loadSnapshot(props);check(loaded.equals(before),"Moving state survives text serialization with queued requests");
  var resumed=engine();resumed.restore(loaded);w.loaded=true;
  tickUntil(resumed,w,()->resumed.snapshot().state()==State.DOORS_OPEN&&w.actual==8);check(!w.front&&w.rear,"Rear-only landing opens rear only");
  tickUntil(resumed,w,()->resumed.snapshot().state()==State.DOORS_OPEN&&w.actual==16);check(w.front&&w.rear,"Both sides open at configured landing");
  check(resumed.request(0),"Downward request accepted");tickUntil(resumed,w,()->resumed.snapshot().state()==State.DOORS_OPEN&&w.actual==0);check(w.front&&!w.rear,"Front-only ground landing");
  var blocked=engine();var bw=new World();blocked.request(8);bw.clear=false;for(int i=0;i<50;i++)blocked.tick(bw);check(blocked.snapshot().state()==State.FAULT&&bw.actual==0,"Collision prevents movement");
  var doorFault=engine();var dw=new World();doorFault.request(8);tickUntil(doorFault,dw,()->doorFault.snapshot().state()==State.MOVING);dw.closed=false;doorFault.tick(dw);check(doorFault.snapshot().state()==State.FAULT,"Open door interlock faults travel");
  rejects(()->new Snapshot(State.DOORS_OPENING,0,0,null,List.of(),0,""));
  rejects(()->new Snapshot(State.DOORS_OPENING,4,0,8,List.of(),0,""));
  rejects(()->new Snapshot(State.IDLE,0,.1,null,List.of(),0,""));
  var deleted=engine();var deletedWorld=new World();deleted.request(8);tickUntil(deleted,deletedWorld,()->deleted.snapshot().state()==State.MOVING);deleted.floors(List.of(STOPS.get(0),STOPS.get(2)));check(deleted.snapshot().state()==State.FAULT,"Removed destination faults before further travel");
  var finalEngine=engine();rejects(()->finalEngine.floors(List.of(new Stop(0,"G",true,true,false),new Stop(5,"g",true,true,false))));
  check(!finalEngine.request(99),"Unknown floor rejected");
  double[] peaks=new double[2];int trial=0;for(int destination:new int[]{4,120}){var run=new CabinMotionController(0,new Settings(.6,.01,0,0),false);run.floors(List.of(new Stop(0,"G",true,true,false),new Stop(destination,"T",true,true,false)));run.request(destination);final double[] peak={0};var world=new World(){public void move(double from,double to){check(from==actual,"Dynamic profile aligns exactly");check(Math.abs(to-from)<=.60000001,"Dynamic maximum bound");actual=to;peak[0]=Math.max(peak[0],Math.abs(to-from));}};tickUntil(run,world,()->run.snapshot().state()==State.DOORS_OPEN);check(world.actual==destination,"Dynamic exact destination");peaks[trial++]=peak[0];}check(peaks[1]>.59&&peaks[0]<.3,"Long trips reach maximum, short trips stay gentle");
  var ordered=new CabinMotionController(0,SETTINGS,false);
  var floorSet=List.of(new Stop(0,"F1",true,true,false),new Stop(4,"F2",true,true,false),
      new Stop(8,"F3",true,true,false),new Stop(12,"F4",true,true,false),new Stop(16,"F5",true,true,false));
  ordered.floors(floorSet);
  var orderedWorld=new World();
  ordered.request(16);
  tickUntil(ordered,orderedWorld,()->ordered.snapshot().state()==State.MOVING&&orderedWorld.actual>1);
  ordered.request(12);ordered.request(8);ordered.request(8);ordered.request(0);
  for(int next:new int[]{8,12,16,0}){
   tickUntil(ordered,orderedWorld,()->ordered.snapshot().state()==State.DOORS_OPEN&&orderedWorld.actual==next);
  }
  check(ordered.snapshot().queue().isEmpty(),"Direction sweeps serve intermediate and reverse stops once");
  var legacy=CabinMotionController.saveSnapshot(before);
  legacy.setProperty("schema","1");legacy.remove("direction");legacy.remove("ambient");legacy.remove("boardingDestination");
  var migrated=engine();migrated.restore(CabinMotionController.loadSnapshot(legacy));
  check(migrated.snapshot().serviceDirection()==1&&migrated.snapshot().queue().equals(before.queue()),"Legacy move resumes with direction and pending stops");
  for(var style:List.of(DoorStyle.CLASSIC_BRUSHED_STEEL,DoorStyle.OG_STEEL,DoorStyle.CLASSIC_WOOD))
   for(var glazing:List.of(Glazing.SOLID,Glazing.FULL_GLASS,Glazing.SLIM_GLASS)){
    var props18=defaults(3,Variant.SOLID).save();
    props18.setProperty("doorStyle",style.name());props18.setProperty("glazing",glazing.name());
    var config=CabinConfiguration.load(props18);
    var faces=CabinGeometry.doors(config,0,0);
    check(faces.stream().anyMatch(face->face.material()==CabinGeometry.Material.DOOR_GLASS)==
        (style!=DoorStyle.CLASSIC_WOOD&&glazing!=Glazing.SOLID),"Glazing follows style and wood remains solid");
   }
  System.out.println("PASS: direction-aware sweep, in-motion insertion, reversal, legacy motion schema, and nine style/glazing combinations.");
  System.out.println("PASS: dynamic short/long trip peaks and exact alignment.");
  System.out.println("PASS: "+combinations+" cabin setting combinations; all "+Chime.values().length+" chime selections persist; trips both directions; per-floor doors; obstruction reopening; unload pause; snapshot recovery; collision and door interlocks. Pure core check; native integration requires a Minecraft runtime.");
 }
 static int sizeCopy(int x){return x;}
}
