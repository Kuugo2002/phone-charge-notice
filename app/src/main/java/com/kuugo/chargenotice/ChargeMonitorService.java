package com.kuugo.chargenotice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.PowerManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChargeMonitorService extends Service {
    static final String ACTION_TEST = "com.kuugo.chargenotice.TEST_ALERT";
    static final String ACTION_STATE_CHANGED = "com.kuugo.chargenotice.STATE_CHANGED";
    static final String EXTRA_LEVEL = "level";
    static final String EXTRA_PLUGGED = "plugged";

    private static final String MONITOR_CHANNEL = "charge_monitor";
    private static final String ALERT_CHANNEL = "battery_full_alert";
    private static final int MONITOR_NOTIFICATION_ID = 1001;
    private static final int FULL_NOTIFICATION_ID = 1002;

    private final ExecutorService rootExecutor = Executors.newSingleThreadExecutor();

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleBattery(intent);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
        startForeground(MONITOR_NOTIFICATION_ID, buildMonitorNotification());
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TEST.equals(intent.getAction())) {
            showFullAlert(true);
        }
        return START_STICKY;
    }

    private void handleBattery(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int pluggedValue = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int percent = BatteryLevel.percentage(level, scale);
        boolean plugged = pluggedValue != 0;
        // A system FULL status alone must not trigger an alert below 100%.
        boolean full = percent == 100;

        Intent state = new Intent(ACTION_STATE_CHANGED)
                .setPackage(getPackageName())
                .putExtra(EXTRA_LEVEL, percent)
                .putExtra(EXTRA_PLUGGED, plugged);
        sendBroadcast(state);

        if (!plugged) {
            AppPrefs.setNotifiedThisCharge(this, false);
        } else if (full && !AppPrefs.wasNotifiedThisCharge(this)) {
            AppPrefs.setNotifiedThisCharge(this, true);
            showFullAlert(false);
        }
    }

    private void showFullAlert(boolean test) {
        wakeScreen();
        if (AppPrefs.isRootEnabled(this)) {
            rootExecutor.execute(RootUtils::wakeScreen);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = test ? "测试成功" : "手机已充满电";
        String text = test ? "亮屏与通知功能工作正常" : "电量已达到 100%，可以拔掉充电器了";
        Notification notification = new Notification.Builder(this, ALERT_CHANNEL)
                .setSmallIcon(R.drawable.ic_battery_full)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .build();
        getSystemService(NotificationManager.class).notify(FULL_NOTIFICATION_ID, notification);
    }

    @SuppressWarnings("deprecation")
    private void wakeScreen() {
        PowerManager powerManager = getSystemService(PowerManager.class);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP
                        | PowerManager.ON_AFTER_RELEASE,
                "ChargeNotice:BatteryFull");
        wakeLock.acquire(8_000L);
    }

    private Notification buildMonitorNotification() {
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                1,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, MONITOR_CHANNEL)
                .setSmallIcon(R.drawable.ic_battery_full)
                .setContentTitle("充满提醒正在运行")
                .setContentText("将在电量达到 100% 时提醒你")
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void createChannels() {
        NotificationManager manager = getSystemService(NotificationManager.class);

        NotificationChannel monitor = new NotificationChannel(
                MONITOR_CHANNEL, "电量监控服务", NotificationManager.IMPORTANCE_LOW);
        monitor.setDescription("显示充满提醒的后台监控状态");
        manager.createNotificationChannel(monitor);

        NotificationChannel alert = new NotificationChannel(
                ALERT_CHANNEL, "充满电提醒", NotificationManager.IMPORTANCE_HIGH);
        alert.setDescription("电量达到 100% 时弹出的提醒");
        alert.enableVibration(true);
        alert.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(alert);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(batteryReceiver);
        rootExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
