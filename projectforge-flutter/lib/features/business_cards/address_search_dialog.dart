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

import 'dart:async';

import 'package:flutter/material.dart';

import '../../core/i18n/translations.dart';
import 'business_cards_repository.dart';

/// Search of the address whose data a card takes over; answers its id.
Future<int?> showAddressSearchDialog(
  BuildContext context,
  BusinessCardsRepository repository,
) => showDialog<int>(
  context: context,
  builder: (_) => _AddressSearchDialog(repository),
);

class _AddressSearchDialog extends StatefulWidget {
  const _AddressSearchDialog(this.repository);

  final BusinessCardsRepository repository;

  @override
  State<_AddressSearchDialog> createState() => _AddressSearchDialogState();
}

class _AddressSearchDialogState extends State<_AddressSearchDialog> {
  Timer? _debounce;
  var _hits = <({int id, String displayName})>[];
  String? _error;

  void _search(String text) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 300), () async {
      if (text.trim().length < 2) return;
      try {
        final hits = await widget.repository.searchAddresses(text.trim());
        if (mounted) {
          setState(() {
            _hits = hits;
            _error = null;
          });
        }
      } on Exception catch (e) {
        if (mounted) setState(() => _error = e.toString());
      }
    });
  }

  @override
  void dispose() {
    _debounce?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: Text(context.t('businessCard.address')),
    content: SizedBox(
      width: double.maxFinite,
      height: 360,
      child: Column(
        children: [
          TextField(
            autofocus: true,
            decoration: InputDecoration(
              prefixIcon: const Icon(Icons.search),
              hintText: context.t('search'),
            ),
            onChanged: _search,
          ),
          if (_error != null)
            Text(
              _error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          Expanded(
            child: ListView(
              children: [
                for (final hit in _hits)
                  ListTile(
                    title: Text(hit.displayName),
                    onTap: () => Navigator.pop(context, hit.id),
                  ),
              ],
            ),
          ),
        ],
      ),
    ),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context),
        child: Text(context.t('cancel')),
      ),
    ],
  );
}
