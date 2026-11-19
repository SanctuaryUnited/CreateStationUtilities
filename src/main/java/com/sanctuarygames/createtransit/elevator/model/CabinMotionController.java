package com.sanctuarygames.createtransit.elevator.model;

import java.util.*;

/** Server-side engine core used by the Minecraft model-cabin controller adapter.
 * All coordinates are absolute cabin-floor heights. Rendering must consume snapshots only. */
public final class CabinMotionController {
    public enum State { IDLE, DOORS_CLOSING, MOVING, LEVELING, DOORS_OPENING, DOORS_OPEN, FAULT }
    public record Stop(int y, String id, boolean enabled, boolean front, boolean rear) {
        public Stop { if (id == null || id.isBlank() || id.length() > 32 || id.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Invalid floor ID"); }
    }
    public record Settings(double maxSpeed, double acceleration, int openingDelay, int closingDelay) {
        public Settings { if (!Double.isFinite(maxSpeed) || !Double.isFinite(acceleration) || maxSpeed <= 0 || maxSpeed > 1 || acceleration <= 0 || acceleration > .1 || openingDelay < 0 || openingDelay > 200 || closingDelay < 0 || closingDelay > 200) throw new IllegalArgumentException("Invalid movement settings"); }
    }
    /** Implementations must sweep the complete cabin and move its collision/riders atomically. */
    public interface World {
        boolean pathLoaded(double from, double to);
        boolean doorsClosed();
        default boolean doorsOpened(){return true;}
        boolean doorwayObstructed();
        boolean canMove(double from, double to);
        void move(double from, double to);
        void doors(boolean front, boolean rear);
    }
    public record Snapshot(State state, double y, double velocity, Integer target, List<Integer> queue, int elapsed, String fault,
                           int serviceDirection, boolean ambient, Integer boardingDestination) {
        public Snapshot(State state, double y, double velocity, Integer target, List<Integer> queue,
                        int elapsed, String fault) {
            this(state,y,velocity,target,queue,elapsed,fault,0,false,null);
        }
        public Snapshot {
            Objects.requireNonNull(state); Objects.requireNonNull(fault); queue = List.copyOf(queue);
            if (Math.abs((long)serviceDirection)>1 || ambient&&target==null
                    || boardingDestination!=null&&!queue.contains(boardingDestination))
                throw new IllegalArgumentException("Invalid service state");
            if (!Double.isFinite(y) || !Double.isFinite(velocity) || elapsed < 0 || new HashSet<>(queue).size() != queue.size()) throw new IllegalArgumentException("Invalid motion snapshot");
            if ((state == State.DOORS_CLOSING || state == State.MOVING || state == State.LEVELING || state == State.DOORS_OPENING) && target == null) throw new IllegalArgumentException("Active snapshot lacks destination");
            if ((state == State.IDLE || state == State.DOORS_OPEN) && target != null) throw new IllegalArgumentException("Inactive snapshot has a destination");
            if (state != State.MOVING && state != State.LEVELING && velocity != 0) throw new IllegalArgumentException("Stationary snapshot has velocity");
            if (state == State.DOORS_OPENING && Math.abs(y-target)>1e-8) throw new IllegalArgumentException("Opening snapshot is not aligned");
        }
    }
    /** Versioned persistence payload for the controller NBT adapter. */
    public static Properties saveSnapshot(Snapshot s) {
        var p=new Properties();p.setProperty("schema","2");p.setProperty("state",s.state().name());
        p.setProperty("y",Double.toString(s.y()));p.setProperty("velocity",Double.toString(s.velocity()));
        p.setProperty("target",s.target()==null?"":s.target().toString());p.setProperty("elapsed",Integer.toString(s.elapsed()));p.setProperty("fault",s.fault());
        p.setProperty("direction",Integer.toString(s.serviceDirection()));
        p.setProperty("ambient",Boolean.toString(s.ambient()));
        p.setProperty("boardingDestination",s.boardingDestination()==null?"":s.boardingDestination().toString());
        p.setProperty("queue",s.queue().stream().map(Object::toString).collect(java.util.stream.Collectors.joining(",")));return p;
    }
    public static Snapshot loadSnapshot(Properties p) {
        if(!"1".equals(p.getProperty("schema"))&&!"2".equals(p.getProperty("schema")))throw new IllegalArgumentException("Unsupported motion schema");
        String pending=Objects.requireNonNull(p.getProperty("queue"));
        var queue=new ArrayList<Integer>();if(!pending.isEmpty())for(var item:pending.split(",",-1))queue.add(Integer.parseInt(item));
        String target=Objects.requireNonNull(p.getProperty("target"));
        String boarding=p.getProperty("boardingDestination","");
        return new Snapshot(State.valueOf(p.getProperty("state")),Double.parseDouble(p.getProperty("y")),Double.parseDouble(p.getProperty("velocity")),target.isEmpty()?null:Integer.valueOf(target),queue,Integer.parseInt(p.getProperty("elapsed")),Objects.requireNonNull(p.getProperty("fault")),
                Integer.parseInt(p.getProperty("direction","0")),Boolean.parseBoolean(p.getProperty("ambient","false")),
                boarding.isEmpty()?null:Integer.valueOf(boarding));
    }
    private final Settings settings;
    private final boolean rearAllowed;
    private final NavigableMap<Integer,Stop> stops = new TreeMap<>();
    private final LinkedHashSet<Integer> queue = new LinkedHashSet<>();
    private State state = State.IDLE;
    private double y, velocity;
    private Integer target;
    private Integer boardingDestination;
    private int serviceDirection;
    private boolean ambient;
    private int elapsed;
    private String fault = "";
    public CabinMotionController(double y, Settings settings, boolean rearAllowed) {
        if (!Double.isFinite(y)) throw new IllegalArgumentException("Invalid initial height");
        this.y = y; this.settings = Objects.requireNonNull(settings); this.rearAllowed = rearAllowed;
    }
    public void floors(Collection<Stop> discovered) {
        var replacement = new TreeMap<Integer,Stop>(); var ids = new HashSet<String>();
        for (var stop : discovered) {
            if (replacement.putIfAbsent(stop.y(), stop) != null || !ids.add(stop.id().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Duplicate floor");
            if (stop.rear() && !rearAllowed) throw new IllegalArgumentException("Rear service is unavailable for this cabin");
        }
        stops.clear(); stops.putAll(replacement); queue.removeIf(q -> !serviceable(q));
        if (boardingDestination!=null&&!queue.contains(boardingDestination)) boardingDestination=null;
        if (target != null && !serviceable(target)) fail("Destination removed or disabled");
    }
    private boolean serviceable(int stop) { var s = stops.get(stop); return s != null && s.enabled() && (s.front() || s.rear()); }
    public boolean request(int floor) {
        if (state == State.FAULT || !serviceable(floor)) return false;
        if (Objects.equals(target,floor)) {
            ambient=false;
            return true;
        }
        queue.add(floor);
        if (state==State.DOORS_CLOSING&&(ambient
                ||(floor-y)*serviceDirection>0&&(target-floor)*serviceDirection>0)) {
            if(!ambient)queue.add(target);
            ambient=false;
            target=null;
            selectTarget();
        }
        return true;
    }
    boolean requestFirst(int floor) {
        if (state != State.DOORS_OPEN || !serviceable(floor) || Math.abs(y-floor)<1e-8) return false;
        queue.add(floor);
        boardingDestination=floor;
        return true;
    }
    boolean requestAmbient(int floor) {
        if (state!=State.IDLE||!queue.isEmpty()||!serviceable(floor)||Math.abs(y-floor)<1e-8) return false;
        target=floor;
        ambient=true;
        serviceDirection=Integer.compare(floor,(int)Math.rint(y));
        transition(State.DOORS_CLOSING);
        return true;
    }
    private List<Integer> orderedQueue() {
        if (queue.isEmpty()) return List.of();
        var remaining=new ArrayList<>(queue);
        var ordered=new ArrayList<Integer>(queue.size());
        double from=target==null?y:target;
        int direction=serviceDirection;
        if (boardingDestination!=null) {
            remaining.remove(boardingDestination);
            ordered.add(boardingDestination);
            direction=Double.compare(boardingDestination,from);
            from=boardingDestination;
        }
        if (direction==0) direction=Double.compare(remaining.isEmpty()?from:remaining.getFirst(),from);
        if (direction==0) direction=1;
        final double pivot=from;
        final int sweep=direction;
        final boolean serveCurrentFirst=target==null&&boardingDestination==null&&!remaining.isEmpty()&&remaining.getFirst()==pivot;
        remaining.sort((a,b)->{
            if(serveCurrentFirst&&(a==pivot||b==pivot))return Boolean.compare(b==pivot,a==pivot);
            boolean aheadA=(a-pivot)*sweep>0, aheadB=(b-pivot)*sweep>0;
            if (aheadA!=aheadB) return aheadA?-1:1;
            return Integer.compare(a,b)*sweep*(aheadA?1:-1);
        });
        ordered.addAll(remaining);
        return List.copyOf(ordered);
    }
    private void selectTarget() {
        target=orderedQueue().getFirst();
        queue.remove(target);
        if (Objects.equals(target,boardingDestination)) boardingDestination=null;
        int direction=Double.compare(target,y);
        if (direction!=0) serviceDirection=direction;
        transition(State.DOORS_CLOSING);
    }
    private void considerIntermediateStop() {
        if (boardingDestination!=null||queue.isEmpty()||target==null) return;
        int direction=Double.compare(target,y);
        if (direction==0) return;
        double speed=Math.abs(velocity);
        double stoppingDistance=speed*speed/(2*settings.acceleration())+speed;
        Integer candidate=null;
        for (int floor:queue) {
            double ahead=(floor-y)*direction;
            if (ahead<=1e-8||ahead<stoppingDistance||(target-floor)*direction<=0) continue;
            if (candidate==null||(candidate-floor)*direction>0) candidate=floor;
        }
        if (candidate==null) return;
        queue.remove(candidate);
        if (!ambient) queue.add(target);
        target=candidate;
        ambient=false;
        state=State.MOVING;
    }
    void cancelFailedRequest(int floor) {
        if (state != State.FAULT) return;
        queue.remove(floor);
        if (Objects.equals(boardingDestination,floor)) boardingDestination=null;
        if (Objects.equals(target, floor)) {target=null;ambient=false;}
    }
    private void transition(State next) { state = next; elapsed = 0; }
    private void fail(String reason) { fault = reason; velocity = 0; transition(State.FAULT); }
    public void fault(String reason){fail(reason);}
    public boolean retry(){if(state!=State.FAULT)return false;fault="";transition(target!=null&&serviceable(target)?State.DOORS_CLOSING:State.IDLE);if(target!=null&&!serviceable(target)){target=null;ambient=false;}return true;}
    void finishIdleClose(){if(state==State.DOORS_OPEN&&queue.isEmpty()){serviceDirection=0;transition(State.IDLE);}}
    public Snapshot snapshot() { return new Snapshot(state, y, velocity, target, orderedQueue(), elapsed, fault,serviceDirection,ambient,boardingDestination); }
    public void restore(Snapshot s) {
        if (Math.abs(s.velocity()) > settings.maxSpeed()) throw new IllegalArgumentException("Saved speed exceeds limit");
        for (int q : s.queue()) if (!serviceable(q)) throw new IllegalArgumentException("Saved queue refers to an unavailable floor");
        if(s.target()!=null&&s.queue().contains(s.target()))throw new IllegalArgumentException("Destination duplicated in saved queue");
        if (s.target() != null && !serviceable(s.target())) throw new IllegalArgumentException("Saved destination unavailable");
        state=s.state(); y=s.y(); velocity=s.velocity(); target=s.target(); elapsed=s.elapsed(); fault=s.fault(); queue.clear(); queue.addAll(s.queue());
        serviceDirection=s.serviceDirection();
        if(serviceDirection==0&&target!=null)serviceDirection=Double.compare(target,y);
        ambient=s.ambient();
        boardingDestination=s.boardingDestination();
    }
    public void tick(World w) {
        if (state == State.FAULT) return;
        double next = target == null ? y : y + Math.copySign(Math.min(Math.abs(target-y), settings.maxSpeed()), target-y);
        if (!w.pathLoaded(y,next)) return; // Freeze motion and timers; do not move through unloaded space.
        if(state!=State.IDLE&&state!=State.DOORS_OPEN)elapsed++;
        switch (state) {
            case IDLE, DOORS_OPEN -> {
                if (!queue.isEmpty()) selectTarget();
            }
            case DOORS_CLOSING -> {
                if (w.doorwayObstructed()) { var current=stops.get((int)Math.rint(y)); if(current==null || Math.abs(y-current.y())>1e-8) { fail("Cabin is not aligned to a registered landing"); return; } w.doors(current.front(), rearAllowed && current.rear()); elapsed=0;return; }
                w.doors(false,false);
                if (elapsed >= settings.closingDelay() && w.doorsClosed()) transition(State.MOVING);
                else if(elapsed>settings.closingDelay()+200)fail("Doors did not confirm closed");
            }
            case MOVING, LEVELING -> {
                if (!w.doorsClosed()) { fail("Door interlock opened during travel"); return; }
                if (target == null || !serviceable(target)) { fail("Destination unavailable"); return; }
                considerIntermediateStop();
                double distance = Math.abs(target-y);
                double speed = Math.min(settings.maxSpeed(), Math.min(Math.abs(velocity)+settings.acceleration()*Math.min(1,elapsed/20.0), Math.sqrt(2*settings.acceleration()*distance+settings.acceleration()*settings.acceleration())-settings.acceleration()));
                double to = y + Math.copySign(Math.min(distance,speed), target-y);
                if(Math.abs(target-to)<1e-8)to=target;
                if (!w.canMove(y,to)) { fail("Cabin sweep obstructed"); return; }
                double from=y; w.move(from,to); y=to;velocity=to-from;
                if (Math.abs(target-y) < 1e-8) { y=target;velocity=0;transition(State.DOORS_OPENING); }
                else if (distance <= settings.maxSpeed()) state=State.LEVELING;
            }
            case DOORS_OPENING -> {
                if (elapsed >= settings.openingDelay()) { var stop=stops.get(target);w.doors(stop.front(),rearAllowed && stop.rear());if(w.doorsOpened()){target=null;ambient=false;transition(State.DOORS_OPEN);} }
            }
            default -> { }
        }
    }
}
