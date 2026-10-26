import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

enum AppColor {
  green('清新绿', Color(0xFF218565)),
  blue('晴空蓝', Color(0xFF3568C0)),
  purple('鸢尾紫', Color(0xFF7955AF)),
  orange('暖阳橙', Color(0xFFB8632D));

  const AppColor(this.label, this.seed);
  final String label;
  final Color seed;
}

class ChargeController extends ChangeNotifier with WidgetsBindingObserver {
  static const channel = MethodChannel('charge_notice/control');
  static const events = EventChannel('charge_notice/battery');
  ThemeMode mode = ThemeMode.system;
  AppColor color = AppColor.green;
  int level = -1;
  bool plugged = false;
  bool notificationsEnabled = false;
  bool rootEnabled = false;
  bool monitorRunning = false;
  bool rootBusy = false;
  bool appearanceBusy = false;
  String? error;
  StreamSubscription<dynamic>? _batterySubscription;
  bool _disposed = false;

  Future<void> initialize() async {
    WidgetsBinding.instance.addObserver(this);
    try {
      _read(await channel.invokeMapMethod<String, dynamic>('initialize'));
      monitorRunning = true;
      _batterySubscription = events.receiveBroadcastStream().listen(
        (event) {
          _read(Map<String, dynamic>.from(event as Map));
          _notify();
        },
        onError: (Object e) {
          error = '电量状态读取失败，请重新打开应用';
          _notify();
        },
      );
      notificationsEnabled =
          await channel.invokeMethod<bool>('requestNotifications') ?? false;
    } on PlatformException catch (e) {
      error = e.message ?? '初始化失败，请重试';
    } on MissingPluginException {
      error = '请在 Android 设备上运行此应用';
    }
    _notify();
  }

  void _read(Map<String, dynamic>? state) {
    if (state == null) return;
    level = state['level'] as int? ?? level;
    plugged = state['plugged'] as bool? ?? plugged;
    notificationsEnabled =
        state['notificationsEnabled'] as bool? ?? notificationsEnabled;
    rootEnabled = state['rootEnabled'] as bool? ?? rootEnabled;
    mode = ThemeMode.values.firstWhere(
      (m) => m.name == state['mode'],
      orElse: () => mode,
    );
    color = AppColor.values.firstWhere(
      (c) => c.name == state['color'],
      orElse: () => color,
    );
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) unawaited(refresh());
  }

  Future<void> refresh() async {
    try {
      _read(await channel.invokeMapMethod<String, dynamic>('getState'));
    } on PlatformException catch (e) {
      error = e.message;
    } on MissingPluginException {
      error = '请在 Android 设备上运行此应用';
    }
    _notify();
  }

  Future<void> setAppearance({ThemeMode? mode, AppColor? color}) async {
    if (appearanceBusy) return;
    final oldMode = this.mode;
    final oldColor = this.color;
    this.mode = mode ?? this.mode;
    this.color = color ?? this.color;
    appearanceBusy = true;
    _notify();
    try {
      await channel.invokeMethod<void>('saveAppearance', {
        'mode': this.mode.name,
        'color': this.color.name,
      });
    } catch (_) {
      this.mode = oldMode;
      this.color = oldColor;
      rethrow;
    } finally {
      appearanceBusy = false;
      _notify();
    }
  }

  Future<void> setRoot(bool enabled) async {
    if (rootBusy) return;
    rootBusy = true;
    _notify();
    try {
      rootEnabled =
          await channel.invokeMethod<bool>('setRootEnabled', {
            'enabled': enabled,
          }) ??
          rootEnabled;
    } finally {
      rootBusy = false;
      _notify();
    }
  }

  Future<void> testAlert() => channel.invokeMethod<void>('testAlert');
  Future<void> openNotifications() =>
      channel.invokeMethod<void>('openNotificationSettings');

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    WidgetsBinding.instance.removeObserver(this);
    unawaited(_batterySubscription?.cancel());
    super.dispose();
  }
}
