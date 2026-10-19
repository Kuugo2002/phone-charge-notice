package com.kuugo.chargenotice;

final class BatteryLevel {
    private BatteryLevel() {}

    static int percentage(int level, int scale) {
        if (scale <= 0 || level < 0 || level > scale) {
            return -1;
        }
        // Never round a partially charged battery up to 100%.
        return (int) (level * 100L / scale);
    }
}
