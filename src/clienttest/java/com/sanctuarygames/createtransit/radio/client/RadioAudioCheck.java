package com.sanctuarygames.createtransit.radio.client;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real HTTP/codec regression, including Minecraft-sized reads and ICY title blocks. */
public final class RadioAudioCheck {
    private static final int SECOND=88200;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static byte[] bytes(ByteBuffer data){byte[] b=new byte[data.remaining()];data.get(b);return b;}
    private static byte[] fixture(String name)throws IOException{
        try(var in=RadioAudioCheck.class.getResourceAsStream("/radio/"+name)){
            if(in==null)throw new IOException("Missing fixture "+name);return in.readAllBytes();
        }
    }
    private static byte[] icy(byte[] source,int interval)throws IOException{
        var out=new ByteArrayOutputStream();for(int p=0;p<source.length;){int n=Math.min(interval,source.length-p);out.write(source,p,n);p+=n;if(n==interval){byte[] metadata=new byte[32];byte[] title="StreamTitle='Buffer test';".getBytes(java.nio.charset.StandardCharsets.US_ASCII);System.arraycopy(title,0,metadata,0,title.length);out.write(2);out.write(metadata);}}return out.toByteArray();
    }
    private static byte[] decode(String url,boolean initialBurst)throws Exception{
        try(var stream=RadioAudio.open(url,s->{})){
            check(stream.getFormat().getChannels()==1&&stream.getFormat().getSampleSizeInBits()==16&&stream.getFormat().getSampleRate()==44100,"Spatial mono format");
            var output=new ByteArrayOutputStream();
            if(initialBurst)for(int i=0;i<4;i++){long start=System.nanoTime();var part=bytes(stream.read(SECOND));check(part.length==SECOND,"Full Minecraft startup second "+i);check(System.nanoTime()-start<TimeUnit.MILLISECONDS.toNanos(100),"Audio read does not wait for decoder/network");output.write(part);}
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(!stream.ended()||stream.bufferedBytes()>0){
                if(stream.bufferedBytes()<SECOND&&!stream.ended()){check(System.nanoTime()<deadline,"Fixture decoder stalled");Thread.sleep(1);continue;}
                output.write(bytes(stream.read(SECOND)));
            }
            check(stream.underruns()==0,"No synthetic silence in finite stream");return output.toByteArray();
        }
    }
    private static void AudioStreamGainCheck()throws Exception{var original=ByteBuffer.allocateDirect(8).order(java.nio.ByteOrder.LITTLE_ENDIAN);for(short value:new short[]{1000,-1000,20000,-20000})original.putShort(value);original.flip();var base=new net.minecraft.client.sounds.AudioStream(){public javax.sound.sampled.AudioFormat getFormat(){return new javax.sound.sampled.AudioFormat(44100,16,1,true,false);}public ByteBuffer read(int size){return original.duplicate().order(java.nio.ByteOrder.LITTLE_ENDIAN);}public void close(){}};try(var amplified=new RadioGainStream(base,()->3)){var samples=amplified.read(8).order(java.nio.ByteOrder.LITTLE_ENDIAN);check(samples.getShort()==3000&&samples.getShort()==-3000&&samples.getShort()==32767&&samples.getShort()==-32768,"Actual PCM amplification and clipping bounds");}System.out.println("PASS: 300 percent saved gain, amplified PCM and bounded saturation.");}
    public static void run()throws Exception{
        RadioBufferCheck.run();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var pool=Executors.newCachedThreadPool(r->{var t=new Thread(r,"Radio fixture HTTP");t.setDaemon(true);return t;});server.setExecutor(pool);
        byte[] mp3=fixture("long-tone.mp3");var stalledRelease=new CountDownLatch(1);
        try{
            for(String extension:new String[]{"mp3","ogg"})for(boolean metadata:new boolean[]{false,true}){
                byte[] encoded=fixture("long-tone."+extension),body=metadata?icy(encoded,97):encoded;String path="/"+extension+(metadata?"-icy":"");
                server.createContext(path,exchange->{exchange.getResponseHeaders().set("Content-Type",extension.equals("mp3")?"audio/mpeg":"audio/ogg");if(metadata)exchange.getResponseHeaders().set("icy-metaint","97");exchange.sendResponseHeaders(200,body.length);try(var out=exchange.getResponseBody()){out.write(body);}});
            }
            server.createContext("/jitter",exchange->{exchange.getResponseHeaders().set("Content-Type","audio/mpeg");exchange.sendResponseHeaders(200,mp3.length);try(var out=exchange.getResponseBody()){for(int p=0,chunk=0;p<mp3.length;p+=2048,chunk++){out.write(mp3,p,Math.min(2048,mp3.length-p));out.flush();Thread.sleep(80);if(chunk%7==6)Thread.sleep(200);}}catch(InterruptedException ex){Thread.currentThread().interrupt();}catch(IOException ignored){}});
            server.createContext("/stalled",exchange->{exchange.sendResponseHeaders(200,0);try(var out=exchange.getResponseBody()){out.write(mp3,0,2048);out.flush();stalledRelease.await(5,TimeUnit.SECONDS);}catch(InterruptedException ex){Thread.currentThread().interrupt();}catch(IOException ignored){}});
            server.start();String base="http://127.0.0.1:"+server.getAddress().getPort();
            byte[] reference=null;
            for(String extension:new String[]{"mp3","ogg"}){
                byte[] plain=decode(base+"/"+extension,true),metadata=decode(base+"/"+extension+"-icy",true);
                check(plain.length>=SECOND*12,"Twelve seconds of real decoded "+extension+" audio");check(Arrays.equals(plain,metadata),"ICY stripping preserves every "+extension+" PCM sample");if(extension.equals("mp3"))reference=plain;
            }
            try(var stream=RadioAudio.open(base+"/jitter",s->{})){
                int offset=0;for(int i=0;i<9;i++){
                    if(i>=4)Thread.sleep(1000);long start=System.nanoTime();byte[] part=bytes(stream.read(SECOND));
                    check(part.length==SECOND,"Jitter route fills Minecraft buffer "+i);check(System.nanoTime()-start<TimeUnit.MILLISECONDS.toNanos(100),"Jitter does not block audio thread");
                    check(Arrays.equals(part,Arrays.copyOfRange(reference,offset,offset+SECOND)),"No inserted silence, dropped or reordered samples at jitter buffer "+i);offset+=SECOND;
                }
                check(stream.underruns()==0,"Four startup + five paced refills survive network bursts without underruns");
            }
            var connections=new java.util.concurrent.atomic.AtomicInteger();
            server.createContext("/shared",exchange->{connections.incrementAndGet();exchange.sendResponseHeaders(200,mp3.length);try(var out=exchange.getResponseBody()){out.write(mp3);}});
            String shared=base+"/shared";SharedRadioStreams.warm(shared);
            var readers=new java.util.ArrayList<net.minecraft.client.sounds.AudioStream>();
            try{for(int i=0;i<12;i++)readers.add(SharedRadioStreams.open(shared).get(5,TimeUnit.SECONDS));
                for(int second=0;second<4;second++)for(var reader:readers)check(Arrays.equals(bytes(reader.read(SECOND)),Arrays.copyOfRange(reference,second*SECOND,(second+1)*SECOND)),"Twelve independent readers preserve station PCM");
                check(connections.get()==1,"Twelve speakers use one HTTP decoder connection");
            }finally{for(var reader:readers)reader.close();}
            long warmStart=System.nanoTime();try(var again=SharedRadioStreams.open(shared).get(1,TimeUnit.SECONDS)){check(again.read(SECOND).remaining()==SECOND,"Warm station immediately serves PCM");}
            check(connections.get()==1&&System.nanoTime()-warmStart<TimeUnit.SECONDS.toNanos(1),"Re-entry reuses warm connection without six-second startup");SharedRadioStreams.clear();
            System.out.println("PASS: twelve shared station readers, exact PCM, one HTTP connection, warm re-entry without reconnect.");
            var freshConnections=new java.util.concurrent.atomic.AtomicInteger();server.createContext("/finite-reconnect",exchange->{freshConnections.incrementAndGet();exchange.sendResponseHeaders(200,mp3.length);try(var out=exchange.getResponseBody()){out.write(mp3);}});String endedUrl=base+"/finite-reconnect";
            try(var finite=SharedRadioStreams.open(endedUrl).get(5,TimeUnit.SECONDS)){boolean ended=false;for(int i=0;i<400;i++){if(!finite.read(SECOND).hasRemaining()){ended=true;break;}Thread.sleep(50);}check(ended,"Finite fixture actually reaches EOF");}
            try(var recovered=SharedRadioStreams.open(endedUrl).get(5,TimeUnit.SECONDS)){check(recovered.read(SECOND).hasRemaining()&&freshConnections.get()==2,"Drained cached source reconnects with fresh PCM");}SharedRadioStreams.clear();System.out.println("PASS: exhausted shared stream reconnects instead of reusing a dead decoder.");
            var live=new SharedRadioStreams.Hub(RadioAudio.open(base+"/mp3",s->{}));try{long start=live.liveBytes();Thread.sleep(1300);check(live.liveBytes()>=start+SECOND,"Live clock advances with no listeners; leaving range does not pause radio");try(var late=live.reader(()->{})){check(late.read(SECOND).remaining()==SECOND,"Returning reader joins current bounded history");}}finally{live.close();}
            System.out.println("PASS: live radio timeline advances without readers; late reader joins current history.");
            var settings=new com.sanctuarygames.createtransit.radio.RadioSettings();settings.configure("volume","3");var restored=new com.sanctuarygames.createtransit.radio.RadioSettings();restored.load(settings.save());check(restored.volume==3,"300 percent persists");
            AudioStreamGainCheck();
            var canceled=new AtomicBoolean();var warming=new CountDownLatch(1);
            CompletableFuture<Boolean> stopped=CompletableFuture.supplyAsync(()->{try(var stream=RadioAudio.open(base+"/stalled",s->{if(s.startsWith("Buffering"))warming.countDown();},canceled::get)){return false;}catch(IOException expected){return canceled.get();}},pool);
            check(warming.await(2,TimeUnit.SECONDS),"Stalled fixture reaches asynchronous prebuffer");canceled.set(true);check(stopped.get(1,TimeUnit.SECONDS),"Cancel startup without waiting for network timeout");stalledRelease.countDown();
            System.out.println("PASS: real HTTP MP3/Ogg, four immediate one-second startup buffers, full PCM continuity across decoder chunks, byte-identical ICY metadata stripping, bursty-network paced playback with zero underruns, nonblocking reads and canceled warmup. No public-station listening test.");
        }finally{stalledRelease.countDown();server.stop(0);pool.shutdownNow();}
    }
}
