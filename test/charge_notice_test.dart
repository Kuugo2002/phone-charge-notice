import 'package:charge_notice/charge_controller.dart';
import 'package:charge_notice/main.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  var mode = 'system';
  var color = 'green';
  var failSave = false;
  var notifications = true;

  setUp(() {
    mode = 'system';
    color = 'green';
    failSave = false;
    notifications = true;
    messenger.setMockMethodCallHandler(ChargeController.channel, (call) async {
      switch (call.method) {
        case 'initialize':
        case 'getState':
          return {
            'mode': mode,
            'color': color,
            'level': 76,
            'plugged': true,
            'rootEnabled': false,
            'notificationsEnabled': notifications,
          };
        case 'requestNotifications':
          return notifications;
        case 'saveAppearance':
          if (failSave) throw PlatformException(code: 'save_failed');
          final args = call.arguments as Map;
          mode = args['mode'] as String;
          color = args['color'] as String;
          return null;
      }
      return null;
    });
    messenger.setMockMethodCallHandler(
      const MethodChannel('charge_notice/battery'),
      (_) async => null,
    );
  });

  tearDown(() {
    messenger.setMockMethodCallHandler(ChargeController.channel, null);
    messenger.setMockMethodCallHandler(
      const MethodChannel('charge_notice/battery'),
      null,
    );
  });

  test(
    'appearance survives controller restart and failed saves roll back',
    () async {
      final first = ChargeController();
      await first.initialize();
      await first.setAppearance(mode: ThemeMode.dark, color: AppColor.purple);
      first.dispose();
      final restarted = ChargeController();
      await restarted.initialize();
      expect(restarted.mode, ThemeMode.dark);
      expect(restarted.color, AppColor.purple);
      failSave = true;
      await expectLater(
        restarted.setAppearance(mode: ThemeMode.light),
        throwsA(isA<PlatformException>()),
      );
      expect(restarted.mode, ThemeMode.dark);
      expect(restarted.appearanceBusy, false);
      notifications = false;
      await restarted.refresh();
      expect(restarted.notificationsEnabled, false);
      restarted.dispose();
    },
  );

  testWidgets(
    'settings switch colors and brightness, following system updates',
    (tester) async {
      final controller = ChargeController();
      await controller.initialize();
      await tester.pumpWidget(ChargeNoticeApp(controller: controller));
      await tester.tap(find.text('设置'));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(ChoiceChip, '深色'));
      await tester.pumpAndSettle();
      expect(
        Theme.of(tester.element(find.text('外观设置'))).brightness,
        Brightness.dark,
      );
      await tester.tap(find.widgetWithText(ChoiceChip, '晴空蓝'));
      await tester.pumpAndSettle();
      expect(controller.color, AppColor.blue);
      expect(color, 'blue');
      await tester.tap(find.widgetWithText(ChoiceChip, '浅色'));
      await tester.pumpAndSettle();
      expect(
        Theme.of(tester.element(find.text('外观设置'))).brightness,
        Brightness.light,
      );
      await tester.tap(find.widgetWithText(ChoiceChip, '跟随系统'));
      await tester.pumpAndSettle();
      tester.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
      await tester.pumpAndSettle();
      expect(
        Theme.of(tester.element(find.text('外观设置'))).brightness,
        Brightness.dark,
      );
      tester.platformDispatcher.clearPlatformBrightnessTestValue();
      await tester.pumpWidget(const SizedBox());
      controller.dispose();
    },
  );

  testWidgets(
    'small screen and large text remain scrollable without overflow',
    (tester) async {
      tester.view.physicalSize = const Size(320, 640);
      tester.view.devicePixelRatio = 1;
      tester.platformDispatcher.textScaleFactorTestValue = 1.6;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
        tester.platformDispatcher.clearTextScaleFactorTestValue();
      });
      final controller = ChargeController();
      await controller.initialize();
      await tester.pumpWidget(ChargeNoticeApp(controller: controller));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      await tester.tap(find.text('设置'));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
      controller.dispose();
    },
  );
}
