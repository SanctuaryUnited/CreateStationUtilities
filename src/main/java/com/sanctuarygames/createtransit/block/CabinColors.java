package com.sanctuarygames.createtransit.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Color storage shared by ordinary cabin pieces, Create seats and Create doors. */
public interface CabinColors {
    int paint(); int accent(); void setColors(int paint,int accent);
    static BlockPos colorPos(BlockPos p,BlockState s){if(s.getBlock() instanceof com.sanctuarygames.createtransit.elevator.ElevatorDoorBlock)return com.sanctuarygames.createtransit.elevator.ElevatorDoorBlock.base(p,s);if(s.getBlock() instanceof com.sanctuarygames.createtransit.lrt.LrtDoorBlock)return p.below(com.sanctuarygames.createtransit.lrt.LrtDoorBlock.row(s));if(s.getBlock() instanceof GondolaWideDoorBlock)return p.below(GondolaWideDoorBlock.row(s));return s.getBlock() instanceof GondolaDoorBlock&&s.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER?p.below():p;}
    static CabinColors get(BlockGetter l,BlockPos p,BlockState s){return l.getBlockEntity(colorPos(p,s)) instanceof CabinColors c?c:null;}
    static ItemInteractionResult dye(ItemStack stack,BlockState s,Level l,BlockPos p,Player player){
        if(!(stack.getItem() instanceof DyeItem d))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!player.mayBuild())return ItemInteractionResult.FAIL;
        var colors=get(l,p,s);
        if(!l.isClientSide&&colors!=null){
            int paint=player.isShiftKeyDown()?colors.paint():d.getDyeColor().getId(),accent=player.isShiftKeyDown()?d.getDyeColor().getId():colors.accent();
            if(paint!=colors.paint()||accent!=colors.accent()){if(s.getBlock() instanceof com.sanctuarygames.createtransit.elevator.ElevatorDoorBlock elevatorDoor)elevatorDoor.setColors(l,p,s,paint,accent);else if(s.getBlock() instanceof com.sanctuarygames.createtransit.lrt.LrtDoorBlock door)door.setColors(l,com.sanctuarygames.createtransit.lrt.LrtDoorBlock.base(p,s),s,paint,accent);else if(s.getBlock() instanceof GondolaWideDoorBlock wide)wide.setColors(l,GondolaWideDoorBlock.base(p,s),s,paint,accent);else colors.setColors(paint,accent);if(!player.getAbilities().instabuild)stack.shrink(1);}
        }
        return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    static void place(Level l,BlockPos p,BlockState s,ItemStack stack){var c=get(l,p,s);if(!l.isClientSide&&c!=null)c.setColors(TransitBlockItem.paint(stack).getId(),TransitBlockItem.accent(stack).getId());}
    static ItemStack pick(LevelReader l,BlockPos p,BlockState s){var c=get(l,p,s);return c==null?new ItemStack(s.getBlock()):TransitBlockItem.finished(s.getBlock(),c.paint(),c.accent());}
}
