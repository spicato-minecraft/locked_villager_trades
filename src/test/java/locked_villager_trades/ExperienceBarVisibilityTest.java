package locked_villager_trades;

import locked_villager_trades.util.ExperienceBarVisibility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceBarVisibilityTest {

    @Test
    void hidesBarWhileMultipleSetsAreSelectable() {
        assertTrue(ExperienceBarVisibility.shouldHide(4, false));
        assertTrue(ExperienceBarVisibility.shouldHideFromSyncedMaxIndex(3, false));
    }

    @Test
    void showsBarAfterFirstTradeLocksToOneSet() {
        assertFalse(ExperienceBarVisibility.shouldHide(4, true));
        assertFalse(ExperienceBarVisibility.shouldHideFromSyncedMaxIndex(3, true));
    }

    @Test
    void showsBarWhenOnlyOneSetExists() {
        assertFalse(ExperienceBarVisibility.shouldHide(1, false));
        assertFalse(ExperienceBarVisibility.shouldHideFromSyncedMaxIndex(0, false));
    }
}
