package com.sanctuarygames.createtransit.elevator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A dedicated registered rail, not a conversion mode on the ordinary rail. */
public final class ElevatorFloorMarkerRailBlock extends ElevatorGuideRailBlock {
    public ElevatorFloorMarkerRailBlock(Properties properties) { super(properties, Kind.RAIL_MARKER); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (ElevatorLinking.wrench(stack)) {
            if (!level.isClientSide) {
                var d = drive(level, pos);
                if (d != null) { d.rescan(); ElevatorLinking.marker(level, pos, player); }
                else player.displayClientMessage(com.sanctuarygames.createtransit.TransitText.tr("message.createtransit.elevator.marker_rail_is_not_connected_to_2"),false);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            var d = drive(level, pos);
            if (d != null) { d.rescan(); d.openPanel(player, true, pos); }
            else player.displayClientMessage(com.sanctuarygames.createtransit.TransitText.tr("message.createtransit.elevator.no_connected_drive_below_this_marker"),false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
