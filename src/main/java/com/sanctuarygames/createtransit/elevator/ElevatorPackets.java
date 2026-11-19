package com.sanctuarygames.createtransit.elevator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Bounded server actions, validated against the sender's currently open menu. */
public final class ElevatorPackets {
    private ElevatorPackets(){}
    public record Action(int menuId,int operation,int floorY,String first,String second,boolean enabled,int opening,int closing,int dwell) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(ElevatorRegistry.id("elevator_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=new StreamCodec<>(){
            public Action decode(RegistryFriendlyByteBuf b){return new Action(b.readVarInt(),b.readVarInt(),b.readInt(),b.readUtf(32),b.readUtf(256),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,Action p){b.writeVarInt(p.menuId());b.writeVarInt(p.operation());b.writeInt(p.floorY());b.writeUtf(p.first(),32);b.writeUtf(p.second(),256);b.writeBoolean(p.enabled());b.writeVarInt(p.opening());b.writeVarInt(p.closing());b.writeVarInt(p.dwell());}
        };
        public Type<Action> type(){return TYPE;}
    }
    public record Snapshot(int menuId,CompoundTag state) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=new Type<>(ElevatorRegistry.id("elevator_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=new StreamCodec<>(){
            public Snapshot decode(RegistryFriendlyByteBuf b){int id=b.readVarInt();var tag=b.readNbt();return new Snapshot(id,tag==null?new CompoundTag():tag);}
            public void encode(RegistryFriendlyByteBuf b,Snapshot p){b.writeVarInt(p.menuId());b.writeNbt(p.state());}
        };
        public Type<Snapshot> type(){return TYPE;}
    }
    public record SelectorScroll(int cabinId,int delta) implements CustomPacketPayload {
        public static final Type<SelectorScroll> TYPE=new Type<>(ElevatorRegistry.id("elevator_selector_scroll"));
        public static final StreamCodec<RegistryFriendlyByteBuf,SelectorScroll> CODEC=new StreamCodec<>(){
            public SelectorScroll decode(RegistryFriendlyByteBuf b){return new SelectorScroll(b.readVarInt(),b.readByte());}
            public void encode(RegistryFriendlyByteBuf b,SelectorScroll p){b.writeVarInt(p.cabinId());b.writeByte(p.delta());}
        };
        public Type<SelectorScroll> type(){return TYPE;}
    }
    public record DestinationScroll(net.minecraft.core.BlockPos pos,int delta) implements CustomPacketPayload {
        public static final Type<DestinationScroll> TYPE=new Type<>(ElevatorRegistry.id("elevator_destination_scroll"));
        public static final StreamCodec<RegistryFriendlyByteBuf,DestinationScroll> CODEC=new StreamCodec<>(){
            public DestinationScroll decode(RegistryFriendlyByteBuf b){return new DestinationScroll(b.readBlockPos(),b.readByte());}
            public void encode(RegistryFriendlyByteBuf b,DestinationScroll p){b.writeBlockPos(p.pos());b.writeByte(p.delta());}
        };
        public Type<DestinationScroll> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event){
        var registrar=event.registrar("elevator3-dev18.4");
        registrar.playToServer(DestinationScroll.TYPE,DestinationScroll.CODEC,(packet,context)->{
            var player=context.player();
            if(!(player.level() instanceof net.minecraft.server.level.ServerLevel level)||!level.hasChunkAt(packet.pos()))return;
            var state=level.getBlockState(packet.pos());
            if(!(state.getBlock() instanceof ElevatorCallBlock call)||!call.destinationSelector())return;
            if(player.pick(player.blockInteractionRange(),1,false) instanceof net.minecraft.world.phys.BlockHitResult hit
                    &&hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&hit.getBlockPos().equals(packet.pos())
                    &&ElevatorCallBlock.screenHit(state,hit))ElevatorCallBlock.scrollDestination(level,packet.pos(),packet.delta());
        });
        registrar.playToServer(SelectorScroll.TYPE,SelectorScroll.CODEC,(p,c)->{
            var player=c.player();
            if(player.level().getEntity(p.cabinId()) instanceof com.sanctuarygames.createtransit.elevator.model.ModelCabinEntity cabin)
                cabin.scrollSelector(player,p.delta());
        });
        registrar.playToServer(Action.TYPE,Action.CODEC,(payload,context)->{if(context.player().containerMenu instanceof ElevatorMenu menu)menu.action(context.player(),payload);else if(context.player().containerMenu instanceof com.sanctuarygames.createtransit.radio.SpeakerMenu speaker)speaker.action(context.player(),payload);else if(context.player().containerMenu instanceof com.sanctuarygames.createtransit.escalator.EscalatorMenu escalator)escalator.action(context.player(),payload);});
        registrar.playToClient(Snapshot.TYPE,Snapshot.CODEC,(payload,context)->{if(context.player().containerMenu instanceof ElevatorMenu menu&&menu.containerId==payload.menuId())menu.accept(payload.state());else if(context.player().containerMenu instanceof com.sanctuarygames.createtransit.radio.SpeakerMenu speaker&&speaker.containerId==payload.menuId())speaker.accept(payload.state());});
    }
}
