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

import '../../core/auth/auth_controller.dart';
import '../../core/auth/session.dart';
import '../../core/i18n/translations.dart';
import '../business_cards/business_cards_screen.dart';
import '../chat/chat_status_controller.dart';
import '../web/web_screen.dart';

/// The tabs of the logged-in app.
enum _Destination { businessCards, chat, web }

/// The start page of the logged-in app: the business cards, the chat and the ProjectForge pages. The chat is the
/// chat of next in a WebView (one implementation for web and app), the app adds the badge of the unread messages.
/// Timesheets follow as a further destination.
class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  static const _chatPath = '/next/chat';

  _Destination _current = _Destination.businessCards;

  /// The page the business cards ask the web tab to show (their creation and editing).
  WebTarget? _webTarget;

  /// The WebViews are created when their tab is opened first, and then kept (with their page) while switching.
  final _opened = <_Destination>{};

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(authControllerProvider).session;
    final chatStatus = ref.watch(chatStatusProvider).value;
    // Unknown (not loaded yet, offline): shown, the server refuses it anyway if it is switched off.
    final chatEnabled = chatStatus?.enabled ?? true;
    final destinations = [
      _Destination.businessCards,
      if (chatEnabled) _Destination.chat,
      _Destination.web,
    ];
    final current = destinations.contains(_current)
        ? _current
        : _Destination.businessCards;
    final unread = chatStatus?.unread ?? 0;
    final landscape =
        MediaQuery.orientationOf(context) == Orientation.landscape;
    final selectedIndex = destinations.indexOf(current);
    final body = IndexedStack(
      index: selectedIndex,
      children: [
        for (final destination in destinations)
          switch (destination) {
            _Destination.businessCards => BusinessCardsScreen(
              onCreate: _openBusinessCardsPage,
            ),
            _Destination.chat =>
              _opened.contains(destination)
                  ? WebScreen(
                      key: const ValueKey(_Destination.chat),
                      active: current == destination,
                      initialPath: _chatPath,
                    )
                  : const SizedBox.shrink(),
            _Destination.web =>
              _opened.contains(destination)
                  ? WebScreen(
                      key: const ValueKey(_Destination.web),
                      active: current == destination,
                      initialPath: _webTarget?.path,
                      target: _webTarget,
                    )
                  : const SizedBox.shrink(),
          },
      ],
    );
    void select(int index) => _select(current, destinations[index]);
    // The pages bring their own header: only the native tab has a bar (with the account menu).
    final appBar = current == _Destination.businessCards
        ? AppBar(
            title: Text(context.t('menu.myBusinessCards')),
            actions: [
              IconButton(
                tooltip: context.t('refresh'),
                icon: const Icon(Icons.refresh),
                onPressed: () => refreshBusinessCards(context, ref),
              ),
              _accountMenu(context, session),
            ],
          )
        : null;
    if (landscape) {
      // The height is scarce: the navigation stands at the side.
      return Scaffold(
        appBar: appBar,
        body: Row(
          children: [
            SafeArea(
              right: false,
              child: NavigationRail(
                selectedIndex: selectedIndex,
                onDestinationSelected: select,
                labelType: NavigationRailLabelType.selected,
                minWidth: 64,
                destinations: [
                  for (final destination in destinations)
                    _railDestination(context, destination, unread),
                ],
              ),
            ),
            Expanded(
              child: SafeArea(left: false, top: appBar == null, child: body),
            ),
          ],
        ),
      );
    }
    return Scaffold(
      appBar: appBar,
      body: appBar == null ? SafeArea(bottom: false, child: body) : body,
      bottomNavigationBar: NavigationBarTheme(
        data: const NavigationBarThemeData(
          height: 56,
          labelBehavior: NavigationDestinationLabelBehavior.onlyShowSelected,
        ),
        child: NavigationBar(
          selectedIndex: selectedIndex,
          onDestinationSelected: select,
          destinations: [
            for (final destination in destinations)
              _barDestination(context, destination, unread),
          ],
        ),
      ),
    );
  }

  Widget _accountMenu(
    BuildContext context,
    Session? session,
  ) => PopupMenuButton<String>(
    icon: const Icon(Icons.account_circle),
    onSelected: (_) => _logout(context, ref),
    itemBuilder: (_) => [
      PopupMenuItem<String>(
        enabled: false,
        child: Text(
          '${session?.fullname ?? session?.username ?? ''}\n${session?.serverUrl ?? ''}',
        ),
      ),
      PopupMenuItem(value: 'logout', child: Text(context.t('menu.logout'))),
    ],
  );

  static String _label(BuildContext context, _Destination destination) =>
      switch (destination) {
        _Destination.businessCards => context.t('menu.myBusinessCards'),
        _Destination.chat => context.t('chat.title'),
        _Destination.web => context.t('app.web'),
      };

  static Widget _icon(_Destination destination, int unread, bool selected) =>
      switch (destination) {
        _Destination.businessCards => Icon(
          selected ? Icons.contact_page : Icons.contact_page_outlined,
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

  void _openBusinessCardsPage() {
    _webTarget = WebTarget('/next/myBusinessCards');
    _select(_current, _Destination.web);
  }

  void _select(_Destination previous, _Destination destination) {
    setState(() {
      _current = destination;
      _opened.add(destination);
    });
    // Reading in the chat (or in the pages) changes the count: no need to wait for the next poll.
    if (previous != destination) {
      ref.read(chatStatusProvider.notifier).refresh();
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
