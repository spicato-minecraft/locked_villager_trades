package locked_villager_trades.mixin;

import locked_villager_trades.LockedTradesAccessor;
import locked_villager_trades.util.LockedTradeData;
import locked_villager_trades.util.LockedTradesStorage;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Adds locked trades storage to Villager and persists it via ValueOutput/ValueInput (1.21.10+).
 */
@Mixin(Villager.class)
public abstract class VillagerPersistMixin implements LockedTradesAccessor {

    @Unique
    private Map<VillagerProfession, LockedTradeData> locked_villager_trades$lockedTrades = new HashMap<>();

    @Unique
    private int locked_villager_trades$generatingSetIndex = 0;

    @Unique
    private int locked_villager_trades$generatingDuplicateRetries = 0;

    @Override
    public Map<VillagerProfession, LockedTradeData> locked_villager_trades$getLockedTrades() {
        return locked_villager_trades$lockedTrades;
    }

    @Override
    public void locked_villager_trades$setLockedTrades(Map<VillagerProfession, LockedTradeData> trades) {
        this.locked_villager_trades$lockedTrades = trades != null ? trades : new HashMap<>();
    }

    @Override
    public void locked_villager_trades$setSelectedTradeSetIndex(VillagerProfession profession, int index) {
        LockedTradeData data = locked_villager_trades$lockedTrades.get(profession);
        if (data != null && data.tradeSets() != null && !data.tradeSets().isEmpty()) {
            int maxIndex = data.tradeSets().size() - 1;
            locked_villager_trades$lockedTrades.put(profession,
                    new LockedTradeData(data.tradeSets(), Math.max(0, Math.min(index, maxIndex)), data.locked(), data.lockedLevel()));
        }
    }

    @Override
    public void locked_villager_trades$setTradeSetLocked(VillagerProfession profession, boolean locked) {
        LockedTradeData data = locked_villager_trades$lockedTrades.get(profession);
        if (data != null) {
            locked_villager_trades$lockedTrades.put(profession,
                    new LockedTradeData(data.tradeSets(), data.selectedIndex(), locked, data.lockedLevel()));
        }
    }

    @Override
    public void locked_villager_trades$setLockedLevel(VillagerProfession profession, int level) {
        LockedTradeData data = locked_villager_trades$lockedTrades.get(profession);
        if (data != null) {
            locked_villager_trades$lockedTrades.put(profession,
                    new LockedTradeData(data.tradeSets(), data.selectedIndex(), data.locked(), level));
        }
    }

    @Override
    public int locked_villager_trades$getGeneratingSetIndex() {
        return locked_villager_trades$generatingSetIndex;
    }

    @Override
    public void locked_villager_trades$setGeneratingSetIndex(int value) {
        this.locked_villager_trades$generatingSetIndex = value;
    }

    @Override
    public int locked_villager_trades$getGeneratingDuplicateRetries() {
        return locked_villager_trades$generatingDuplicateRetries;
    }

    @Override
    public void locked_villager_trades$setGeneratingDuplicateRetries(int value) {
        this.locked_villager_trades$generatingDuplicateRetries = value;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void locked_villager_trades$writeNbt(ValueOutput output, CallbackInfo ci) {
        LockedTradesStorage.writeTo(output, locked_villager_trades$lockedTrades);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void locked_villager_trades$readNbt(ValueInput input, CallbackInfo ci) {
        Map<VillagerProfession, LockedTradeData> read = LockedTradesStorage.readFrom(input);
        if (!read.isEmpty()) {
            locked_villager_trades$lockedTrades = read;
        }
    }
}
