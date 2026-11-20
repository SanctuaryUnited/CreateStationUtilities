package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.elevator.ElevatorBanks;
import com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.IndicatorStyle;
import java.util.UUID;import net.minecraft.core.BlockPos;import net.minecraft.nbt.*;
import net.minecraft.gametest.framework.*;import net.neoforged.neoforge.gametest.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class BankPruneGameTests {
 private static void check(boolean b,String m){if(!b)throw new GameTestAssertException(m);}
 private static CompoundTag car(UUID id,BlockPos p){var t=new CompoundTag();t.putUUID("Id",id);t.putLong("Drive",p.asLong());return t;}
 private static CompoundTag bank(UUID id,ListTag cars,ListTag merged){var t=new CompoundTag();t.putUUID("Id",id);t.putInt("Y",64);t.putString("Side","north");var s=IndicatorStyle.values()[0].name();t.putString("Button",s);t.putString("Header",s);t.put("Cars",cars);t.put("Merged",merged);return t;}
 private static ElevatorBanks load(CompoundTag t)throws Exception{var m=ElevatorBanks.class.getDeclaredMethod("load",CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class);m.setAccessible(true);return (ElevatorBanks)m.invoke(null,t,null);}
 private static void prune(ElevatorBanks b,GameTestHelper h)throws Exception{var m=ElevatorBanks.class.getDeclaredMethod("prune",net.minecraft.server.level.ServerLevel.class);m.setAccessible(true);m.invoke(b,h.getLevel());}
 private static int cars(CompoundTag saved,int bank){return saved.getList("Banks",10).getCompound(bank).getList("Cars",10).size();}
 @GameTest(template="elevator2_shaft",batch="bankprune",timeoutTicks=1400)
 public static void prune_removes_only_definitely_gone_state(GameTestHelper h)throws Exception{
  var level=h.getLevel();var dead=h.absolutePos(new BlockPos(1,1,1));var far=new BlockPos(29_000_000,64,29_000_000);var panel=h.absolutePos(new BlockPos(2,1,2));
  check(level.hasChunkAt(dead)&&!level.hasChunkAt(far),"test positions: one loaded, one unloaded");
  UUID live=UUID.randomUUID(),gone=UUID.randomUUID(),gone2=UUID.randomUUID(),keepBank=UUID.randomUUID(),deadBank=UUID.randomUUID(),deletedBank=UUID.randomUUID();
  var keepCars=new ListTag();keepCars.add(car(live,far));keepCars.add(car(gone,dead));
  var childCars=new ListTag();childCars.add(car(gone,dead));var merged=new ListTag();merged.add(bank(UUID.randomUUID(),childCars,new ListTag()));
  var deadCars=new ListTag();deadCars.add(car(gone2,dead.east()));
  var banks=new ListTag();banks.add(bank(keepBank,keepCars,merged));banks.add(bank(deadBank,deadCars,new ListTag()));
  var overrides=new ListTag();var o=new CompoundTag();o.putLong("Pos",panel.asLong());o.putUUID("Bank",keepBank);overrides.add(o);
  var o2=new CompoundTag();o2.putLong("Pos",far.asLong());o2.putUUID("Bank",keepBank);overrides.add(o2);
  var pending=new ListTag();
  var j1=new CompoundTag();j1.putUUID("Bank",deletedBank);j1.putLong("Panel",panel.asLong());j1.putInt("Y",64);j1.putString("Side","north");j1.putInt("Direction",1);pending.add(j1);
  var j2=new CompoundTag();j2.putLong("Panel",panel.asLong());j2.putInt("Y",64);j2.putString("Side","north");j2.putInt("Direction",1);pending.add(j2);
  var data=new CompoundTag();data.put("Banks",banks);data.put("SelectorOverrides",overrides);data.put("Pending",pending);
  var b=load(data);
  check(b.save(new CompoundTag(),null).getList("Pending",10).size()==1,"loader already drops calls for unknown banks");
  prune(b,h);
  var s=b.save(new CompoundTag(),null);
  check(s.getList("Banks",10).size()==1,"bank with only dead cars removed");
  check(cars(s,0)==1&&s.getList("Banks",10).getCompound(0).getList("Cars",10).getCompound(0).getUUID("Id").equals(live),"dead car removed, out-of-range car kept");
  check(s.getList("Banks",10).getCompound(0).getList("Merged",10).getCompound(0).getList("Cars",10).isEmpty(),"dead car removed from merged child too");
  var keptOverrides=s.getList("SelectorOverrides",10);
  check(keptOverrides.size()==1&&BlockPos.of(keptOverrides.getCompound(0).getLong("Pos")).equals(far),"override at missing loaded panel removed; unloaded kept");
  check(s.getList("Pending",10).size()==1,"unservable call waits out its grace period");
  h.runAtTickTime(1300,()->{try{prune(b,h);check(b.save(new CompoundTag(),null).getList("Pending",10).isEmpty(),"unservable call expired after 60 s");h.succeed();}catch(Exception e){throw new GameTestAssertException(e.toString());}});
 }
}
