import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'charge_controller.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final controller = ChargeController();
  await controller.initialize();
  runApp(ChargeNoticeApp(controller: controller));
}

class ChargeNoticeApp extends StatelessWidget {
  const ChargeNoticeApp({super.key, required this.controller});
  final ChargeController controller;

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: controller,
    builder: (context, _) => MaterialApp(
      title: '充满提醒',
      debugShowCheckedModeBanner: false,
      themeMode: controller.mode,
      theme: _theme(Brightness.light),
      darkTheme: _theme(Brightness.dark),
      home: HomePage(controller: controller),
    ),
  );

  ThemeData _theme(Brightness brightness) {
    final scheme = ColorScheme.fromSeed(
      seedColor: controller.color.seed,
      brightness: brightness,
    );
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: scheme.surface,
      appBarTheme: AppBarTheme(
        backgroundColor: scheme.surface,
        centerTitle: false,
        systemOverlayStyle: SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: brightness == Brightness.dark
              ? Brightness.light
              : Brightness.dark,
          systemNavigationBarColor: scheme.surface,
          systemNavigationBarIconBrightness: brightness == Brightness.dark
              ? Brightness.light
              : Brightness.dark,
        ),
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        color: scheme.surfaceContainerLow,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      ),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key, required this.controller});
  final ChargeController controller;
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _page = 0;
  ChargeController get c => widget.controller;

  Future<void> _perform(
    Future<void> Function() action, [
    String? success,
  ]) async {
    try {
      await action();
      if (mounted && success != null) _message(success);
    } on PlatformException catch (e) {
      if (mounted) _message(e.message ?? '操作失败，请重试');
    } catch (_) {
      if (mounted) _message('操作失败，请重试');
    }
  }

  void _message(String text) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('充满提醒')),
    body: SafeArea(
      child: Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 640),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
            children: _page == 0 ? _monitor(context) : _settings(context),
          ),
        ),
      ),
    ),
    bottomNavigationBar: NavigationBar(
      selectedIndex: _page,
      onDestinationSelected: (value) => setState(() => _page = value),
      destinations: const [
        NavigationDestination(
          icon: Icon(Icons.battery_charging_full_rounded),
          label: '电量',
        ),
        NavigationDestination(
          icon: Icon(Icons.palette_outlined),
          selectedIcon: Icon(Icons.palette),
          label: '设置',
        ),
      ],
    ),
  );

  List<Widget> _monitor(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return [
      Text(
        '安心充电，充满即提醒',
        style: theme.textTheme.headlineSmall?.copyWith(
          fontWeight: FontWeight.bold,
        ),
      ),
      const SizedBox(height: 8),
      Text('让每一次充电，都恰到好处。', style: TextStyle(color: colors.onSurfaceVariant)),
      const SizedBox(height: 28),
      Card(
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(
            children: [
              SizedBox(
                width: 184,
                height: 184,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    SizedBox.expand(
                      child: CircularProgressIndicator(
                        value: c.level < 0 ? 0 : c.level.clamp(0, 100) / 100,
                        strokeWidth: 12,
                        strokeCap: StrokeCap.round,
                        backgroundColor: colors.surfaceContainerHighest,
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.all(18),
                      child: FittedBox(
                        fit: BoxFit.scaleDown,
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              c.plugged
                                  ? Icons.bolt_rounded
                                  : Icons.battery_std_rounded,
                              color: colors.primary,
                              size: 30,
                            ),
                            Text(
                              c.level < 0 ? '—' : '${c.level}%',
                              style: theme.textTheme.displayMedium?.copyWith(
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                            Text(
                              c.plugged
                                  ? (c.level >= 100 ? '已充满' : '正在充电')
                                  : '未连接充电器',
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 28),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    c.monitorRunning
                        ? Icons.shield_outlined
                        : Icons.info_outline,
                    size: 18,
                    color: colors.primary,
                  ),
                  const SizedBox(width: 8),
                  Flexible(
                    child: Text(c.monitorRunning ? '后台监控已开启' : '监控尚未启动'),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              const Text('电量达到 100% 时亮屏并发送通知', textAlign: TextAlign.center),
            ],
          ),
        ),
      ),
      const SizedBox(height: 20),
      if (c.error != null) ...[
        _notice(c.error!, Icons.error_outline),
        const SizedBox(height: 16),
      ],
      if (!c.notificationsEnabled) ...[
        Card(
          child: ListTile(
            leading: const Icon(Icons.notifications_off_outlined),
            title: const Text('通知权限未开启'),
            subtitle: const Text('开启后才能收到充满提醒'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => _perform(c.openNotifications),
          ),
        ),
        const SizedBox(height: 16),
      ],
      FilledButton.icon(
        onPressed: c.monitorRunning
            ? () => _perform(c.testAlert, '测试提醒已发送')
            : null,
        icon: const Icon(Icons.notifications_active_outlined),
        label: const Padding(padding: EdgeInsets.all(14), child: Text('测试提醒')),
      ),
      const SizedBox(height: 20),
      _notice(
        '同一次充电只提醒一次，拔下充电器后自动重置。部分手机需要在系统设置中允许自启动和锁屏通知。',
        Icons.info_outline,
      ),
    ];
  }

  List<Widget> _settings(BuildContext context) => [
    Text(
      '外观设置',
      style: Theme.of(
        context,
      ).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
    ),
    const SizedBox(height: 8),
    const Text('选择喜欢的颜色，让界面适应你的习惯。'),
    const SizedBox(height: 24),
    Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('主题颜色', style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: AppColor.values
                  .map(
                    (color) => ChoiceChip(
                      avatar: CircleAvatar(
                        backgroundColor: color.seed,
                        radius: 9,
                      ),
                      label: Text(color.label),
                      selected: c.color == color,
                      onSelected: c.appearanceBusy
                          ? null
                          : (_) =>
                                _perform(() => c.setAppearance(color: color)),
                    ),
                  )
                  .toList(),
            ),
          ],
        ),
      ),
    ),
    const SizedBox(height: 16),
    Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('暗黑模式', style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _modeChip(
                  ThemeMode.system,
                  '跟随系统',
                  Icons.brightness_auto_outlined,
                ),
                _modeChip(ThemeMode.light, '浅色', Icons.light_mode_outlined),
                _modeChip(ThemeMode.dark, '深色', Icons.dark_mode_outlined),
              ],
            ),
            const SizedBox(height: 12),
            const Text('设置即时生效，并在下次打开时保留。'),
          ],
        ),
      ),
    ),
    const SizedBox(height: 28),
    Text('提醒设置', style: Theme.of(context).textTheme.titleLarge),
    const SizedBox(height: 16),
    Card(
      child: Column(
        children: [
          ListTile(
            leading: const Icon(Icons.notifications_outlined),
            title: const Text('系统通知设置'),
            subtitle: Text(c.notificationsEnabled ? '通知已允许' : '通知未开启'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => _perform(c.openNotifications),
          ),
          const Divider(height: 1, indent: 16, endIndent: 16),
          SwitchListTile(
            secondary: const Icon(Icons.admin_panel_settings_outlined),
            title: const Text('Root 增强'),
            subtitle: Text(
              c.rootBusy ? '正在处理 Root 授权…' : '需设备已 Root，用于增强后台保活和亮屏',
            ),
            value: c.rootEnabled,
            onChanged: c.rootBusy
                ? null
                : (enabled) => _perform(() => c.setRoot(enabled)),
          ),
        ],
      ),
    ),
  ];

  Widget _modeChip(ThemeMode mode, String label, IconData icon) => ChoiceChip(
    avatar: Icon(icon, size: 18),
    label: Text(label),
    selected: c.mode == mode,
    onSelected: c.appearanceBusy
        ? null
        : (_) => _perform(() => c.setAppearance(mode: mode)),
  );

  Widget _notice(String text, IconData icon) => Row(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Icon(
        icon,
        size: 18,
        color: Theme.of(context).colorScheme.onSurfaceVariant,
      ),
      const SizedBox(width: 10),
      Expanded(
        child: Text(
          text,
          style: TextStyle(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
            height: 1.6,
          ),
        ),
      ),
    ],
  );
}
