package com.kuugo.chargenotice;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EventChannel.EventSink batterySink;
    private boolean receiverRegistered;
    private MethodChannel.Result permissionResult;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (batterySink != null) batterySink.success(batteryState(intent));
        }
    };

    @Override public void configureFlutterEngine(@NonNull FlutterEngine engine) {
        super.configureFlutterEngine(engine);
        new MethodChannel(engine.getDartExecutor().getBinaryMessenger(), "charge_notice/control")
                .setMethodCallHandler((call, result) -> {
                    switch (call.method) {
                        case "initialize":
                            try {
                                startForegroundService(new Intent(this, ChargeMonitorService.class));
                                result.success(currentState());
                            } catch (Exception error) {
                                result.error("monitor_failed", error.getMessage(), null);
                            }
                            break;
                        case "getState": result.success(currentState()); break;
                        case "requestNotifications":
                            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                                    != PackageManager.PERMISSION_GRANTED) {
                                if (permissionResult != null) {
                                    result.error("permission_pending", "通知权限请求正在进行", null);
                                    break;
                                }
                                permissionResult = result;
                                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 40);
                            } else result.success(notificationsEnabled());
                            break;
                        case "testAlert":
                            if (!notificationsEnabled()) result.error("notifications_disabled", "请先在系统设置中允许通知", null);
                            else {
                                startForegroundService(new Intent(this, ChargeMonitorService.class)
                                        .setAction(ChargeMonitorService.ACTION_TEST));
                                result.success(null);
                            }
                            break;
                        case "openNotificationSettings":
                            startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
                            result.success(null);
                            break;
                        case "saveAppearance":
                            String mode = call.argument("mode");
                            String color = call.argument("color");
                            if (!("system".equals(mode) || "light".equals(mode) || "dark".equals(mode))
                                    || !("green".equals(color) || "blue".equals(color)
                                    || "purple".equals(color) || "orange".equals(color))) {
                                result.error("invalid_appearance", "无效的主题设置", null);
                            } else executor.execute(() -> {
                                boolean saved = AppPrefs.setAppearance(this, mode, color);
                                runOnUiThread(() -> {
                                    if (saved) result.success(null);
                                    else result.error("save_failed", "主题保存失败", null);
                                });
                            });
                            break;
                        case "setRootEnabled":
                            boolean enabled = Boolean.TRUE.equals(call.argument("enabled"));
                            executor.execute(() -> {
                                boolean success = enabled
                                        ? RootUtils.hasRoot() && RootUtils.enableKeepAlive(getPackageName())
                                        : RootUtils.disableKeepAlive(getPackageName());
                                if (success) AppPrefs.setRootEnabled(this, enabled);
                                runOnUiThread(() -> {
                                    if (success) result.success(enabled);
                                    else result.error("root_failed", "Root 操作未成功，请检查授权后重试", null);
                                });
                            });
                            break;
                        default: result.notImplemented();
                    }
                });
        new EventChannel(engine.getDartExecutor().getBinaryMessenger(), "charge_notice/battery")
                .setStreamHandler(new EventChannel.StreamHandler() {
                    @Override public void onListen(Object arguments, EventChannel.EventSink sink) {
                        batterySink = sink;
                        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                        receiverRegistered = true;
                    }
                    @Override public void onCancel(Object arguments) { removeBatteryReceiver(); }
                });
    }

    private boolean notificationsEnabled() {
        return getSystemService(NotificationManager.class).areNotificationsEnabled();
    }

    private Map<String, Object> currentState() {
        Map<String, Object> state = batteryState(registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
        state.put("notificationsEnabled", notificationsEnabled());
        state.put("rootEnabled", AppPrefs.isRootEnabled(this));
        state.put("mode", AppPrefs.themeMode(this));
        state.put("color", AppPrefs.themeColor(this));
        return state;
    }

    private Map<String, Object> batteryState(Intent battery) {
        Map<String, Object> state = new HashMap<>();
        int level = battery == null ? -1 : battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery == null ? 0 : battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        state.put("level", level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1);
        state.put("plugged", battery != null && battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0);
        return state;
    }

    @Override public void onRequestPermissionsResult(int code, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 40 && permissionResult != null) {
            permissionResult.success(notificationsEnabled());
            permissionResult = null;
        }
    }

    private void removeBatteryReceiver() {
        if (receiverRegistered) unregisterReceiver(batteryReceiver);
        receiverRegistered = false;
        batterySink = null;
    }

    @Override public void onDestroy() {
        removeBatteryReceiver();
        executor.shutdown();
        super.onDestroy();
    }
}
