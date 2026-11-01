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
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_exception.dart';
import '../../core/i18n/translations.dart';
import 'address_search_dialog.dart';
import 'business_card.dart';
import 'business_card_fields.dart';
import 'business_cards_controller.dart';

/// Creates or edits a card, as the editor of projectforge-next does: every field has a tick whether it is on the
/// card. An unticked field is stored empty; ticking it again fills it from the source address. Taken over from an
/// address, the business fields with a value are ticked, the private ones never.
class BusinessCardEditorScreen extends ConsumerStatefulWidget {
  const BusinessCardEditorScreen({
    super.key,
    required this.initial,
    required this.fromAddress,
    required this.gatewayConfigured,
  });

  final BusinessCard initial;

  /// [initial] is a new card made of an address (the private fields start unticked).
  final bool fromAddress;
  final bool gatewayConfigured;

  @override
  ConsumerState<BusinessCardEditorScreen> createState() =>
      _BusinessCardEditorScreenState();
}

class _BusinessCardEditorScreenState
    extends ConsumerState<BusinessCardEditorScreen> {
  final _cardName = TextEditingController();
  final _controllers = {
    for (final f in businessCardFields) f.name: TextEditingController(),
  };
  final _included = <String, bool>{};
  late BusinessCard _base;
  BusinessCard? _source;
  late bool _includePhoto;
  BusinessCardPreview? _preview;
  Timer? _previewTimer;
  String? _cardNameError;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _apply(widget.initial, fromAddress: widget.fromAddress);
    _cardName.text = widget.initial.cardName ?? '';
    _includePhoto = widget.initial.includePhoto;
    _cardName.addListener(_schedulePreview);
    for (final controller in _controllers.values) {
      controller.addListener(_schedulePreview);
    }
  }

  /// Takes the values of [card]; from an address, its private fields start unticked (and empty).
  void _apply(BusinessCard card, {required bool fromAddress}) {
    _base = card;
    if (fromAddress) _source = card;
    for (final field in businessCardFields) {
      final value = card[field.name] ?? '';
      final include = value.isNotEmpty && !(fromAddress && field.private);
      _included[field.name] = include;
      _controllers[field.name]!.text = include ? value : '';
    }
    _schedulePreview();
  }

  @override
  void dispose() {
    _previewTimer?.cancel();
    _cardName.dispose();
    for (final controller in _controllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  BusinessCard _current() {
    final json = Map<String, dynamic>.from(_base.json)
      ..['id'] = widget.initial.id
      ..['cardName'] = _cardName.text.trim()
      ..['includePhoto'] = _includePhoto
      ..['addressId'] = (_source ?? _base).addressId;
    for (final field in businessCardFields) {
      final text = _controllers[field.name]!.text.trim();
      json[field.name] = _included[field.name] == true && text.isNotEmpty
          ? text
          : null;
    }
    return BusinessCard(json);
  }

  void _schedulePreview() {
    _previewTimer?.cancel();
    _previewTimer = Timer(const Duration(milliseconds: 600), () async {
      final repository = ref.read(businessCardsRepositoryProvider);
      if (repository == null) return;
      try {
        final preview = await repository.preview(_current());
        if (mounted) setState(() => _preview = preview);
      } on ApiException {
        // The preview is a nicety: the save reports what is wrong.
      }
    });
  }

  Future<void> _toggle(BusinessCardField field, bool value) async {
    setState(() => _included[field.name] = value);
    final controller = _controllers[field.name]!;
    if (!value || controller.text.isNotEmpty) return;
    final addressId = _base.addressId;
    if (_source == null && addressId != null) {
      try {
        _source = await ref
            .read(businessCardsRepositoryProvider)!
            .fromAddress(addressId);
      } on ApiException {
        return; // The address is gone or not readable anymore: the field is just empty.
      }
    }
    controller.text = _source?[field.name] ?? '';
  }

  Future<void> _takeOverAddress() async {
    final repository = ref.read(businessCardsRepositoryProvider)!;
    final id = await showAddressSearchDialog(context, repository);
    if (id == null) return;
    final card = await repository.fromAddress(id);
    setState(() => _apply(card, fromAddress: true));
  }

  Future<void> _save() async {
    setState(() {
      _saving = true;
      _cardNameError = null;
    });
    final messenger = ScaffoldMessenger.of(context);
    final navigator = Navigator.of(context);
    final savedText = context.t('businessCard.saved');
    try {
      await ref.read(businessCardsRepositoryProvider)!.save(_current());
      messenger.showSnackBar(SnackBar(content: Text(savedText)));
      navigator.pop(true);
    } on ApiException catch (e) {
      final nameError = e.validationErrors
          .where((it) => it.fieldId == 'cardName')
          .firstOrNull;
      setState(() => _cardNameError = nameError?.message);
      if (nameError == null) {
        messenger.showSnackBar(SnackBar(content: Text(e.message)));
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final preview = _preview;
    final png = preview?.qrCodePng;
    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.initial.id == null
              ? context.t('businessCard.new')
              : context.t('edit'),
        ),
        actions: [
          TextButton(
            onPressed: _saving ? null : _save,
            child: Text(context.t('save')),
          ),
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            TextField(
              controller: _cardName,
              decoration: InputDecoration(
                labelText: context.t('businessCard.cardName'),
                errorText: _cardNameError,
              ),
            ),
            const SizedBox(height: 8),
            Align(
              alignment: Alignment.centerLeft,
              child: OutlinedButton.icon(
                icon: const Icon(Icons.contacts),
                label: Text(context.t('businessCard.address')),
                onPressed: _takeOverAddress,
              ),
            ),
            if (png != null) ...[
              const SizedBox(height: 12),
              Center(
                child: Image.memory(
                  png,
                  width: 200,
                  height: 200,
                  filterQuality: FilterQuality.none,
                ),
              ),
              if (preview!.vCardLength > denseVCardLength)
                Text(
                  context.t('businessCard.qrCode.dense'),
                  style: TextStyle(color: Theme.of(context).colorScheme.error),
                ),
            ],
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(context.t('businessCard.includePhoto')),
              subtitle: Text(
                !widget.gatewayConfigured
                    ? context.t('businessCard.includePhoto.noGateway')
                    : preview?.photoAllowed == false
                    ? context.t('businessCard.includePhoto.notAllowed')
                    : context.t('businessCard.includePhoto.tooltip'),
              ),
              value: _includePhoto,
              onChanged:
                  widget.gatewayConfigured &&
                          (preview?.photoAllowed ?? false) ||
                      _includePhoto
                  ? (value) => setState(() {
                      _includePhoto = value;
                      _schedulePreview();
                    })
                  : null,
            ),
            _SectionTitle(context.t('businessCard.section.business')),
            for (final field in businessCardFields.where((it) => !it.private))
              _fieldRow(field),
            _SectionTitle(context.t('businessCard.section.private')),
            for (final field in businessCardFields.where((it) => it.private))
              _fieldRow(field),
          ],
        ),
      ),
    );
  }

  Widget _fieldRow(BusinessCardField field) => Row(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Checkbox(
        value: _included[field.name] ?? false,
        onChanged: (value) => _toggle(field, value ?? false),
      ),
      Expanded(
        child: TextField(
          controller: _controllers[field.name],
          enabled: _included[field.name] ?? false,
          minLines: 1,
          maxLines: field.multiline ? 4 : 1,
          decoration: InputDecoration(labelText: context.t(field.labelKey)),
        ),
      ),
    ],
  );
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.text);

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 20, bottom: 4),
    child: Text(text, style: Theme.of(context).textTheme.titleMedium),
  );
}
