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
    return Scaffold(
      appBar: AppBar(
        title: Text(switch (current) {
          _Destination.businessCards => context.t('menu.myBusinessCards'),
          _Destination.chat => context.t('chat.title'),
          _Destination.web => context.t('app.web'),
        }),
        actions: [
          PopupMenuButton<String>(
            icon: const Icon(Icons.account_circle),
            onSelected: (_) => _logout(context, ref),
            itemBuilder: (_) => [
              PopupMenuItem<String>(
                enabled: false,
                child: Text(
                  '${session?.fullname ?? session?.username ?? ''}\n${session?.serverUrl ?? ''}',
                ),
              ),
              PopupMenuItem(
                value: 'logout',
                child: Text(context.t('menu.logout')),
              ),
            ],
          ),
        ],
      ),
      body: IndexedStack(
        index: destinations.indexOf(current),
        children: [
          for (final destination in destinations)
            switch (destination) {
              _Destination.businessCards => const BusinessCardsScreen(),
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
                      )
                    : const SizedBox.shrink(),
            },
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: destinations.indexOf(current),
        onDestinationSelected: (index) => _select(current, destinations[index]),
        destinations: [
          for (final destination in destinations)
            switch (destination) {
              _Destination.businessCards => NavigationDestination(
                icon: const Icon(Icons.contact_page_outlined),
                selectedIcon: const Icon(Icons.contact_page),
                label: context.t('menu.myBusinessCards'),
              ),
              _Destination.chat => NavigationDestination(
                icon: Badge(
                  label: Text('$unread'),
                  isLabelVisible: unread > 0,
                  child: const Icon(Icons.chat_bubble_outline),
                ),
                selectedIcon: Badge(
                  label: Text('$unread'),
                  isLabelVisible: unread > 0,
                  child: const Icon(Icons.chat_bubble),
                ),
                label: context.t('chat.title'),
              ),
              _Destination.web => NavigationDestination(
                icon: const Icon(Icons.public_outlined),
                selectedIcon: const Icon(Icons.public),
                label: context.t('app.web'),
              ),
            },
        ],
      ),
    );
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
