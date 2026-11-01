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
import '../web/web_screen.dart';

/// The start page of the logged-in app: the business cards and the ProjectForge pages (WebView). Timesheets and chat
/// follow as further destinations.
class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  static const _webTab = 1;

  int _tab = 0;

  /// The WebView is created when its tab is opened first, and then kept (with its page) while switching.
  bool _webOpened = false;

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(authControllerProvider).session;
    return Scaffold(
      appBar: AppBar(
        title: Text(
          _tab == _webTab
              ? context.t('app.web')
              : context.t('menu.myBusinessCards'),
        ),
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
        index: _tab,
        children: [
          const BusinessCardsScreen(),
          _webOpened
              ? WebScreen(active: _tab == _webTab)
              : const SizedBox.shrink(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (index) => setState(() {
          _tab = index;
          _webOpened |= index == _webTab;
        }),
        destinations: [
          NavigationDestination(
            icon: const Icon(Icons.contact_page_outlined),
            selectedIcon: const Icon(Icons.contact_page),
            label: context.t('menu.myBusinessCards'),
          ),
          NavigationDestination(
            icon: const Icon(Icons.public_outlined),
            selectedIcon: const Icon(Icons.public),
            label: context.t('app.web'),
          ),
        ],
      ),
    );
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
