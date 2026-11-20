package com.sanctuarygames.createtransit.radio.client;
import java.nio.*;import javax.sound.sampled.AudioFormat;import net.minecraft.client.sounds.AudioStream;
public final class RadioStreamCheck {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a)throws Exception{
  var fmt=new AudioFormat(8000,16,1,true,false);int second=16000;
  var decoder=new AudioStream(){long n;public AudioFormat getFormat(){return fmt;}
   public ByteBuffer read(int size){var b=ByteBuffer.allocate(size/2*2);while(b.hasRemaining())b.put((byte)(n++%251));return b.flip();}public void close(){}};
  var ctor=RadioAudio.class.getDeclaredConstructor(AudioStream.class,java.util.function.Consumer.class);ctor.setAccessible(true);
  var audio=(RadioAudio)ctor.newInstance(decoder,(java.util.function.Consumer<String>)s->{});
  long deadline=System.currentTimeMillis()+3000;while(audio.bufferedBytes()<second*6&&System.currentTimeMillis()<deadline)Thread.sleep(10);
  var internal=audio.read(1000);check(!internal.isDirect(),"RadioAudio.read is heap");check(internal.remaining()==1000,"internal read size");
  for(int i=0;i<1000;i++)check(internal.get(i)==(byte)(i%251),"internal order "+i);
  var hub=new SharedRadioStreams.Hub(audio);
  Thread.sleep(5500); // pump runs past the 8 s ring, so appends and reads cross the wrap point
  check(hub.liveBytes()>second*8L,"history wrapped: "+hub.liveBytes());
  var reader=hub.reader(()->{});int prev=-1;long total=0;
  long wraps=0,startCursor=-1;for(int r=0;r<55;r++){Thread.sleep(200);var out=reader.read(4001);check(out.isDirect(),"reader output must stay direct for OpenAL");
   check(out.remaining()%2==0,"whole frames");
   for(int i=out.position();i<out.limit();i++){int v=out.get(i)&255;if(prev>=0)check(v==(prev+1)%251,"sequence broken at read "+r+" byte "+i+": "+prev+"->"+v);prev=v;}total+=out.remaining();}
  check(total>second*8L,"reader advanced past one full ring: "+total);
  check(total>0,"reader produced audio");
  reader.close();hub.close();
  System.out.println("PASS: heap internal PCM, direct OpenAL buffers, ring wrap preserves order ("+total+" bytes checked)");
 }
}
