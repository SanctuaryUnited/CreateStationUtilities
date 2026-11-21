package com.sanctuarygames.createtransit.elevator;

import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.elevator.model.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Uses the player's ordinary remappable Sneak/Attack/Use actions, without a custom key or tool. */
@EventBusSubscriber(modid=CreateTransit.MOD_ID)
public final class ElevatorLinkingEvents {
 private static boolean gesture(Player p){return p.isShiftKeyDown()&&p.getMainHandItem().isEmpty()&&p.getOffhandItem().isEmpty();}
 private static boolean marker(BlockState s){return s.getBlock() instanceof ElevatorFloorMarkerRailBlock||s.getBlock() instanceof ElevatorFloorMarkerBlock;}
 private static boolean allowed(PlayerInteractEvent e){var p=e.getEntity();return p.mayBuild()&&!p.isSpectator()&&p.canInteractWithBlock(e.getPos(),1)&&e.getLevel().mayInteract(p,e.getPos());}

 @SubscribeEvent public static void breakAction(PlayerInteractEvent.LeftClickBlock event){
  if(event.isCanceled()||!gesture(event.getEntity()))return;var l=event.getLevel();var pos=event.getPos();var s=l.getBlockState(pos);
  if(!marker(s)&&!(s.getBlock() instanceof ElevatorDriveBlock)&&!(s.getBlock() instanceof ElevatorCallBlock))return;
  event.setCanceled(true); // Also cancel prediction and mining hold in creative/survival.
  if(l.isClientSide||event.getAction()!=PlayerInteractEvent.LeftClickBlock.Action.START)return;
  if(!allowed(event)){ElevatorFeedback.error(event.getEntity(),com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.you_cannot_edit_elevator_links_here"));return;}
  if(marker(s))ElevatorLinking.selectMarker(l,pos,event.getEntity());
  else if(s.getBlock() instanceof ElevatorDriveBlock)ElevatorLinking.drive(l,pos,event.getEntity());
  else if(l.getBlockEntity(pos) instanceof ElevatorPartEntity call){call.clearMarkers();call.bindMarker(null);event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.literal("[Elevator] Manual call links cleared; automatic landing detection restored."),false);}
 }
 @SubscribeEvent public static void placeAction(PlayerInteractEvent.RightClickBlock event){
  if(event.isCanceled()||event.getHand()!=InteractionHand.MAIN_HAND||!gesture(event.getEntity()))return;var l=event.getLevel();var pos=event.getPos();var s=l.getBlockState(pos);
  if(!(s.getBlock() instanceof ElevatorCallBlock)&&!(s.getBlock() instanceof ModelLandingBlock))return;
  event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
  if(l.isClientSide)return;
  if(!allowed(event)){ElevatorFeedback.error(event.getEntity(),com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.you_cannot_edit_elevator_links_here"));return;}
  if(s.getBlock() instanceof ElevatorCallBlock){var selected=ElevatorLinking.selectedMarker(event.getEntity(),l);if(selected!=null&&l.getBlockEntity(pos) instanceof ElevatorPartEntity call&&call.callMarkers().contains(selected))event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.literal("[Elevator] This floor is already linked to the call button."),false);else ElevatorLinking.button(l,pos,event.getEntity());}
  else{var base=ModelLandingBlock.base(pos,s);var state=l.getBlockState(base);if(!ModelLandingBlock.complete(l,base,s.getValue(ModelLandingBlock.SIZE),s.getValue(ElevatorPartBlock.FACING))){ElevatorFeedback.error(event.getEntity(),com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.complete_the_landing_door_frame_before"));return;}
   for(int x=0;x<s.getValue(ModelLandingBlock.SIZE);x++)for(int y=0;y<2;y++)if(!l.mayInteract(event.getEntity(),ModelLandingBlock.part(base,state.getValue(ElevatorPartBlock.FACING),x,y))){ElevatorFeedback.error(event.getEntity(),com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.you_cannot_edit_this_landing"));return;}
   if(l.getBlockEntity(base) instanceof ModelLandingEntity door)door.link(event.getEntity());
  }
 }
}
