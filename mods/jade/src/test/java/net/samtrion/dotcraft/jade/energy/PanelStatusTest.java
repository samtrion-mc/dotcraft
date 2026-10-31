package net.samtrion.dotcraft.jade.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** The one status word a panel shows without the details key. */
class PanelStatusTest {
    /** Nothing limits a panel in the sun on a clear day. */
    @Test
    void noStatusWhenUnlimited() {
        assertNull(PanelStatus.of(false, true, false, "CLEAR", 0.5, true));
        assertNull(PanelStatus.of(false, true, false, "CLEAR", 0.5, false));
    }

    /** Each reason alone is reported; sun and moon differ only in the word for no light. */
    @Test
    void singleReasons() {
        assertEquals("occupied", PanelStatus.of(true, true, false, "CLEAR", 1, true));
        assertEquals("no_sky", PanelStatus.of(false, false, false, "CLEAR", 1, true));
        assertEquals("waterlogged", PanelStatus.of(false, true, true, "CLEAR", 1, true));
        assertEquals("thunder", PanelStatus.of(false, true, false, "THUNDER", 1, true));
        assertEquals("rain", PanelStatus.of(false, true, false, "RAIN", 1, true));
        assertEquals("night", PanelStatus.of(false, true, false, "CLEAR", 0, true));
        assertEquals("day", PanelStatus.of(false, true, false, "CLEAR", 0, false));
    }

    /** With several reasons the first of {@link PanelStatus#ALL} wins. */
    @Test
    void priority() {
        assertEquals("occupied", PanelStatus.of(true, false, true, "THUNDER", 0, true));
        assertEquals("no_sky", PanelStatus.of(false, false, true, "RAIN", 0, true));
        assertEquals("waterlogged", PanelStatus.of(false, true, true, "RAIN", 0, true));
        assertEquals("thunder", PanelStatus.of(false, true, false, "THUNDER", 0, true));
        assertEquals("rain", PanelStatus.of(false, true, false, "RAIN", 0, true));
    }
}
