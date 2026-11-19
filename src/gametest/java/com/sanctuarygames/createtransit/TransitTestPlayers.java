package com.sanctuarygames.createtransit;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.UUID;
/** ServerPlayer subclass also supports Steam 'n' Rails' possession mixins. */
final class TransitTestPlayers {
    static FakePlayer makeMockPlayer(GameTestHelper helper,GameType mode){var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"TransitTest"));player.gameMode.changeGameModeForPlayer(mode);return player;}
}
