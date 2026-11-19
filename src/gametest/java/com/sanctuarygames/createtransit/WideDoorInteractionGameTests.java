package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.block.GondolaWideDoorBlock;
import com.sanctuarygames.createtransit.block.GondolaDoorBlockEntity;
import com.sanctuarygames.createtransit.block.WideDoorTiming;
import com.sanctuarygames.createtransit.registry.ModBlocks;
import com.sanctuarygames.createtransit.registry.ModSounds;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.decoration.slidingDoor.DoorControl;
import com.simibubi.create.content.decoration.slidingDoor.DoorControlBehaviour;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

/** Exercise actual registered interaction entrypoints without loading a graphical client. */
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class WideDoorInteractionGameTests {
    private static void check(boolean b,String m){if(!b)throw new GameTestAssertException(m);}
    private static final class TestLevel extends WrappedLevel {
        private final boolean client;
        private int sounds;
        private SoundEvent lastSound;
        TestLevel(Level original,boolean client){super(original);this.client=client;}
        @Override public boolean isClientSide(){return client;}
        @Override public void playSound(Player excluded,BlockPos p,SoundEvent sound,SoundSource source,float volume,float pitch){check(!client,"Client must not broadcast door sound");check(excluded==null,"Clicking player must receive the server sound");sounds++;lastSound=sound;}
    }
    private static final class TestContraption extends CarriageContraption {
        private int invalidations;
        private GondolaDoorBlockEntity renderedDoor;
        TestContraption(){super(Direction.SOUTH);}
        @Override public void invalidateColliders(){super.invalidateColliders();invalidations++;}
        @Override public BlockEntity getBlockEntityClientSide(BlockPos p){return renderedDoor!=null&&p.equals(BlockPos.ZERO)?renderedDoor:super.getBlockEntityClientSide(p);}
    }
    private static final class TestEntity extends OrientedContraptionEntity {
        private int updates;
        TestEntity(Level level,TestContraption c){super(AllEntityTypes.ORIENTED_CONTRAPTION.get(),level);contraption=c;c.entity=this;}
        @Override public void setBlock(BlockPos p,StructureBlockInfo info){check(!level().isClientSide(),"Regression: client interaction tried to publish clientbound packets");updates++;contraption.getBlocks().put(p,info);}
    }
    private static TestContraption frame(boolean tall,Direction f){
        var c=new TestContraption();var b=ModBlocks.GONDOLA_DOUBLE_DOOR.get();var s=b.defaultBlockState().setValue(GondolaWideDoorBlock.TALL,tall).setValue(DoorBlock.FACING,f);
        for(int col=0;col<3;col++)for(int row=0;row<GondolaWideDoorBlock.rows(s);row++){
            var p=GondolaWideDoorBlock.part(BlockPos.ZERO,s,col,row);var state=s.setValue(GondolaWideDoorBlock.COLUMN,col).setValue(GondolaWideDoorBlock.ROW,tall?row:0).setValue(DoorBlock.HALF,row==0?DoubleBlockHalf.LOWER:DoubleBlockHalf.UPPER);
            var nbt=new CompoundTag();nbt.putInt("paint",0);nbt.putInt("accent",1);nbt.putString("sentinel",col+"/"+row);c.getBlocks().put(p,new StructureBlockInfo(p,state,nbt));c.bounds=c.bounds==null?new AABB(p):c.bounds.minmax(new AABB(p));
        }return c;
    }
    @GameTest(template="empty",timeoutTicks=35)
    public static void client_click_accepts_every_cell_without_updates_or_prediction(GameTestHelper h){
        var l=new TestLevel(h.getLevel(),true);var player=FakePlayerFactory.getMinecraft(h.getLevel());var interaction=MovingInteractionBehaviour.REGISTRY.get(ModBlocks.GONDOLA_DOUBLE_DOOR.get());
        for(boolean tall:new boolean[]{false,true})for(Direction f:Direction.Plane.HORIZONTAL){
            var c=frame(tall,f);var entity=new TestEntity(l,c);var saved=java.util.Map.copyOf(c.getBlocks());
            for(var info:saved.values())for(var hand:InteractionHand.values()){
                check(interaction.handlePlayerInteraction(player,hand,info.pos(),entity),"Accepted client click allows Create to send its server interaction packet");
                check(c.getBlocks().equals(saved),"Client waits for authoritative states without toggling any cell");
                WideDoorMovement.setOpen(c,info.pos(),info.state(),true);check(c.getBlocks().equals(saved),"Direct client setOpen is also guarded");
            }
            check(entity.updates==0&&c.invalidations==0&&l.sounds==0,"Client sends no server-only block/sound updates or collider invalidations");
            check(!interaction.handlePlayerInteraction(player,InteractionHand.MAIN_HAND,new BlockPos(20,0,20),entity),"Stale client position is rejected safely");
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=35)
    public static void server_click_toggles_whole_frame_once_and_keeps_nbt_and_sound(GameTestHelper h){
        var l=new TestLevel(h.getLevel(),false);var player=FakePlayerFactory.getMinecraft(h.getLevel());var interaction=MovingInteractionBehaviour.REGISTRY.get(ModBlocks.GONDOLA_DOUBLE_DOOR.get());
        for(boolean tall:new boolean[]{false,true})for(Direction f:Direction.Plane.HORIZONTAL){
            var c=frame(tall,f);var entity=new TestEntity(l,c);var saved=java.util.Map.copyOf(c.getBlocks());boolean open=false;int clicks=0,soundsBefore=l.sounds;
            for(var clicked:saved.values())for(var hand:InteractionHand.values()){
                check(interaction.handlePlayerInteraction(player,hand,clicked.pos(),entity),"Server accepts every door cell");open=!open;clicks++;
                for(var info:c.getBlocks().values()){check(info.state().getValue(DoorBlock.OPEN)==open,"All door cells toggle together");check(info.nbt().equals(saved.get(info.pos()).nbt()),"Updates preserve dye and other captured NBT");}
                check(entity.updates==clicks*saved.size()&&c.invalidations==clicks,"Each changed cell publishes once and colliders invalidate once");
                check(l.sounds==soundsBefore+clicks&&l.lastSound==(open?ModSounds.GONDOLA_OPEN:ModSounds.GONDOLA_CLOSE).get(),"One matching chime/hiss cue per server click");
            }
            var first=c.getBlocks().values().iterator().next();WideDoorMovement.setOpen(c,first.pos(),first.state(),open);check(entity.updates==clicks*saved.size()&&c.invalidations==clicks,"Already-correct state causes no duplicate updates");
            check(!interaction.handlePlayerInteraction(player,InteractionHand.MAIN_HAND,new BlockPos(20,0,20),entity),"Missing server cell is rejected");
            c.getBlocks().put(first.pos(),new StructureBlockInfo(first.pos(),Blocks.AIR.defaultBlockState(),null));check(!interaction.handlePlayerInteraction(player,InteractionHand.MAIN_HAND,first.pos(),entity),"Removed door state is rejected without property errors");
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=35)
    public static void wide_motion_leds_and_reversal_preserve_all_accents(GameTestHelper h){
        var b=ModBlocks.GONDOLA_DOUBLE_DOOR.get();
        for(boolean tall:new boolean[]{false,true})for(Direction f:Direction.Plane.HORIZONTAL)for(DyeColor dye:DyeColor.values()){
            var s=b.defaultBlockState().setValue(GondolaWideDoorBlock.TALL,tall).setValue(DoorBlock.FACING,f);
            var be=new GondolaDoorBlockEntity(BlockPos.ZERO,s);be.setColors(0,dye.getId());int accent=dye.getTextureDiffuseColor();
            check(be.progress(1)==0&&be.wideLedColor(1)==accent&&be.centerLedColor(1)==WideDoorTiming.OFF,"Initially closed LEDs show the saved accent");
            be.setBlockState(s.setValue(DoorBlock.OPEN,true));float previous=0;
            for(int tick=1;tick<=WideDoorTiming.OPEN_TICKS;tick++){
                be.advance(true);check(be.progress(0)==previous,"Opening interpolation begins at previous frame");
                int expected=tick==WideDoorTiming.OPEN_TICKS||tick/WideDoorTiming.FLASH_TICKS%2==0?WideDoorTiming.GREEN:WideDoorTiming.OFF;
                check(be.progress(1)>previous&&be.wideLedColor(1)==expected&&be.centerLedColor(1)==expected,"Opening progresses with flashing green; fully open is steady green");
                if(tick<WideDoorTiming.OPEN_TICKS)check(be.progress(1)<1,"Door cannot finish before 32 ticks");previous=be.progress(1);
            }
            check(previous==1,"Opening finishes exactly at 32 ticks");
            be.setBlockState(s);check(be.wideLedColor(1)==WideDoorTiming.YELLOW,"Closing warning starts yellow immediately");
            boolean yellow=false,off=false;
            for(int tick=1;tick<=WideDoorTiming.CLOSE_TICKS;tick++){
                be.advance(false);check(be.progress(0)==previous&&be.progress(1)<previous,"Closing is continuous and monotone");
                int signal=be.wideLedColor(1);if(tick<WideDoorTiming.CLOSE_TICKS){check(signal==WideDoorTiming.YELLOW||signal==WideDoorTiming.OFF,"Moving closure only shows flashing yellow/off");check(be.centerLedColor(1)==signal,"Center signal flashes with the closing lamps");yellow|=signal==WideDoorTiming.YELLOW;off|=signal==WideDoorTiming.OFF;}
                previous=be.progress(1);
            }
            check(previous==0&&yellow&&off&&be.wideLedColor(1)==accent&&be.centerLedColor(1)==WideDoorTiming.OFF,"Fully shut restores every saved accent after actual flashing");
            check(be.progress(0)>0&&be.wideLedColor(0)!=WideDoorTiming.GREEN,"Last closing frame remains interpolated until shut");
            be.setBlockState(s.setValue(DoorBlock.OPEN,true));for(int i=0;i<16;i++)be.advance(true);float half=be.progress(1);
            be.setBlockState(s);be.advance(false);check(be.progress(0)==half,"A mid-opening reversal keeps its position");float reversing=be.progress(1);
            be.setBlockState(s.setValue(DoorBlock.OPEN,true));be.advance(true);check(be.progress(0)==reversing&&be.wideLedColor(1)==WideDoorTiming.GREEN,"Reopening reverses without snapping and cancels yellow");
            check(be.accent()==dye.getId(),"Temporary indications never overwrite stored dye");
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=35)
    public static void moving_wide_door_animates_client_and_sounds_once_on_server(GameTestHelper h){
        for(boolean tall:new boolean[]{false,true})for(Direction f:Direction.Plane.HORIZONTAL){
            var client=new TestLevel(h.getLevel(),true);var c=frame(tall,f);var entity=new TestEntity(client,c);var info=c.getBlocks().get(BlockPos.ZERO);
            c.renderedDoor=new GondolaDoorBlockEntity(BlockPos.ZERO,info.state());c.renderedDoor.setColors(0,DyeColor.BLUE.getId());
            var context=new MovementContext(client,info,c);context.position=Vec3.ZERO;var behaviour=new WideDoorMovement();
            c.getBlocks().replaceAll((p,i)->new StructureBlockInfo(p,i.state().setValue(DoorBlock.OPEN,true),i.nbt()));
            for(int i=0;i<WideDoorTiming.OPEN_TICKS;i++)behaviour.tick(context);
            check(c.renderedDoor.progress(1)==1&&c.renderedDoor.wideLedColor(1)==WideDoorTiming.GREEN&&c.renderedDoor.centerLedColor(1)==WideDoorTiming.GREEN,"Create client actor opens with new timing/green signal");
            c.getBlocks().replaceAll((p,i)->new StructureBlockInfo(p,i.state().setValue(DoorBlock.OPEN,false),i.nbt()));
            for(int i=0;i<WideDoorTiming.CLOSE_TICKS;i++)behaviour.tick(context);
            check(c.renderedDoor.progress(1)==0&&c.renderedDoor.wideLedColor(1)==DyeColor.BLUE.getTextureDiffuseColor()&&c.renderedDoor.centerLedColor(1)==WideDoorTiming.OFF,"Create client actor closes, restores dye and turns the center signal off");
            check(client.sounds==0&&entity.updates==0&&c.invalidations==0,"Client animation broadcasts no sound or block updates");
            var server=new TestLevel(h.getLevel(),false);var train=frame(tall,f);new TestEntity(server,train);var actor=new MovementContext(server,train.getBlocks().get(BlockPos.ZERO),train);actor.position=new Vec3(5,6,7);actor.firstMovement=false;
            var controls=new DoorControlBehaviour(new GondolaDoorBlockEntity(BlockPos.ZERO,actor.state));controls.mode=DoorControl.ALL;actor.temporaryData=new java.lang.ref.WeakReference<>(controls);actor.motion=Vec3.ZERO;
            behaviour.tick(actor);check(server.sounds==1&&server.lastSound==ModSounds.GONDOLA_OPEN.get(),"Create controller opening emits one combined chime/hiss cue");
            behaviour.tick(actor);check(server.sounds==1,"Settled controller decision does not repeat audio");
            controls.mode=DoorControl.NONE;behaviour.tick(actor);check(server.sounds==2&&server.lastSound==ModSounds.GONDOLA_CLOSE.get(),"Create controller closing emits one two-note/hiss cue");
            behaviour.tick(actor);check(server.sounds==2,"Settled closure does not repeat audio");
        }h.succeed();
    }
}
