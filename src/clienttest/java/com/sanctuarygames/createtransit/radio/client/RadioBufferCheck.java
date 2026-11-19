package com.sanctuarygames.createtransit.radio.client;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.*;

/** Exact Minecraft four-buffer startup contract, partial reads, wrapping and cancellation. */
public final class RadioBufferCheck {
    private static final int SECOND=88200;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static byte[] sequence(int n){byte[] result=new byte[n];for(int i=0;i<n;i++)result[i]=(byte)(1+i%127);return result;}
    private static byte[] take(ByteBuffer data){byte[] result=new byte[data.remaining()];data.get(result);return result;}
    public static void main(String[] args)throws Exception{run();}
    public static void run()throws Exception{
        var source=sequence(SECOND*6);var buffer=new RadioPcmBuffer(SECOND,2);buffer.write(ByteBuffer.wrap(source));buffer.awaitStartup(()->false,1000);
        for(int i=0;i<4;i++)check(Arrays.equals(take(buffer.read(SECOND)),Arrays.copyOfRange(source,i*SECOND,(i+1)*SECOND)),"Minecraft startup buffer "+i+" contains continuous real PCM");
        check(buffer.underruns()==0&&buffer.bufferedBytes()==SECOND*2,"Four one-second startup reads retain two seconds of reserve");
        var partial=new RadioPcmBuffer(SECOND,2);byte[] shortAudio=sequence(8192);partial.write(ByteBuffer.wrap(shortAudio));check(Arrays.equals(take(partial.read(SECOND)),shortAudio),"Partial decoder chunk must not be padded to one second");check(partial.underruns()==0,"Partial real data is not an underrun");
        var fallback=partial.read(SECOND);check(fallback.remaining()==SECOND/4&&partial.underruns()==1,"Silence is only a short fallback for an actually empty live buffer");partial.write(ByteBuffer.wrap(shortAudio));check(Arrays.equals(take(partial.read(SECOND)),shortAudio),"Fresh data resumes without injected tail silence");
        var wrapping=new RadioPcmBuffer(SECOND,2);byte[] nine=sequence(SECOND*9);var finished=new CountDownLatch(1);var producer=new Thread(()->{try{wrapping.write(ByteBuffer.wrap(nine));}catch(InterruptedException e){throw new AssertionError(e);}finally{finished.countDown();}},"Buffer bound test");producer.setDaemon(true);producer.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);while(wrapping.bufferedBytes()<wrapping.capacityBytes()&&System.nanoTime()<deadline)Thread.sleep(1);check(wrapping.bufferedBytes()==SECOND*8&&finished.getCount()==1,"Producer backpressure bounds memory to eight seconds");check(Arrays.equals(take(wrapping.read(SECOND)),Arrays.copyOfRange(nine,0,SECOND)),"Wrapped first block is exact");check(finished.await(2,TimeUnit.SECONDS),"Consumption releases producer backpressure");wrapping.finish(null);for(int i=1;i<9;i++)check(Arrays.equals(take(wrapping.read(SECOND)),Arrays.copyOfRange(nine,i*SECOND,(i+1)*SECOND)),"Ring wrap preserves sample ordering "+i);check(!wrapping.read(SECOND).hasRemaining(),"EOF drains without padded silence");
        var warming=new RadioPcmBuffer(SECOND,2);warming.write(ByteBuffer.wrap(sequence(SECOND*5)));var ready=new CompletableFuture<Void>();var waiter=new Thread(()->{try{warming.awaitStartup(()->false,2000);ready.complete(null);}catch(Exception e){ready.completeExceptionally(e);}});waiter.setDaemon(true);waiter.start();try{ready.get(50,TimeUnit.MILLISECONDS);throw new AssertionError("Five seconds must not start four-buffer playback prematurely");}catch(TimeoutException expected){}warming.write(ByteBuffer.wrap(sequence(SECOND)));ready.get(1,TimeUnit.SECONDS);
        var canceled=new RadioPcmBuffer(SECOND,2);var canceledResult=new CompletableFuture<Boolean>();var cancelWaiter=new Thread(()->{try{canceled.awaitStartup(()->false,2000);canceledResult.complete(false);}catch(java.io.IOException expected){canceledResult.complete(true);}});cancelWaiter.setDaemon(true);cancelWaiter.start();canceled.close();check(canceledResult.get(1,TimeUnit.SECONDS),"Close releases a startup waiter");
        var finite=new RadioPcmBuffer(SECOND,2);finite.write(ByteBuffer.wrap(shortAudio));finite.finish(null);finite.awaitStartup(()->false,1000);check(Arrays.equals(take(finite.read(SECOND)),shortAudio),"Short finite recordings start and end without waiting for six seconds");
        System.out.println("PASS: radio PCM: four exact one-second startup reads + two-second reserve; no partial-chunk silence padding; bounded eight-second ring; wrap/order; underrun recovery; EOF and startup cancellation.");
    }
}
