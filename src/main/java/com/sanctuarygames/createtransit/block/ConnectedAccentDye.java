package com.sanctuarygames.createtransit.block;

import com.sanctuarygames.createtransit.CreateTransit;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Server-owned, bounded primary/secondary-color flood fill. Never traverses another mod or loads chunks. */
public final class ConnectedAccentDye {
    public static final int DOUBLE_CLICK_TICKS=10, MAX_BLOCKS=4096;
    private record Click(net.minecraft.resources.ResourceKey<Level> dimension,BlockPos pos,InteractionHand hand,int color,boolean secondary,long tick) {}
    private static final Map<Player,Click> CLICKS=new WeakHashMap<>();
    private ConnectedAccentDye() {}
    public static boolean transit(Level level,BlockPos pos){return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getNamespace().equals(CreateTransit.MOD_ID);}

    public static void interact(PlayerInteractEvent.RightClickBlock event){
        var level=event.getLevel();if(level.isClientSide)return;
        var player=event.getEntity();var stack=event.getItemStack();var pos=event.getPos();
        if(event.isCanceled()||event.getUseBlock()==TriState.FALSE||event.getUseItem()==TriState.FALSE||!player.mayBuild()||!(stack.getItem() instanceof DyeItem dye)||!transit(level,pos)){
            CLICKS.remove(player);return;
        }
        long now=level.getGameTime();var previous=CLICKS.remove(player);int color=dye.getDyeColor().getId();boolean secondary=player.isShiftKeyDown();
        if(previous==null||!previous.dimension().equals(level.dimension())||!previous.pos().equals(pos)||previous.hand()!=event.getHand()||previous.color()!=color||previous.secondary()!=secondary||now<previous.tick()||now-previous.tick()>DOUBLE_CLICK_TICKS){
            CLICKS.put(player,new Click(level.dimension(),pos.immutable(),event.getHand(),color,secondary,now));return;
        }
        // Consume the second interaction so the normal block handler cannot charge/apply it again.
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        apply(level,pos,player,stack,event.getFace());
    }

    /** Preflight the entire component before changing anything or consuming the bulk-operation dye. */
    public static int apply(Level level,BlockPos origin,Player player,ItemStack stack,Direction face){
        if(level.isClientSide||!player.mayBuild()||stack.isEmpty()||!(stack.getItem() instanceof DyeItem dye)||!level.hasChunkAt(origin)||!transit(level,origin))return 0;
        boolean secondary=player.isShiftKeyDown();
        var todo=new ArrayDeque<BlockPos>();var visited=new HashSet<BlockPos>();var targets=new ArrayList<CabinColors>();
        int connected=0;todo.add(origin.immutable());
        while(!todo.isEmpty()){
            var pos=todo.removeFirst();if(!visited.add(pos)||level.isOutsideBuildHeight(pos))continue;
            if(!level.hasChunkAt(pos)){message(player,"unloaded");return 0;}
            if(!transit(level,pos))continue;
            if(++connected>MAX_BLOCKS){message(player,"limit",MAX_BLOCKS);return 0;}
            if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,face,stack)){message(player,"protected");return 0;}
            if(level.getBlockEntity(pos) instanceof CabinColors colors&&(secondary?colors.accent():colors.paint())!=dye.getDyeColor().getId())targets.add(colors);
            for(var direction:Direction.values())todo.add(pos.relative(direction));
        }
        for(var colors:targets)colors.setColors(secondary?colors.paint():dye.getDyeColor().getId(),secondary?dye.getDyeColor().getId():colors.accent());
        if(!targets.isEmpty()&&!player.getAbilities().instabuild)stack.shrink(1);
        message(player,(secondary?"":"primary_")+(targets.isEmpty()?"unchanged":"applied"),targets.size());return targets.size();
    }
    private static void message(Player player,String key,Object... args){player.displayClientMessage(Component.translatable("message.createtransit.connected_dye."+key,args),true);}
}
