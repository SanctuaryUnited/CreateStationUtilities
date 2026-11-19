package com.sanctuarygames.createtransit.lrt;

import com.simibubi.create.content.trains.entity.Train;

/** A carriage roof horn is recognized by Create's own continuous whistle audio. */
public final class LrtHorn {
 public static boolean has(Train train){
  for(var carriage:train.carriages){var entity=carriage.anyAvailableEntity();
   if(entity!=null&&entity.getContraption()!=null&&entity.getContraption().getBlocks().values().stream().anyMatch(info->info.state().is(LrtRegistry.HORN.get())))return true;
  }
  return false;
 }
 private LrtHorn(){}
}
