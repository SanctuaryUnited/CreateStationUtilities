package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class InsetCabinGameTests {
    private static void check(boolean b,String m){if(!b)throw new GameTestAssertException(m);}
    private static Direction corner(int x,int z){return x==0?(z==0?Direction.NORTH:Direction.WEST):(z==0?Direction.EAST:Direction.SOUTH);}
    private static Direction edge(int x,int z){return z==0?Direction.SOUTH:z==4?Direction.NORTH:x==0?Direction.EAST:Direction.WEST;}
    @GameTest(template="empty",timeoutTicks=50)
    public static void inset_five_by_five_cabin_keeps_narrow_shell_and_assembles(GameTestHelper h){
        var l=h.getLevel();var origin=h.absolutePos(new BlockPos(3,1,3));var bogey=origin.offset(2,0,2);
        l.setBlock(bogey,AllBlocks.SMALL_BOGEY.getDefaultState().setValue(BlockStateProperties.HORIZONTAL_AXIS,Direction.Axis.Z),3);
        double minX=5,maxX=0,minZ=5,maxZ=0;
        for(int x=0;x<5;x++)for(int z=0;z<5;z++){
            boolean outside=x==0||x==4||z==0||z==4,atCorner=(x==0||x==4)&&(z==0||z==4);
            var floor=(atCorner?ModBlocks.GONDOLA_FLOOR_OUTER_CORNER:outside?ModBlocks.GONDOLA_FLOOR_EDGE:ModBlocks.GONDOLA_FLOOR).get();var p=origin.offset(x,1,z);var s=floor.defaultBlockState().setValue(GondolaBlock.FACING,atCorner?corner(x,z):edge(x,z));l.setBlock(p,s,3);
            var box=s.getCollisionShape(l,p).bounds();minX=Math.min(minX,x+box.minX);maxX=Math.max(maxX,x+box.maxX);minZ=Math.min(minZ,z+box.minZ);maxZ=Math.max(maxZ,z+box.maxZ);
            if(outside){
                var trim=(atCorner?ModBlocks.GONDOLA_LED_HORIZONTAL_OUTER_CORNER:ModBlocks.GONDOLA_LED_HORIZONTAL).get();l.setBlock(origin.offset(x,5,z),trim.defaultBlockState().setValue(GondolaBlock.FACING,atCorner?corner(x,z):edge(x,z)),3);
                for(int y=2;y<5;y++){
                    if(atCorner)l.setBlock(origin.offset(x,y,z),ModBlocks.GONDOLA_LED_VERTICAL_OUTER_CORNER.get().defaultBlockState().setValue(GondolaBlock.FACING,corner(x,z)),3);
                    else if(z!=0)l.setBlock(origin.offset(x,y,z),ModBlocks.GONDOLA_WINDOW.get().defaultBlockState().setValue(GondolaBlock.FACING,edge(x,z)),3);
                }
            }else if(x==2&&z==2)l.setBlock(origin.offset(x,5,z),ModBlocks.GONDOLA_HANGER.get().defaultBlockState(),3);
            else{
                var f=z==1?Direction.SOUTH:z==3?Direction.NORTH:x==1?Direction.EAST:Direction.WEST;
                if(x==3&&z==1)f=Direction.WEST;if(x==1&&z==3)f=Direction.EAST;
                l.setBlock(origin.offset(x,5,z),ModBlocks.GONDOLA_ROOF_EDGE.get().defaultBlockState().setValue(StairBlock.FACING,f),3);
            }
        }
        check(minX==.8125&&maxX==4.1875&&minZ==.8125&&maxZ==4.1875,"Floor occupies five cells but visible shell is 3.375 blocks including trim");
        for(int z=1;z<4;z++)for(int x:new int[]{1,3})l.setBlock(origin.offset(x,2,z),ModBlocks.GONDOLA_SEAT.get().defaultBlockState().setValue(GondolaBlock.FACING,x==1?Direction.EAST:Direction.WEST),3);
        var door=origin.offset(3,2,0);var b=ModBlocks.GONDOLA_DOUBLE_DOOR.get();var ds=b.placementState().setValue(DoorBlock.FACING,Direction.SOUTH);l.setBlock(door,ds,3);b.setPlacedBy(l,door,ds,null,TransitBlockItem.finished(b,DyeColor.WHITE.getId(),DyeColor.BLUE.getId()));
        for(int col=0;col<3;col++)for(int row=0;row<3;row++)check(l.getBlockState(door.west(col).above(row)).is(b),"Inset front has all nine door cells");
        var glue=new SuperGlueEntity(l,SuperGlueEntity.span(origin,origin.offset(4,5,4)));l.addFreshEntity(glue);var c=new CarriageContraption(Direction.SOUTH);
        try{check(c.assemble(l,bogey),"Complete inset cabin assembles with a real Create bogey");}catch(AssemblyException e){throw new GameTestAssertException(e.getMessage());}
        check(c.getBlocks().size()==105,"Create captures all 105 cabin/bogey cells");check(c.getSeats().size()==6,"Create recognizes all six interior seats");check(c.getBlocks().containsKey(origin.offset(4,5,4).subtract(bogey)),"Carriage includes outer LED roof corner");
        c.startMoving(l);var restored=Contraption.fromNBT(l,c.writeNBT(l.registryAccess(),false),false);check(restored.getBlocks().size()==105&&restored.getSeats().size()==6,"Complete cabin round-trips Create serialization");glue.discard();h.succeed();
    }
}
