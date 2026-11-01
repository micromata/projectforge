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

import '../../core/api/api_exception.dart';
import '../../core/i18n/translations.dart';
import '../../core/widgets/error_view.dart';
import 'business_card.dart';
import 'business_card_qr_screen.dart';
import 'business_cards_controller.dart';
import 'vcard_share.dart';

/// The own business cards: tap shows the QR code to scan, the menu shares a card. Cards are created and edited in
/// the web pages in the app, which the link at the end opens ([onCreate]).
class BusinessCardsScreen extends ConsumerWidget {
  const BusinessCardsScreen({super.key, required this.onCreate});

  /// Shows the page for creating and editing cards.
  final VoidCallback onCreate;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cards = ref.watch(businessCardsProvider);
    return Scaffold(
      body: cards.when(
        skipLoadingOnRefresh: true,
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          message: error.toString(),
          onRetry: () => ref.read(businessCardsProvider.notifier).refresh(),
        ),
        data: (data) => RefreshIndicator(
          onRefresh: () => ref.read(businessCardsProvider.notifier).refresh(),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 24),
            children: [
              if (data.fromCache)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Text(
                    context.t('app.offline'),
                    style: TextStyle(
                      color: Theme.of(context).colorScheme.error,
                    ),
                  ),
                ),
              if (data.cards.isEmpty)
                Padding(
                  padding: const EdgeInsets.all(24),
                  child: Text(
                    context.t('businessCard.none'),
                    textAlign: TextAlign.center,
                  ),
                ),
              for (final info in data.cards)
                _CardTile(info: info, offline: data.fromCache),
              if (!data.fromCache) _CreateOnDesktopLink(onPressed: onCreate),
            ],
          ),
        ),
      ),
    );
  }
}

class _CardTile extends ConsumerWidget {
  const _CardTile({required this.info, required this.offline});

  final BusinessCardInfo info;
  final bool offline;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final card = info.card;
    final png = info.preview.qrCodePng;
    return Card(
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
        leading: png == null
            ? const Icon(Icons.qr_code_2, size: 48)
            : Image.memory(
                png,
                width: 56,
                height: 56,
                filterQuality: FilterQuality.none,
              ),
        title: Text(card.cardName ?? card.displayName),
        subtitle: Text(
          [
            card.displayName,
            card['organization'],
          ].whereType<String>().where((it) => it.isNotEmpty).join('\n'),
        ),
        onTap: () => Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => BusinessCardQrScreen(info: info, offline: offline),
          ),
        ),
        trailing: offline
            ? null
            : PopupMenuButton<String>(
                onSelected: (action) => _onAction(context, ref, action),
                itemBuilder: (_) => [
                  PopupMenuItem(
                    value: 'share',
                    child: Text(context.t('app.vCard.share')),
                  ),
                ],
              ),
      ),
    );
  }

  Future<void> _onAction(
    BuildContext context,
    WidgetRef ref,
    String action,
  ) async {
    final messenger = ScaffoldMessenger.of(context);
    try {
      switch (action) {
        case 'share':
          final repository = ref.read(businessCardsRepositoryProvider);
          if (repository != null) await shareVCard(repository, info.card);
      }
    } on ApiException catch (e) {
      messenger.showSnackBar(SnackBar(content: Text(e.message)));
    }
  }
}

/// Cards are created in the web version: the link shows its page in the web tab.
class _CreateOnDesktopLink extends StatelessWidget {
  const _CreateOnDesktopLink({required this.onPressed});

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 8),
    child: TextButton.icon(
      icon: const Icon(Icons.edit_note),
      label: Text(context.t('app.businessCard.createOnDesktop')),
      onPressed: onPressed,
    ),
  );
}
