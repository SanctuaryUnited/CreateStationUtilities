package com.sanctuarygames.createtransit.elevator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
/** Elevator chat notices (controller warnings and refusals). Styling and duplicate suppression live in TransitText. */
public final class ElevatorFeedback {
 private ElevatorFeedback(){}
 public static void error(Player player,String message){if(message==null||message.isBlank())return;com.sanctuarygames.createtransit.TransitText.tell(player,com.sanctuarygames.createtransit.TransitText.Tone.WARN,"elevator",com.sanctuarygames.createtransit.TransitText.text(message));}
}
