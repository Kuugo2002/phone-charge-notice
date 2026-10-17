package com.kuugo.chargenotice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

final class RootUtils {
    private RootUtils() {}

    static boolean hasRoot() {
        return run("id");
    }

    static boolean enableKeepAlive(String packageName) {
        boolean whitelist = run("cmd deviceidle whitelist +" + packageName);
        run("cmd appops set " + packageName + " RUN_IN_BACKGROUND allow");
        run("cmd appops set " + packageName + " RUN_ANY_IN_BACKGROUND allow");
        return whitelist;
    }

    static boolean disableKeepAlive(String packageName) {
        return run("cmd deviceidle whitelist -" + packageName);
    }

    static void wakeScreen() {
        run("input keyevent KEYCODE_WAKEUP");
    }

    private static boolean run(String command) {
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // Drain output so the root process cannot block.
                }
            }
            if (!process.waitFor(8, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
