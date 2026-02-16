package locked_villager_trades.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessorMixin {

    @Invoker("addDataSlots")
    void locked_villager_trades$invokeAddDataSlots(ContainerData containerData);
}
