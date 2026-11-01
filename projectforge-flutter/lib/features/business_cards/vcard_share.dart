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

import 'dart:io';

import 'package:share_plus/share_plus.dart';

import '../../core/cache/shared_files.dart';
import 'business_card.dart';
import 'business_cards_repository.dart';

/// Shares the card as vCard file (mail, messenger, AirDrop, …).
Future<void> shareVCard(
  BusinessCardsRepository repository,
  BusinessCard card,
) async {
  final bytes = await repository.vCard(card.id!);
  final name =
      (card.displayName.isNotEmpty ? card.displayName : card.cardName ?? 'card')
          .replaceAll(RegExp(r'[^\w\- ]+', unicode: true), '_');
  final file = File('${(await SharedFiles().directory()).path}/$name.vcf');
  await file.writeAsBytes(bytes, flush: true);
  await SharePlus.instance.share(
    ShareParams(files: [XFile(file.path, mimeType: 'text/vcard')]),
  );
}
