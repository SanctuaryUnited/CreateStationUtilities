package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.radio.*;
import net.minecraft.gametest.framework.*;import net.neoforged.neoforge.gametest.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class RadioGroupGameTests {
 private static void check(boolean b,String m){if(!b)throw new GameTestAssertException(m);}
 @GameTest(template="elevator2_shaft",batch="radiogroup")
 public static void full_group_list_never_throws_on_read(GameTestHelper h){
  var data=new RadioStations();var seed=new RadioSettings();seed.enabled=true;seed.station="s";seed.volume=.8;
  for(int i=0;i<64;i++)data.group("g"+i,seed);
  check(data.snapshot().getList("Groups",10).size()==64,"64 groups");
  var local=new RadioSettings();local.enabled=true;local.station="mine";local.volume=.5;
  var e=data.effective("unknown",local);
  check(e.enabled&&e.station.equals("mine")&&Math.abs(e.volume-.5)<1e-9,"falls back to local settings");
  check(data.snapshot().getList("Groups",10).size()==64,"read did not create a group");
  check(Math.abs(data.peek("unknown",local).volume-1)<1e-9,"preview seeded like group()");
  var shared=data.group("g0",seed);shared.station="shared";
  var f=data.effective("g0",local);check(f.station.equals("shared")&&Math.abs(f.volume-.5*shared.volume)<1e-9,"existing group still applies");
  try{data.group("g64",seed);throw new GameTestAssertException("explicit create should still cap at 64");}catch(IllegalArgumentException expected){}
  h.succeed();
 }
}
