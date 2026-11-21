package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.radio.RadioStations;
import java.net.InetAddress;import java.util.List;
import net.minecraft.gametest.framework.*;import net.neoforged.neoforge.gametest.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class RadioSecurityGameTests {
 private static void check(boolean b,String m){if(!b)throw new GameTestAssertException(m);}
 private static boolean literal(String host)throws Exception{var m=RadioStations.class.getDeclaredMethod("privateLiteral",String.class);m.setAccessible(true);return (boolean)m.invoke(null,host);}
 @GameTest(template="elevator2_shaft",batch="radiosec")
 public static void radio_address_rules(GameTestHelper h)throws Exception{
  for(var a:List.of("127.0.0.1","10.1.2.3","172.16.0.1","192.168.1.1","169.254.169.254","100.64.0.1","0.0.0.0","224.0.0.1","::1","fd00::1","fe80::1","::ffff:192.168.0.1"))check(RadioStations.privateAddress(InetAddress.getByName(a)),"private "+a);
  for(var a:List.of("8.8.8.8","1.1.1.1","2606:4700::1111"))check(!RadioStations.privateAddress(InetAddress.getByName(a)),"public "+a);
  for(var host:List.of("localhost","x.localhost","127.0.0.1","192.168.0.5","[::1]","[fd12::1]"))check(literal(host),"literal "+host);
  for(var host:List.of("radio.sanctuaryunited.net","8.8.8.8","[2606:4700::1111]"))check(!literal(host),"not literal "+host);
  check(RadioStations.host("https://radio.sanctuaryunited.net/public/sanctuary_radio").equals("radio.sanctuaryunited.net"),"host parse");
  check(TransitServerConfig.hostAllowed("radio.sanctuaryunited.net"),"empty allow-list allows any host");
  check(TransitServerConfig.STATION_EDITORS.get()==TransitServerConfig.StationEditors.OPERATORS,"default editors");
  var data=new RadioStations();
  check(data.servable(new RadioStations.Station("a","S","https://radio.sanctuaryunited.net/public/sanctuary_radio")),"real station servable");
  check(!data.servable(new RadioStations.Station("b","L","http://192.168.1.10:8000/live")),"LAN station filtered");
  var fake=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
  try{data.add(fake,"S","https://radio.sanctuaryunited.net/public/sanctuary_radio");throw new GameTestAssertException("non-op added station");}catch(IllegalArgumentException expected){check(expected.getMessage().contains("operators"),expected.getMessage());}
  h.succeed();
 }
}
