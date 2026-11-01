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

import 'dart:io';

import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../core/i18n/translations.dart';
import 'address.dart';
import 'address_avatar.dart';

/// An address with its numbers, mail addresses and postal addresses. A number offers a call or a text message, a mail
/// address opens the mail app, a postal address the maps app. [onEdit] shows the form in the web pages; null (offline)
/// hides the button.
class AddressDetailScreen extends StatelessWidget {
  const AddressDetailScreen({super.key, required this.address, this.onEdit});

  final Address address;
  final VoidCallback? onEdit;

  @override
  Widget build(BuildContext context) {
    final colors = AddressAvatar.gradientOf(address);
    final dark = HSLColor.fromColor(colors[1]).withLightness(0.28).toColor();
    final mobile = address.phones.where((it) => it.mobile).firstOrNull;
    final phone = mobile ?? address.phones.firstOrNull;
    final email = address.emails.firstOrNull;
    final subtitle = [
      address.positionText,
      address.division,
      address.organization,
    ].where((it) => it != null && it.trim().isNotEmpty).join(' · ');
    return Theme(
      data: Theme.of(context).copyWith(brightness: Brightness.dark),
      child: Scaffold(
        extendBodyBehindAppBar: true,
        backgroundColor: dark,
        appBar: AppBar(
          backgroundColor: Colors.transparent,
          foregroundColor: Colors.white,
          elevation: 0,
          scrolledUnderElevation: 0,
          actions: [
            if (onEdit != null && address.writeAccess)
              TextButton(
                onPressed: () {
                  Navigator.of(context).pop();
                  onEdit!();
                },
                child: Text(
                  context.t('edit'),
                  style: const TextStyle(color: Colors.white),
                ),
              ),
          ],
        ),
        body: Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: [colors[1].withValues(alpha: 0.75), dark],
            ),
          ),
          child: SafeArea(
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 32),
              children: [
                Center(
                  child: AddressAvatar(address: address, size: 144, full: true),
                ),
                const SizedBox(height: 16),
                Text(
                  address.displayName,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 24,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                if (subtitle.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(
                      subtitle,
                      textAlign: TextAlign.center,
                      style: const TextStyle(color: Colors.white70),
                    ),
                  ),
                const SizedBox(height: 16),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    _QuickAction(
                      icon: Icons.chat_bubble,
                      label: context.t('app.address.message'),
                      onPressed: mobile == null
                          ? null
                          : () => _launch('sms', mobile.number),
                    ),
                    _QuickAction(
                      icon: Icons.call,
                      label: context.t('app.address.call'),
                      onPressed: phone == null
                          ? null
                          : () => _launch('tel', phone.number),
                    ),
                    _QuickAction(
                      icon: Icons.mail,
                      label: context.t('email'),
                      onPressed: email == null
                          ? null
                          : () => _launch('mailto', email.address),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                if (address.phones.isNotEmpty)
                  _Section([
                    for (final it in address.phones)
                      _Row(
                        label: _phoneLabel(context, it.labelKey),
                        value: it.number,
                        icon: Icons.call,
                        onTap: () => _phoneSheet(context, it),
                      ),
                  ]),
                if (address.emails.isNotEmpty)
                  _Section([
                    for (final it in address.emails)
                      _Row(
                        label: it.private
                            ? context.t('address.private')
                            : context.t('address.business'),
                        value: it.address,
                        icon: Icons.mail,
                        onTap: () => _launch('mailto', it.address),
                      ),
                  ]),
                if (address.website != null &&
                    address.website!.trim().isNotEmpty)
                  _Section([
                    _Row(
                      label: context.t('address.website'),
                      value: address.website!.trim(),
                      icon: Icons.language,
                      onTap: () => _launchWebsite(address.website!),
                    ),
                  ]),
                for (final (label, postal) in [
                  (
                    context.t('address.heading.businessAddress'),
                    address.business,
                  ),
                  (context.t('address.heading.postalAddress'), address.postal),
                  (
                    context.t('address.heading.privateAddress'),
                    address.private,
                  ),
                ])
                  if (postal != null && postal.lines.isNotEmpty)
                    _Section([
                      _Row(
                        label: label,
                        value: postal.lines.join('\n'),
                        icon: Icons.map_outlined,
                        onTap: () => _launchMap(postal),
                      ),
                    ]),
                if (address.birthday != null)
                  _Section([
                    _Row(
                      label: context.t('address.birthday'),
                      value: address.birthday!,
                    ),
                  ]),
                if (address.comment != null &&
                    address.comment!.trim().isNotEmpty)
                  _Section([
                    _Row(
                      label: context.t('comment'),
                      value: address.comment!.trim(),
                    ),
                  ]),
              ],
            ),
          ),
        ),
      ),
    );
  }

  static String _phoneLabel(BuildContext context, String key) => switch (key) {
    'address.phoneType.business' => context.t(
      'address.phoneType.business.short',
    ),
    'address.phoneType.mobile' => context.t('address.phoneType.mobile.short'),
    'address.phoneType.private' => context.t('address.phoneType.private.short'),
    'address.phoneType.privateMobile' => context.t(
      'address.phoneType.privateMobile.short',
    ),
    _ => context.t('address.phoneType.fax.short'),
  };

  /// A number: call or text message (the latter only for a mobile number).
  Future<void> _phoneSheet(BuildContext context, PhoneNumber number) {
    final isFax = number.labelKey == 'address.phoneType.fax';
    return showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (sheet) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(title: Text(number.number)),
            if (!isFax)
              ListTile(
                leading: const Icon(Icons.call),
                title: Text(context.t('app.address.call')),
                onTap: () {
                  Navigator.pop(sheet);
                  _launch('tel', number.number);
                },
              ),
            if (number.mobile)
              ListTile(
                leading: const Icon(Icons.chat_bubble_outline),
                title: Text(context.t('app.address.message')),
                onTap: () {
                  Navigator.pop(sheet);
                  _launch('sms', number.number);
                },
              ),
          ],
        ),
      ),
    );
  }

  static Future<void> _launch(String scheme, String path) => launchUrl(
    Uri(
      scheme: scheme,
      path: path.replaceAll(RegExp(r'[^0-9+*#@.a-zA-Z_-]'), ''),
    ),
    mode: LaunchMode.externalApplication,
  );

  static Future<void> _launchWebsite(String website) {
    final trimmed = website.trim();
    final url = RegExp(r'^https?://', caseSensitive: false).hasMatch(trimmed)
        ? trimmed
        : 'https://$trimmed';
    return launchUrl(Uri.parse(url), mode: LaunchMode.externalApplication);
  }

  static Future<void> _launchMap(PostalAddress postal) {
    final query = Uri.encodeQueryComponent(postal.oneLine);
    final url = Platform.isIOS
        ? 'https://maps.apple.com/?q=$query'
        : 'geo:0,0?q=$query';
    return launchUrl(Uri.parse(url), mode: LaunchMode.externalApplication);
  }
}

class _QuickAction extends StatelessWidget {
  const _QuickAction({required this.icon, required this.label, this.onPressed});

  final IconData icon;
  final String label;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(horizontal: 6),
    child: IconButton.filledTonal(
      tooltip: label,
      icon: Icon(icon),
      style: IconButton.styleFrom(
        backgroundColor: Colors.white.withValues(
          alpha: onPressed == null ? 0.08 : 0.2,
        ),
        foregroundColor: Colors.white.withValues(
          alpha: onPressed == null ? 0.4 : 1,
        ),
        fixedSize: const Size(52, 52),
      ),
      onPressed: onPressed,
    ),
  );
}

/// A group of rows on a translucent card.
class _Section extends StatelessWidget {
  const _Section(this.rows);

  final List<_Row> rows;

  @override
  Widget build(BuildContext context) => Container(
    margin: const EdgeInsets.only(bottom: 10),
    decoration: BoxDecoration(
      color: Colors.white.withValues(alpha: 0.14),
      borderRadius: BorderRadius.circular(20),
    ),
    child: Column(
      children: [
        for (var i = 0; i < rows.length; i++) ...[
          if (i > 0) Divider(height: 1, indent: 16, color: Colors.white24),
          rows[i],
        ],
      ],
    ),
  );
}

class _Row extends StatelessWidget {
  const _Row({required this.label, required this.value, this.icon, this.onTap});

  final String label;
  final String value;
  final IconData? icon;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) => InkWell(
    borderRadius: BorderRadius.circular(20),
    onTap: onTap,
    child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: const TextStyle(color: Colors.white70)),
                const SizedBox(height: 2),
                Text(
                  value,
                  style: const TextStyle(color: Colors.white, fontSize: 16),
                ),
              ],
            ),
          ),
          if (icon != null) Icon(icon, size: 18, color: Colors.white),
        ],
      ),
    ),
  );
}
