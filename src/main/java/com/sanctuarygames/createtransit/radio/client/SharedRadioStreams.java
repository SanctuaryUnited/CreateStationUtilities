package com.sanctuarygames.createtransit.radio.client;
import java.io.*;import java.nio.*;import java.util.*;import java.util.concurrent.*;import javax.sound.sampled.AudioFormat;import net.minecraft.client.sounds.AudioStream;
/** One live decoder per URL, with an independent wall-clock consumer even without listeners. */
public final class SharedRadioStreams {
 public static final int LIMIT=12;
 private static final Map<String,Entry> entries=new LinkedHashMap<>();
 private static final ExecutorService CONNECT=Executors.newFixedThreadPool(LIMIT,r->{var t=new Thread(r,"Station radio warmup");t.setDaemon(true);return t;});
 private static final class Entry {final long created=System.currentTimeMillis();long used=created;int readers;volatile boolean canceled;final CompletableFuture<Hub> future;Entry(String url){future=CompletableFuture.supplyAsync(()->{try{return new Hub(RadioAudio.open(url,s->{},()->canceled));}catch(IOException e){throw new CompletionException(e);}},CONNECT);}}
 public static synchronized void warm(String url){entry(url);}
 private static Entry entry(String url){var e=entries.get(url);if(e!=null&&e.future.isDone()&&!e.future.isCompletedExceptionally()){var hub=e.future.getNow(null);if(hub!=null&&hub.source.ended()&&hub.source.bufferedBytes()==0&&e.readers==0){close(e);entries.remove(url);e=null;}}if(e!=null){e.used=System.currentTimeMillis();return e;}if(entries.size()>=LIMIT){var old=entries.entrySet().stream().filter(x->x.getValue().readers==0).min(Comparator.comparingLong(x->x.getValue().used)).orElse(null);if(old==null)return null;close(old.getValue());entries.remove(old.getKey());}e=new Entry(url);entries.put(url,e);return e;}
 public static synchronized CompletableFuture<AudioStream> open(String url){var e=entry(url);if(e==null)return CompletableFuture.failedFuture(new IOException("Twelve stations already active"));e.readers++;return e.future.<AudioStream>thenApply(h->h.reader(()->release(e))).whenComplete((r,error)->{if(error!=null)release(e);});}
 private static synchronized void release(Entry e){e.readers=Math.max(0,e.readers-1);e.used=System.currentTimeMillis();}
 public static synchronized void sweep(){long now=System.currentTimeMillis();var it=entries.values().iterator();while(it.hasNext()){var e=it.next();if(e.readers==0&&(now-e.used>30000||e.future.isCompletedExceptionally()&&now-e.created>10000)){close(e);it.remove();}}}
 private static void close(Entry e){e.canceled=true;e.future.thenAccept(Hub::close);}
 public static synchronized void clear(){entries.values().forEach(SharedRadioStreams::close);entries.clear();}
 static final class Hub {
  final RadioAudio source;final byte[] history;final int second,frame;long written;private volatile boolean closed;private final Thread pump;
  Hub(RadioAudio source){this.source=source;var f=source.getFormat();second=(int)f.getSampleRate()*f.getFrameSize();frame=f.getFrameSize();history=new byte[second*8];append(source.read(second*4));
   pump=Thread.ofPlatform().daemon().name("Station radio live clock").start(()->{long next=System.nanoTime();int chunk=second/20/frame*frame;try{while(!closed){next+=50_000_000L;long remaining=next-System.nanoTime();if(remaining>0)TimeUnit.NANOSECONDS.sleep(remaining);else if(remaining<-1_000_000_000L)next=System.nanoTime();append(source.read(chunk));if(source.ended()&&source.bufferedBytes()==0)break;}}catch(InterruptedException ignored){Thread.currentThread().interrupt();}});
  }
  private synchronized void append(ByteBuffer data){while(data.hasRemaining()){history[(int)(written%history.length)]=data.get();written++;}}
  void close(){closed=true;pump.interrupt();source.close();}
  synchronized long liveBytes(){return written;}
  synchronized AudioStream reader(Runnable release){return new AudioStream(){long cursor=Math.max(0,written-second*4L);boolean closedReader;public AudioFormat getFormat(){return source.getFormat();}
   public ByteBuffer read(int size)throws IOException{synchronized(Hub.this){if(closedReader)return ByteBuffer.allocateDirect(0);int want=Math.min(size,history.length)/frame*frame;
    boolean sync=com.sanctuarygames.createtransit.client.StationClientSettings.get().autoRadioSync;
    if(cursor<written-history.length||sync&&cursor<written-second*6L)cursor=Math.max(0,written-second*4L);
    int count=(int)Math.min(want,Math.max(0,written-cursor));
    // A live source that hasn't advanced yet must not be mistaken for EOF.
    if(count==0&&(!source.ended()||source.bufferedBytes()>0)&&!Hub.this.closed)return ByteBuffer.allocateDirect(Math.min(want,second/20/frame*frame)).order(ByteOrder.LITTLE_ENDIAN);
    var out=ByteBuffer.allocateDirect(count).order(ByteOrder.LITTLE_ENDIAN);for(int i=0;i<count;i++)out.put(history[(int)(cursor++%history.length)]);return out.flip();}}
   public void close(){synchronized(Hub.this){if(closedReader)return;closedReader=true;}release.run();}
  };}
 }
}
