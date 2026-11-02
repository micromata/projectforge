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
import 'package:screen_brightness/screen_brightness.dart';

import '../../core/api/api_exception.dart';
import '../../core/i18n/translations.dart';
import '../../core/wallet/wallet_pass.dart';
import 'business_card.dart';
import 'business_cards_controller.dart';
import 'business_cards_repository.dart';
import 'vcard_share.dart';
import 'wallet_pass.dart';

/// The QR code of a card in full size, for scanning by somebody else: the screen is set to full brightness while
/// it is shown. Works offline (the QR code is part of the cached list).
class BusinessCardQrScreen extends ConsumerStatefulWidget {
  const BusinessCardQrScreen({
    super.key,
    required this.info,
    this.offline = false,
    this.walletConfigured = false,
  });

  final BusinessCardInfo info;
  final bool offline;
  final bool walletConfigured;

  @override
  ConsumerState<BusinessCardQrScreen> createState() =>
      _BusinessCardQrScreenState();
}

class _BusinessCardQrScreenState extends ConsumerState<BusinessCardQrScreen> {
  @override
  void initState() {
    super.initState();
    ScreenBrightness.instance.setApplicationScreenBrightness(1.0).ignore();
  }

  @override
  void dispose() {
    ScreenBrightness.instance.resetApplicationScreenBrightness().ignore();
    super.dispose();
  }

  Future<void> _addToWallet(
    BusinessCardsRepository repository,
    BusinessCard card,
  ) async {
    final messenger = ScaffoldMessenger.of(context);
    try {
      await addToWallet(repository, card);
    } on ApiException catch (e) {
      if (!mounted) return;
      messenger.showSnackBar(SnackBar(content: Text(context.errorText(e))));
    } on WalletException catch (e) {
      messenger.showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final card = widget.info.card;
    final png = widget.info.preview.qrCodePng;
    final repository = ref.watch(businessCardsRepositoryProvider);
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        title: Text(card.cardName ?? ''),
        actions: [
          if (!widget.offline &&
              repository != null &&
              widget.walletConfigured &&
              walletAvailable)
            IconButton(
              tooltip: context.t('businessCard.addToWallet'),
              icon: const Icon(Icons.wallet),
              onPressed: () => _addToWallet(repository, card),
            ),
          if (!widget.offline && repository != null)
            IconButton(
              tooltip: context.t('app.vCard.share'),
              icon: const Icon(Icons.ios_share),
              onPressed: () => shareVCard(repository, card),
            ),
        ],
      ),
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            const padding = 24.0;
            final landscape = constraints.maxWidth > constraints.maxHeight;
            final details = Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: landscape
                  ? CrossAxisAlignment.start
                  : CrossAxisAlignment.center,
              children: [
                Text(
                  card.displayName,
                  style: const TextStyle(color: Colors.black, fontSize: 22),
                ),
                if ((card['organization'] ?? '').isNotEmpty)
                  Text(
                    card['organization']!,
                    style: const TextStyle(color: Colors.black54, fontSize: 16),
                  ),
              ],
            );
            // Portrait: the texts below the code; landscape: beside it, the code as large as the height allows.
            final available = landscape
                ? constraints.maxHeight - 2 * padding
                : constraints.maxHeight - 2 * padding - 80;
            final size = available.clamp(80.0, 400.0);
            final code = png == null
                ? null
                : SizedBox(
                    width: size,
                    height: size,
                    // No smoothing: the modules of the code must stay sharp when scaled up.
                    child: Image.memory(
                      png,
                      filterQuality: FilterQuality.none,
                      fit: BoxFit.contain,
                    ),
                  );
            return Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(padding),
                child: landscape
                    ? Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          ?code,
                          const SizedBox(width: 24),
                          Flexible(child: details),
                        ],
                      )
                    : Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [?code, const SizedBox(height: 24), details],
                      ),
              ),
            );
          },
        ),
      ),
    );
  }
}
