package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.block.TransitBlockItem;
import com.sanctuarygames.createtransit.escalator.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

/** Real player interactions, rather than direct calls to setColors. */
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class EscalatorDyeGameTests {
    private static void click(GameTestHelper h, FakePlayer player, BlockPos p, InteractionHand hand, boolean crouch) {
        player.setPos(Vec3.atCenterOf(p).add(0,0,2)); player.setShiftKeyDown(crouch);
        var result = player.gameMode.useItemOn(player,h.getLevel(),player.getItemInHand(hand),hand,
            new BlockHitResult(Vec3.atCenterOf(p),Direction.SOUTH,p,false));
        h.assertTrue(result.consumesAction(),"Dye click reaches the block handler, crouch="+crouch+" hand="+hand);
        player.setShiftKeyDown(false);
    }

    @GameTest(template="empty", timeoutTicks=40)
    public static void actual_frame_and_crouched_led_clicks_all_parts_all_colors(GameTestHelper h) {
        var level=h.getLevel(); var p=h.absolutePos(new BlockPos(2,1,2));
        var player=TransitTestPlayers.makeMockPlayer(h,GameType.SURVIVAL);
        for (var entry:EscalatorRegistry.allBlocks()) {
            var block=entry; var state=block.defaultBlockState(); level.setBlock(p,state,Block.UPDATE_ALL);
            var be=(EscalatorEntity)level.getBlockEntity(p);
            for (var color:DyeColor.values()) {
                int accent=(color.getId()+1)%16; be.setColors((color.getId()+1)%16,accent);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(DyeItem.byColor(color),2));
                click(h,player,p,InteractionHand.MAIN_HAND,false);
                h.assertTrue(be.paint()==color.getId()&&be.accent()==accent,"Frame click changes only paint: "+entry);
                h.assertTrue(player.getMainHandItem().getCount()==1,"Survival frame click consumes one dye");
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(DyeItem.byColor(color),2));
                click(h,player,p,InteractionHand.MAIN_HAND,true);
                h.assertTrue(be.paint()==color.getId()&&be.accent()==color.getId(),"Crouch click changes LEDs: "+entry);
                h.assertTrue(player.getMainHandItem().getCount()==1,"Survival LED click consumes one dye");
                click(h,player,p,InteractionHand.MAIN_HAND,true);
                h.assertTrue(player.getMainHandItem().getCount()==1,"Applying the same color consumes no extra dye");
                var picked=block.getCloneItemStack(state,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false),level,p,player);
                h.assertTrue(TransitBlockItem.paint(picked)==color&&TransitBlockItem.accent(picked)==color,"Pick block preserves both channels");
            }
            level.removeBlock(p,false);
        }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=40)
    public static void offhand_led_dye_and_creative_dye_keep_the_other_channel(GameTestHelper h) {
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(2,1,2));
        var state=EscalatorRegistry.part(EscalatorBlock.Kind.INCLINE,false).defaultBlockState();
        level.setBlock(p,state,Block.UPDATE_ALL);var be=(EscalatorEntity)level.getBlockEntity(p);be.setColors(14,0);
        var player=TransitTestPlayers.makeMockPlayer(h,GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.CYAN_DYE,2));
        click(h,player,p,InteractionHand.OFF_HAND,true);
        h.assertTrue(be.paint()==14&&be.accent()==DyeColor.CYAN.getId(),"Offhand crouch dye preserves frame color");
        h.assertTrue(player.getOffhandItem().getCount()==1,"Offhand dye consumes one");
        player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BLUE_DYE,2));
        click(h,player,p,InteractionHand.MAIN_HAND,false);
        h.assertTrue(be.paint()==DyeColor.BLUE.getId()&&be.accent()==DyeColor.CYAN.getId(),"Frame dye preserves LEDs");
        h.assertTrue(player.getMainHandItem().getCount()==2,"Creative dye is not consumed");h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=40)
    public static void network_color_packet_and_reload_preserve_both_dye_channels(GameTestHelper h) {
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(2,1,2));
        var state=EscalatorRegistry.part(EscalatorBlock.Kind.INCLINE,true).defaultBlockState();
        level.setBlock(p,state,Block.UPDATE_ALL);var be=(EscalatorEntity)level.getBlockEntity(p);be.setColors(14,9);
        var received=new EscalatorEntity(p,state);received.onDataPacket(null,be.getUpdatePacket(),level.registryAccess());
        h.assertTrue(received.paint()==14&&received.accent()==9,"Native block-entity packet carries both colors");
        be.setColors(11,4);received.onDataPacket(null,be.getUpdatePacket(),level.registryAccess());
        h.assertTrue(received.paint()==11&&received.accent()==4,"A later packet replaces both colors");
        var saved=be.saveWithoutMetadata(level.registryAccess());be.setColors(0,0);be.loadWithComponents(saved,level.registryAccess());
        h.assertTrue(be.paint()==11&&be.accent()==4,"World reload keeps the chosen colors");h.succeed();
    }
}
