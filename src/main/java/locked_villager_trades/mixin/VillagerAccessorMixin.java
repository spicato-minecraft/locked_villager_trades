package locked_villager_trades.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Villager.class)
public interface VillagerAccessorMixin {

    @Invoker("resendOffersToTradingPlayer")
    void locked_villager_trades$invokeResendOffersToTradingPlayer();

    @Invoker("updateTrades")
    void locked_villager_trades$invokeUpdateTrades(ServerLevel level);

    @Invoker("updateSpecialPrices")
    void locked_villager_trades$invokeUpdateSpecialPrices(Player player);

    @Invoker("addAdditionalSaveData")
    void locked_villager_trades$invokeAddAdditionalSaveData(ValueOutput output);

    @Invoker("readAdditionalSaveData")
    void locked_villager_trades$invokeReadAdditionalSaveData(ValueInput input);
}
