package com.sanctuarygames.createtransit.elevator;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
/** Persistent chat, with duplicate suppression across GUI and automatic fault notices. */
public final class ElevatorFeedback {
 private record Notice(String text,long tick){}
 private static final Map<Player,Notice> LAST=new WeakHashMap<>();
 public static void error(Player player,String message){if(message==null||message.isBlank())return;long tick=player.level().getGameTime();var old=LAST.get(player);if(old!=null&&old.text().equals(message)&&tick-old.tick()<100)return;LAST.put(player,new Notice(message,tick));player.displayClientMessage(Component.literal("[Elevator] "+message),false);}
}
