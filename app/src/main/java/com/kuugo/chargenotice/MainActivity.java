package com.kuugo.chargenotice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int NOTIFICATION_REQUEST = 40;

    private TextView statusText;
    private TextView serviceText;
    private Switch rootSwitch;
    private final ExecutorService rootExecutor = Executors.newSingleThreadExecutor();
    private boolean changingRootSwitch;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateBatteryText(
                    intent.getIntExtra(ChargeMonitorService.EXTRA_LEVEL, -1),
                    intent.getBooleanExtra(ChargeMonitorService.EXTRA_PLUGGED, false));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        serviceText = findViewById(R.id.serviceText);
        rootSwitch = findViewById(R.id.rootSwitch);
        Button testButton = findViewById(R.id.testButton);
        Button settingsButton = findViewById(R.id.notificationSettingsButton);

        rootSwitch.setChecked(AppPrefs.isRootEnabled(this));
        rootSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!changingRootSwitch) {
                changeRootMode(checked);
            }
        });
        testButton.setOnClickListener(view -> testAlert());
        settingsButton.setOnClickListener(view -> openNotificationSettings());

        startMonitor();
        requestNotificationPermissionIfNeeded();
        readCurrentBattery();
    }

    private void startMonitor() {
        Intent intent = new Intent(this, ChargeMonitorService.class);
        startForegroundService(intent);
        serviceText.setText(R.string.service_running);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_REQUEST);
        }
    }

    private void testAlert() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (!manager.areNotificationsEnabled()) {
            Toast.makeText(this, R.string.notification_permission_needed, Toast.LENGTH_LONG).show();
            openNotificationSettings();
            return;
        }
        Intent intent = new Intent(this, ChargeMonitorService.class)
                .setAction(ChargeMonitorService.ACTION_TEST);
        startForegroundService(intent);
        Toast.makeText(this, R.string.test_sent, Toast.LENGTH_SHORT).show();
    }

    private void changeRootMode(boolean enable) {
        rootSwitch.setEnabled(false);
        serviceText.setText(enable ? R.string.root_requesting : R.string.root_disabling);
        rootExecutor.execute(() -> {
            boolean success;
            if (enable) {
                success = RootUtils.hasRoot() && RootUtils.enableKeepAlive(getPackageName());
            } else {
                success = RootUtils.disableKeepAlive(getPackageName());
            }
            boolean finalSuccess = success;
            runOnUiThread(() -> {
                rootSwitch.setEnabled(true);
                if (finalSuccess) {
                    AppPrefs.setRootEnabled(this, enable);
                    serviceText.setText(enable ? R.string.service_root_running : R.string.service_running);
                } else {
                    changingRootSwitch = true;
                    rootSwitch.setChecked(!enable);
                    changingRootSwitch = false;
                    serviceText.setText(R.string.root_failed);
                    Toast.makeText(this, R.string.root_failed_toast, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(intent);
    }

    private void readCurrentBattery() {
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) {
            return;
        }
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int percent = level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
        updateBatteryText(percent, plugged != 0);
    }

    private void updateBatteryText(int percent, boolean plugged) {
        if (percent < 0) {
            statusText.setText(R.string.battery_unavailable);
            return;
        }
        statusText.setText(getString(
                plugged ? R.string.battery_charging : R.string.battery_not_charging,
                percent));
    }

    @Override
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ChargeMonitorService.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, filter);
        }
    }

    @Override
    protected void onStop() {
        unregisterReceiver(stateReceiver);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        rootExecutor.shutdownNow();
        super.onDestroy();
    }
}
