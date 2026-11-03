/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/auth/app_version.dart';
import '../../core/auth/auth_controller.dart';
import '../../core/auth/session.dart';
import '../../core/compat/compatibility.dart';
import '../../core/i18n/translations.dart';
import '../../core/push/push_service.dart';
import '../../core/settings/app_settings.dart';
import '../../core/status/user_status_controller.dart';
import '../../core/web/native_page.dart';
import '../addresses/addresses_screen.dart';
import '../business_cards/business_cards_screen.dart';
import '../notifications/notification_bell.dart';
import '../notifications/notification_text.dart';
import '../notifications/notifications_screen.dart';
import '../settings/settings_screen.dart';
import 'server_switch.dart';
import 'system_alert_banner.dart';
import '../timesheets/timesheets_screen.dart';
import '../web/web_screen.dart';

/// The tabs of the logged-in app.
enum _Destination { businessCards, addresses, timesheets, chat, web }

/// The start page of the logged-in app: the business cards, the addresses, the time sheets, the chat and the
/// ProjectForge pages. The chat is the chat of next in a WebView (one implementation for web and app), the app adds
/// the badge of the unread messages.
class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  static const _chatPath = '/next/chat';

  _Destination _current = _Destination.businessCards;

  /// The page the business cards ask the web tab to show (their creation and editing, a link of a notification).
  WebTarget? _webTarget;

  /// The page of the chat tab other than its start page (a link of a notification).
  WebTarget? _chatTarget;

  /// The WebViews are created when their tab is opened first, and then kept (with their page) while switching.
  final _opened = <_Destination>{};

  /// The restarts of the WebView tabs asked for in the account menu (see [WebScreen.restarts]).
  final _restarts = <_Destination, int>{};

  /// Keeps the tabs (the WebViews with their pages) while the frame around them changes: with or without the bar of
  /// the native tab, portrait or landscape. Otherwise they would be created again, back on their start page.
  final _bodyKey = GlobalKey();

  /// The compatibility warning shown last (server and issue): once per server and app start, not on every poll.
  String? _shownIssue;

  @override
  void initState() {
    super.initState();
    // Loaded before the web pages ask for a native tab (see _openNative).
    ref.read(appSettingsProvider);
    // Asks for the permission to show pushes, registers and hands the token to the server.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(pushServiceProvider)?.start().catchError((Object _) {});
    });
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(authControllerProvider).session;
    ref.listen(pushLinkProvider, (_, link) {
      if (link == null) return;
      ref.read(pushLinkProvider.notifier).clear();
      _openLink(link);
    });
    // The number on the icon follows the notifications.
    ref.listen(userStatusProvider.select((s) => s.value?.openNotifications), (
      _,
      count,
    ) {
      if (count != null) ref.read(pushServiceProvider)?.setBadge(count);
    });
    ref.listen(compatibilityIssueProvider, (_, issue) {
      _showCompatibilityIssue(session, issue);
    });
    final issue = ref.watch(compatibilityIssueProvider);
    final userStatus = ref.watch(userStatusProvider).value;
    // Unknown (not loaded yet, offline): shown, the server refuses it anyway if it is switched off.
    final chatEnabled = userStatus?.chatEnabled ?? true;
    final destinations = [
      _Destination.businessCards,
      _Destination.addresses,
      if (timesheetsTabEnabled) _Destination.timesheets,
      if (chatEnabled) _Destination.chat,
      _Destination.web,
    ];
    final current = destinations.contains(_current)
        ? _current
        : _Destination.businessCards;
    final unread = userStatus?.chatUnread ?? 0;
    final landscape =
        MediaQuery.orientationOf(context) == Orientation.landscape;
    final selectedIndex = destinations.indexOf(current);
    // The pages of the web tab show the message themselves, the chat page doesn't.
    final showAlert = current != _Destination.web;
    final body = IndexedStack(
      key: _bodyKey,
      index: selectedIndex,
      children: [
        for (final destination in destinations)
          switch (destination) {
            _Destination.businessCards => BusinessCardsScreen(
              onCreate: _openBusinessCardsPage,
            ),
            _Destination.addresses => AddressesScreen(
              active: current == destination,
              onEdit: (id) => _openWebPage('/next/address/$id/edit'),
              onCreate: () => _openWebPage('/next/address/new/edit'),
            ),
            _Destination.timesheets => TimesheetsScreen(
              active: current == destination,
            ),
            _Destination.chat =>
              _opened.contains(destination)
                  ? WebScreen(
                      key: const ValueKey(_Destination.chat),
                      active: current == destination,
                      initialPath: _chatTarget?.path ?? _chatPath,
                      target: _chatTarget,
                      restarts: _restarts[destination] ?? 0,
                    )
                  : const SizedBox.shrink(),
            _Destination.web =>
              _opened.contains(destination)
                  ? WebScreen(
                      key: const ValueKey(_Destination.web),
                      active: current == destination,
                      initialPath: _webTarget?.path,
                      target: _webTarget,
                      onNativePage: (page) => _openNative(page, chatEnabled),
                      restarts: _restarts[destination] ?? 0,
                    )
                  : const SizedBox.shrink(),
          },
      ],
    );
    void select(int index) {
      final destination = destinations[index];
      if (destination == _Destination.chat && current == destination) {
        // Tapping the chat again leads back to its start page (the rooms), wherever the chat has got to.
        setState(() => _chatTarget = WebTarget(_chatPath));
        return;
      }
      _select(current, destination);
    }

    // The pages bring their own header: only the native tab has a bar. The bell and the account menu stand in the
    // navigation instead, so they are there on every tab.
    final appBar = switch (current) {
      _Destination.businessCards ||
      _Destination.addresses ||
      _Destination.timesheets => AppBar(
        title: Text(_label(context, current)),
        actions: [
          if (current == _Destination.businessCards)
            IconButton(
              tooltip: context.t('refresh'),
              icon: const Icon(Icons.refresh),
              onPressed: () => refreshBusinessCards(context, ref),
            ),
        ],
      ),
      _ => null,
    };
    final actions = [
      NotificationBell(onOpenLink: _openLink),
      _accountMenu(context, session, current, issue),
    ];
    if (landscape) {
      // The height is scarce: the navigation stands at the side.
      return Scaffold(
        appBar: appBar,
        body: Row(
          children: [
            // Icons only, inside the safe area (clear of the camera cutout and the home indicator).
            SafeArea(
              right: false,
              child: NavigationRail(
                selectedIndex: selectedIndex,
                onDestinationSelected: select,
                labelType: NavigationRailLabelType.none,
                minWidth: 56,
                trailing: Column(children: actions),
                destinations: [
                  for (final destination in destinations)
                    _railDestination(context, destination, unread),
                ],
              ),
            ),
            Expanded(
              child: SafeArea(
                left: false,
                top: appBar == null,
                child: Column(
                  children: [
                    if (showAlert) const SystemAlertBanner(),
                    Expanded(child: body),
                  ],
                ),
              ),
            ),
          ],
        ),
      );
    }
    return Scaffold(
      appBar: appBar,
      body: _withAlert(appBar == null, showAlert, body),
      // Slim: icons only (the label is their semantics and tooltip), the full inset of the home indicator below.
      bottomNavigationBar: ColoredBox(
        color: Theme.of(context).colorScheme.surfaceContainer,
        child: Padding(
          padding: EdgeInsets.only(
            bottom: MediaQuery.paddingOf(context).bottom,
          ),
          child: MediaQuery.removePadding(
            // The context is the one outside of the Scaffold: its top inset (the status bar) has to go too.
            context: context,
            removeTop: true,
            removeBottom: true,
            child: NavigationBarTheme(
              data: const NavigationBarThemeData(
                height: 44,
                labelBehavior: NavigationDestinationLabelBehavior.alwaysHide,
              ),
              // A fixed height: the Centers of the actions would otherwise take all the height the Scaffold offers.
              child: SizedBox(
                height: 44,
                child: Row(
                  children: [
                    // Every icon gets the same share of the width: the bar's destinations and one slot per action.
                    Expanded(
                      flex: destinations.length,
                      child: NavigationBar(
                        selectedIndex: selectedIndex,
                        onDestinationSelected: select,
                        destinations: [
                          for (final destination in destinations)
                            _barDestination(context, destination, unread),
                        ],
                      ),
                    ),
                    for (final action in actions)
                      Expanded(child: Center(child: action)),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  /// The message of the administrators above the page; clear of the status bar if there is no app bar.
  Widget _withAlert(bool safeTop, bool showAlert, Widget body) {
    final column = Column(
      children: [
        if (showAlert) const SystemAlertBanner(),
        Expanded(child: body),
      ],
    );
    return safeTop ? SafeArea(bottom: false, child: column) : column;
  }

  /// @param current The tab shown: a WebView tab offers a reload, the way out of pages that stay empty.
  Widget _accountMenu(
    BuildContext context,
    Session? session,
    _Destination current,
    CompatibilityIssue? issue,
  ) {
    final version = ref.watch(appVersionProvider).value;
    final webTab = current == _Destination.web || current == _Destination.chat;
    return PopupMenuButton<String>(
      tooltip: context.t('settings'),
      // An incompatibility is visible without opening the menu: red gear with an exclamation mark.
      icon: issue == null
          ? const Icon(Icons.settings_outlined)
          : Badge(
              label: const Text('!'),
              backgroundColor: Theme.of(context).colorScheme.error,
              child: Icon(
                Icons.settings_outlined,
                color: Theme.of(context).colorScheme.error,
              ),
            ),
      onSelected: (action) => switch (action) {
        'reload' => setState(
          () => _restarts[current] = (_restarts[current] ?? 0) + 1,
        ),
        'compatibility' => null, // Only to read.
        'server' => showServerSwitch(context, ref),
        'settings' => Navigator.of(
          context,
        ).push(MaterialPageRoute<void>(builder: (_) => const SettingsScreen())),
        _ => _logout(context, ref),
      },
      itemBuilder: (_) => [
        PopupMenuItem<String>(
          enabled: false,
          child: Text(
            '${session?.fullname ?? session?.username ?? ''}\n${session?.serverUrl ?? ''}',
          ),
        ),
        if (issue != null)
          // Not `enabled: false`: that greys the text out, and a warning has to stand out. Long text, so it wraps.
          PopupMenuItem<String>(
            value: 'compatibility',
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 280),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(
                    Icons.warning_amber,
                    color: Theme.of(context).colorScheme.error,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      _compatibilityText(context, issue),
                      softWrap: true,
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.error,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        if (webTab)
          PopupMenuItem(
            value: 'reload',
            child: Text(context.t('app.web.reload')),
          ),
        PopupMenuItem(
          value: 'server',
          child: Text(context.t('app.server.switch')),
        ),
        PopupMenuItem(value: 'settings', child: Text(context.t('settings'))),
        PopupMenuItem(value: 'logout', child: Text(context.t('menu.logout'))),
        if (version != null)
          PopupMenuItem<String>(
            enabled: false,
            child: Text(context.t('app.version', [version])),
          ),
      ],
    );
  }

  static String _compatibilityText(
    BuildContext context,
    CompatibilityIssue issue,
  ) => switch (issue) {
    AppTooOld() => context.t('app.compatibility.appTooOld', [issue.minVersion]),
    ServerTooOld() => context.t('app.compatibility.serverTooOld', [
      issue.minVersion,
    ]),
  };

  /// Shows [issue] as a banner over the tabs, once per server and app start; the menu keeps showing it.
  void _showCompatibilityIssue(Session? session, CompatibilityIssue? issue) {
    final messenger = ScaffoldMessenger.of(context);
    if (issue == null) {
      messenger.hideCurrentMaterialBanner();
      return;
    }
    final key =
        '${session?.serverUrl}#${issue.runtimeType}#${issue.minVersion}';
    if (key == _shownIssue) return;
    _shownIssue = key;
    messenger.showMaterialBanner(
      MaterialBanner(
        leading: const Icon(Icons.warning_amber),
        content: Text(_compatibilityText(context, issue)),
        actions: [
          TextButton(
            onPressed: messenger.hideCurrentMaterialBanner,
            child: Text(context.t('ok')),
          ),
        ],
      ),
    );
  }

  static String _label(BuildContext context, _Destination destination) =>
      switch (destination) {
        _Destination.businessCards => context.t('menu.myBusinessCards'),
        _Destination.addresses => context.t('menu.addressList'),
        _Destination.timesheets => context.t('timesheet.title.heading'),
        _Destination.chat => context.t('chat.title'),
        _Destination.web => context.t('app.web'),
      };

  static Widget _icon(_Destination destination, int unread, bool selected) =>
      switch (destination) {
        _Destination.businessCards => Icon(
          selected ? Icons.contact_page : Icons.contact_page_outlined,
        ),
        _Destination.addresses => Icon(
          selected ? Icons.contacts : Icons.contacts_outlined,
        ),
        _Destination.timesheets => Icon(
          selected ? Icons.schedule : Icons.schedule_outlined,
        ),
        _Destination.chat => Badge(
          label: Text('$unread'),
          isLabelVisible: unread > 0,
          child: Icon(selected ? Icons.chat_bubble : Icons.chat_bubble_outline),
        ),
        _Destination.web => Icon(
          selected ? Icons.public : Icons.public_outlined,
        ),
      };

  static NavigationDestination _barDestination(
    BuildContext context,
    _Destination destination,
    int unread,
  ) => NavigationDestination(
    icon: _icon(destination, unread, false),
    selectedIcon: _icon(destination, unread, true),
    label: _label(context, destination),
  );

  static NavigationRailDestination _railDestination(
    BuildContext context,
    _Destination destination,
    int unread,
  ) => NavigationRailDestination(
    icon: _icon(destination, unread, false),
    selectedIcon: _icon(destination, unread, true),
    label: Text(_label(context, destination)),
  );

  /// Shows the page of a link of a notification (see [webPathOfLink]): the chat in its tab, everything else in the web tab.
  void _openLink(String link) {
    final path = webPathOfLink(link);
    if (path == null) return;
    if (isChatPath(path)) {
      _chatTarget = WebTarget(path);
      _select(_current, _Destination.chat);
    } else {
      _webTarget = WebTarget(path);
      _select(_current, _Destination.web);
    }
  }

  /// The web tab navigates to a page with a native tab: that tab is shown instead (the chat only if enabled), unless
  /// the settings keep the page in the web version.
  bool _openNative(NativePage page, bool chatEnabled) {
    final settings = ref.read(appSettingsProvider).value ?? const AppSettings();
    if (!settings.opensInApp(page)) return false;
    if (page == NativePage.timesheets && !timesheetsTabEnabled) return false;
    final destination = switch (page) {
      NativePage.chat => chatEnabled ? _Destination.chat : null,
      NativePage.businessCards => _Destination.businessCards,
      NativePage.addresses => _Destination.addresses,
      NativePage.timesheets => _Destination.timesheets,
      NativePage.notifications => null,
    };
    if (page == NativePage.notifications) {
      // No tab: the list lies over the tabs, like the bell of the business cards tab opens it.
      Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => NotificationsScreen(onOpenLink: _openLink),
        ),
      );
      return true;
    }
    if (destination == null) return false;
    _select(_current, destination);
    return true;
  }

  void _openBusinessCardsPage() => _openWebPage('/next/myBusinessCards');

  /// Shows a page of the server in the web tab (editing of the native tabs happens there).
  void _openWebPage(String path) {
    _webTarget = WebTarget(path);
    _select(_current, _Destination.web);
  }

  void _select(_Destination previous, _Destination destination) {
    setState(() {
      _current = destination;
      _opened.add(destination);
    });
    // Reading in the chat (or in the pages) changes the count: no need to wait for the next poll.
    if (previous != destination) {
      ref.read(userStatusProvider.notifier).refresh();
    }
  }

  Future<void> _logout(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        content: Text(context.t('app.logout.question')),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text(context.t('cancel')),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text(context.t('menu.logout')),
          ),
        ],
      ),
    );
    if (confirmed == true) {
      await ref.read(authControllerProvider.notifier).logout();
    }
  }
}
