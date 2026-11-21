package com.sanctuarygames.createtransit.radio.client;
import java.io.*;import java.net.*;import java.nio.*;import java.util.concurrent.*;import javax.sound.sampled.AudioFormat;import net.minecraft.client.sounds.*;import javazoom.jl.decoder.*;
/** Bounded network/decode buffers. The Minecraft audio thread never waits for a network read.
 * read() returns heap buffers: only SharedRadioStreams readers hand PCM to OpenAL, and those are direct. */
public final class RadioAudio implements AudioStream {
 private static final ExecutorService CLEANUP=Executors.newFixedThreadPool(4,r->{var t=new Thread(r,"Transit radio cleanup");t.setDaemon(true);return t;});
 private final AudioStream decoder;private final RadioPcmBuffer buffer;private final Thread producer;
 private final java.util.concurrent.atomic.AtomicBoolean decoderClosed=new java.util.concurrent.atomic.AtomicBoolean();
 private volatile boolean closed;private final java.util.function.Consumer<String> status;private long lastUnderruns;
 public static RadioAudio open(String url,java.util.function.Consumer<String> status)throws IOException{return open(url,status,()->false);}
 public static RadioAudio open(String url,java.util.function.Consumer<String> status,java.util.function.BooleanSupplier canceled)throws IOException{
  InputStream input=null;RadioAudio result=null;
  try{
   for(int redirects=0;redirects<6;redirects++){
    if(canceled.getAsBoolean())throw new InterruptedIOException("Radio playback canceled");
    var uri=URI.create(url);com.sanctuarygames.createtransit.radio.RadioStations.validate("Stream",url);requirePublicHost(uri.getHost());
    var connection=(HttpURLConnection)uri.toURL().openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(12000);connection.setInstanceFollowRedirects(false);
    connection.setRequestProperty("User-Agent","CreateStationUtilities/0.2");connection.setRequestProperty("Icy-MetaData","0");connection.setRequestProperty("Accept-Encoding","identity");
    int code=connection.getResponseCode();
    if(code>=300&&code<400){String next=connection.getHeaderField("Location");connection.disconnect();if(next==null)throw new IOException("Redirect has no location");url=uri.resolve(next).toString();continue;}
    if(code!=200){connection.disconnect();throw new IOException("HTTP "+code);}
    input=connection.getInputStream();String interval=connection.getHeaderField("icy-metaint");
    if(interval!=null){int n;try{n=Integer.parseInt(interval.trim());}catch(NumberFormatException ex){throw new IOException("Invalid ICY metadata interval",ex);}if(n<=0||n>16*1024*1024)throw new IOException("Invalid ICY metadata interval");input=new IcyMetadataInput(input,n);}
    input=new BufferedInputStream(input,32768);break;
   }
   if(input==null)throw new IOException("Too many stream redirects");
   var push=new PushbackInputStream(input,4);byte[] magic=push.readNBytes(4);push.unread(magic);
   AudioStream decoder=magic.length==4&&magic[0]=='O'&&magic[1]=='g'&&magic[2]=='g'&&magic[3]=='S'?new MonoOgg(new JOrbisAudioStream(push)):new Mp3(push);
   result=new RadioAudio(decoder,status);result.buffer.awaitStartup(canceled,20000);status.accept("Playing");return result;
  }catch(Exception ex){if(result!=null)result.close();else if(input!=null)try{input.close();}catch(IOException ignored){}throw ex instanceof IOException io?io:new IOException("Unsupported stream: use MP3 or Ogg Vorbis",ex);}
 }
 /** Test fixtures only: lets the radio audio check stream from its own server on 127.0.0.1. Nothing in the game sets it. */
 static volatile boolean allowLoopbackForTests;
 /** Server-supplied URLs (and every redirect) must not reach this computer or its local network. */
 static void requirePublicHost(String host)throws IOException{
  if(host==null||host.equalsIgnoreCase("localhost")||host.toLowerCase(java.util.Locale.ROOT).endsWith(".localhost"))throw new IOException("Radio stream refused: local address");
  for(var address:InetAddress.getAllByName(host)){
   if(allowLoopbackForTests&&address.isLoopbackAddress())continue;
   if(com.sanctuarygames.createtransit.radio.RadioStations.privateAddress(address))throw new IOException("Radio stream refused: private or local network address");
  }
 }
 private RadioAudio(AudioStream decoder,java.util.function.Consumer<String> status){
  this.decoder=decoder;this.status=status;var f=decoder.getFormat();buffer=new RadioPcmBuffer((int)f.getSampleRate()*f.getFrameSize(),f.getFrameSize());status.accept("Buffering…");
  producer=Thread.ofPlatform().daemon().name("Transit radio PCM").start(()->{
   IOException failure=null;
   try{while(!closed){var data=decoder.read(8192);if(!data.hasRemaining())break;buffer.write(data);}}
   catch(InterruptedException ignored){Thread.currentThread().interrupt();}
   catch(Exception ex){if(!closed){failure=ex instanceof IOException io?io:new IOException("Radio decoder failed",ex);status.accept("Connection lost — finishing buffered audio");}}
   finally{buffer.finish(failure);closeDecoder();}
  });
 }
 @Override public AudioFormat getFormat(){return decoder.getFormat();}
 @Override public ByteBuffer read(int bytes){
  var result=buffer.read(bytes);long underruns=buffer.underruns();
  if(underruns!=lastUnderruns){lastUnderruns=underruns;status.accept("Buffering…");}
  else if(result.hasRemaining()&&!closed)status.accept("Playing");
  return result;
 }
 @Override public void close(){if(closed)return;closed=true;buffer.close();producer.interrupt();CLEANUP.execute(this::closeDecoder);}
 private void closeDecoder(){if(decoderClosed.compareAndSet(false,true))try{decoder.close();}catch(IOException ignored){}}
 long underruns(){return buffer.underruns();}
 int bufferedBytes(){return buffer.bufferedBytes();}
 int capacityBytes(){return buffer.capacityBytes();}
 boolean ended(){return buffer.ended();}
 /** Remove ICY song-title blocks even if a server ignores Icy-MetaData: 0. */
 static final class IcyMetadataInput extends FilterInputStream {
  private final int interval;private int remaining;
  IcyMetadataInput(InputStream in,int interval){super(in);this.interval=interval;remaining=interval;}
  private boolean audio()throws IOException{
   if(remaining>0)return true;int length=in.read();if(length<0)return false;int bytes=length*16;
   while(bytes>0){long skipped=in.skip(bytes);if(skipped<=0){if(in.read()<0)throw new EOFException("Truncated ICY metadata");skipped=1;}bytes-=skipped;}
   remaining=interval;return true;
  }
  @Override public int read()throws IOException{if(!audio())return -1;int value=in.read();if(value>=0)remaining--;return value;}
  @Override public int read(byte[] bytes,int off,int len)throws IOException{java.util.Objects.checkFromIndexSize(off,len,bytes.length);if(len==0)return 0;if(!audio())return -1;int count=in.read(bytes,off,Math.min(len,remaining));if(count>0)remaining-=count;return count;}
  @Override public long skip(long n)throws IOException{long total=0;byte[] discard=new byte[(int)Math.min(4096,Math.max(0,n))];while(total<n){int count=read(discard,0,(int)Math.min(discard.length,n-total));if(count<0)break;total+=count;}return total;}
 }
 private static final class Mp3 implements AudioStream {
  private final InputStream input;private final Bitstream bits;private final Decoder decoder=new Decoder();private AudioFormat format;private short[] pending=new short[0];private int index,channels;private boolean eof;
  Mp3(InputStream input)throws IOException{this.input=input;bits=new Bitstream(input);frame();if(format==null)throw new IOException("No MP3 audio frames (AAC/HLS/playlists are not supported)");}
  private void frame()throws IOException{try{var h=bits.readFrame();if(h==null){eof=true;return;}var sample=(SampleBuffer)decoder.decodeFrame(h,bits);channels=sample.getChannelCount();if(format==null)format=new AudioFormat(sample.getSampleFrequency(),16,1,true,false);else if(sample.getSampleFrequency()!=(int)format.getSampleRate())throw new IOException("Stream sample rate changed");pending=java.util.Arrays.copyOf(sample.getBuffer(),sample.getBufferLength());index=0;bits.closeFrame();}catch(JavaLayerException e){throw new IOException("MP3 decode failed",e);}}
  public AudioFormat getFormat(){return format;}
  public ByteBuffer read(int size)throws IOException{var b=ByteBuffer.allocate(size/2*2).order(ByteOrder.LITTLE_ENDIAN);while(b.remaining()>=2){if(index>=pending.length){if(eof)break;frame();if(eof)break;}int value=pending[index++];if(channels==2&&index<pending.length)value=(value+pending[index++])/2;b.putShort((short)value);}return b.flip();}
  public void close()throws IOException{input.close();try{bits.close();}catch(BitstreamException ignored){}}
 }
 private static final class MonoOgg implements AudioStream {
  private final JOrbisAudioStream ogg;private final AudioFormat format;
  MonoOgg(JOrbisAudioStream stream){ogg=stream;var f=ogg.getFormat();format=new AudioFormat(f.getSampleRate(),16,1,true,false);}
  public AudioFormat getFormat(){return format;}
  public ByteBuffer read(int bytes)throws IOException{int channels=ogg.getFormat().getChannels();var raw=ogg.read(bytes*channels).order(ByteOrder.LITTLE_ENDIAN);if(channels==1)return raw;var out=ByteBuffer.allocate(raw.remaining()/channels).order(ByteOrder.LITTLE_ENDIAN);while(raw.remaining()>=channels*2){int sum=0;for(int i=0;i<channels;i++)sum+=raw.getShort();out.putShort((short)(sum/channels));}return out.flip();}
  public void close()throws IOException{ogg.close();}
 }
}
