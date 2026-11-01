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

import '../../core/i18n/translations.dart';
import 'business_card.dart';
import 'business_cards_controller.dart';
import 'vcard_share.dart';

/// The QR code of a card in full size, for scanning by somebody else: the screen is set to full brightness while
/// it is shown. Works offline (the QR code is part of the cached list).
class BusinessCardQrScreen extends ConsumerStatefulWidget {
  const BusinessCardQrScreen({
    super.key,
    required this.info,
    this.offline = false,
  });

  final BusinessCardInfo info;
  final bool offline;

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
          if (!widget.offline && repository != null)
            IconButton(
              tooltip: context.t('app.vCard.share'),
              icon: const Icon(Icons.ios_share),
              onPressed: () => shareVCard(repository, card),
            ),
        ],
      ),
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (png != null)
                  ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 400),
                    child: AspectRatio(
                      aspectRatio: 1,
                      // No smoothing: the modules of the code must stay sharp when scaled up.
                      child: Image.memory(
                        png,
                        filterQuality: FilterQuality.none,
                        fit: BoxFit.contain,
                      ),
                    ),
                  ),
                const SizedBox(height: 24),
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
            ),
          ),
        ),
      ),
    );
  }
}
