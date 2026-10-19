package com.kuugo.chargenotice;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BatteryLevelTest {
    @Test
    public void incompleteBatteryNeverReachesOneHundredPercent() {
        for (int level = 95; level < 100; level++) {
            assertEquals(level, BatteryLevel.percentage(level, 100));
        }
        assertEquals(99, BatteryLevel.percentage(199, 200));
        assertEquals(99, BatteryLevel.percentage(999, 1000));
        assertEquals(99, BatteryLevel.percentage(Integer.MAX_VALUE - 1, Integer.MAX_VALUE));
    }

    @Test
    public void onlyExactFullScaleReachesOneHundredPercent() {
        int[] scales = {1, 100, 200, 1000, Integer.MAX_VALUE};
        for (int scale : scales) {
            assertEquals(100, BatteryLevel.percentage(scale, scale));
        }
    }

    @Test
    public void invalidReadingsAreUnavailable() {
        assertEquals(-1, BatteryLevel.percentage(-1, 100));
        assertEquals(-1, BatteryLevel.percentage(100, -1));
        assertEquals(-1, BatteryLevel.percentage(100, 0));
        assertEquals(-1, BatteryLevel.percentage(101, 100));
        assertEquals(-1, BatteryLevel.percentage(Integer.MAX_VALUE, 100));
    }

    @Test
    public void everyPartialReadingStaysBelowFullScale() {
        for (int scale = 1; scale <= 1000; scale++) {
            for (int level = 0; level < scale; level++) {
                int percent = BatteryLevel.percentage(level, scale);
                assertTrue(percent >= 0 && percent < 100);
            }
        }
    }
}
