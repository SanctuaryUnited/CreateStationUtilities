package com.sanctuarygames.createtransit;

import com.google.gson.GsonBuilder;
import com.sanctuarygames.createtransit.escalator.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Compare the exact previously delivered collision path, including look-at
 * ray casts and selection edges. No graphics or FPS measurement. */
public final class EscalatorInclineCheck {
    private static volatile double blackhole;
    private static final Vec3 START=new Vec3(.5,2.8,.3), END=new Vec3(.5,-.5,.3);
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static VoxelShape shape(boolean old,boolean left,boolean right,Direction facing,double phase){
        return old?Alpha163Shapes.shape(EscalatorBlock.Kind.INCLINE,left,right,facing,phase)
            :EscalatorShapes.shape(EscalatorBlock.Kind.INCLINE,left,right,facing,phase);
    }
    private static List<double[]> edges(VoxelShape shape){
        var result=new ArrayList<double[]>();shape.forAllEdges((x,y,z,X,Y,Z)->result.add(new double[]{x,y,z,X,Y,Z}));
        result.sort((a,b)->{for(int i=0;i<6;i++){int difference=Double.compare(a[i],b[i]);if(difference!=0)return difference;}return 0;});
        return result;
    }
    private static void equivalence(){
        int shapes=0,hits=0;
        var random=new Random(164);
        for(int sample=0;sample<20;sample++)for(int sides=0;sides<4;sides++)for(var facing:Direction.Plane.HORIZONTAL){
            double phase=sample==0?0:sample==1?.25:random.nextDouble();
            var before=shape(true,(sides&1)!=0,(sides&2)!=0,facing,phase);
            var after=shape(false,(sides&1)!=0,(sides&2)!=0,facing,phase);
            check(!Shapes.joinIsNotEmpty(before,after,BooleanOp.NOT_SAME),"Exact physical volume "+sides+"/"+facing+"/"+phase);shapes++;
            var oldEdges=edges(before);var newEdges=edges(after);var repeatedEdges=edges(after);
            check(oldEdges.size()==newEdges.size(),"Selection outline edge count unchanged");
            for(int edge=0;edge<oldEdges.size();edge++)for(int axis=0;axis<6;axis++){
                check(Math.abs(oldEdges.get(edge)[axis]-newEdges.get(edge)[axis])<1e-7,"Selection outline positions unchanged");
                check(newEdges.get(edge)[axis]==repeatedEdges.get(edge)[axis],"Repeated selection outline is stable");
            }
            for(double x:new double[]{.05,.5,.95})for(double z:new double[]{.1,.3,.7,.9}){
                var a=new Vec3(x,2.8,z);var b=new Vec3(x,-.5,z);
                var oldHit=before.clip(a,b,BlockPos.ZERO);var newHit=after.clip(a,b,BlockPos.ZERO);
                check((oldHit==null)==(newHit==null),"Ray cast hit/miss unchanged");
                if(oldHit!=null)check(oldHit.getDirection()==newHit.getDirection()&&oldHit.getLocation().distanceTo(newHit.getLocation())<1e-7,"Look-at surface and direction unchanged");hits++;
            }
        }
        System.out.println("PASS: "+shapes+" additional exact incline volumes and "+hits+" native selection ray casts.");
    }
    private static final class EdgeSink implements Shapes.DoubleLineConsumer{
        double sum;long edges;
        public void consume(double x,double y,double z,double X,double Y,double Z){sum+=x+y+z+X+Y+Z;edges++;}
    }
    private static void work(boolean old,int ticks,int sections,boolean independent,Direction facing,boolean selection){
        double sum=0;var edges=new EdgeSink();
        for(int tick=0;tick<ticks;tick++)for(int section=0;section<sections;section++){
            double phase=EscalatorGeometry.wrap(tick*EscalatorGeometry.SPEED+(independent?section*.07123:0));
            boolean left=sections>1&&section%2!=0,right=sections>1&&!left;
            var collider=shape(old,left,right,facing,phase);sum+=collider.max(Direction.Axis.Y);
            if(selection)for(int frame=0;frame<4;frame++){
                var hit=collider.clip(START,END,BlockPos.ZERO);if(hit!=null)sum+=hit.getLocation().y;
                collider.forAllEdges(edges);
            }
        }
        blackhole=sum+edges.sum+edges.edges;
    }
    private record Measurement(double milliseconds,long allocatedBytes){}
    private static Measurement measure(com.sun.management.ThreadMXBean bean,Runnable work){
        long thread=Thread.currentThread().threadId(),bytes=bean.getThreadAllocatedBytes(thread),time=System.nanoTime();
        work.run();return new Measurement((System.nanoTime()-time)/1e6,bean.getThreadAllocatedBytes(thread)-bytes);
    }
    public static void main(String[] args)throws Exception{
        equivalence();var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);
        var scenarios=new LinkedHashMap<String,Object>();
        for(int sections:new int[]{1,12})for(var facing:new Direction[]{Direction.NORTH,Direction.EAST})for(boolean independent:new boolean[]{false,true})for(boolean selection:new boolean[]{true}){
            if(sections==1&&independent)continue;
            for(int warm=0;warm<1;warm++){work(true,3,sections,independent,facing,selection);work(false,3,sections,independent,facing,selection);}
            var before=new ArrayList<Measurement>();var after=new ArrayList<Measurement>();
            for(int sample=0;sample<3;sample++){
                if(sample%2==0){before.add(measure(bean,()->work(true,8,sections,independent,facing,selection)));after.add(measure(bean,()->work(false,8,sections,independent,facing,selection)));}
                else{after.add(measure(bean,()->work(false,8,sections,independent,facing,selection)));before.add(measure(bean,()->work(true,8,sections,independent,facing,selection)));}
            }
            System.out.println("Completed incline benchmark: "+sections+" sections "+facing+" independent="+independent);
            scenarios.put(sections+"_sections_"+facing.name().toLowerCase()+"_"+(independent?"independent":"shared")+"_"+(selection?"collision_and_selection":"collision"),Map.of("alpha16_3",before,"alpha16_4",after));
        }
        var report=Map.of("scope","Alpha16.3 versus Alpha16.4 native CPU/allocation work. Exact collision generation, optional clip ray and selection-edge enumeration at 80 FPS/20 TPS; no GPU, shader, draw call or actual FPS measurement. Three alternating samples after warmup.","ticks_per_sample",8,"scenarios",scenarios);
        var json=new GsonBuilder().setPrettyPrinting().create().toJson(report);var output=Path.of(args[0]);Files.createDirectories(output.getParent());Files.writeString(output,json+"\n");
        System.out.println("PASS: incline collision and look-at CPU/allocation benchmark written to "+output);
    }
}
