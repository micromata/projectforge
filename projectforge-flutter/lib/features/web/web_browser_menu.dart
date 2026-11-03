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
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../core/i18n/translations.dart';
import 'web_screen_controller.dart';

/// The browser functions of a WebView tab, which has no address bar: back, forward, reload in a row on top (as the
/// menu of Chrome on Android), then the search in the page, sharing the link, the page in the system browser and
/// the restart of the WebView.
class WebBrowserMenu extends StatelessWidget {
  const WebBrowserMenu({super.key, required this.controller});

  final WebScreenController controller;

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: controller,
    builder: (context, _) => PopupMenuButton<String>(
      tooltip: context.t('app.web.menu'),
      icon: const Icon(Icons.more_vert),
      onSelected: (action) => switch (action) {
        'find' => controller.openFind(),
        'share' => _share(),
        'browser' => _openInBrowser(),
        _ => controller.restart(),
      },
      itemBuilder: (context) => [
        _Navigation(controller: controller),
        const PopupMenuDivider(),
        PopupMenuItem(
          value: 'find',
          enabled: controller.canFind,
          child: _Entry(Icons.search, context.t('app.web.find')),
        ),
        PopupMenuItem(
          value: 'share',
          enabled: controller.url != null,
          child: _Entry(Icons.share, context.t('app.web.share')),
        ),
        PopupMenuItem(
          value: 'browser',
          enabled: controller.url != null,
          child: _Entry(
            Icons.open_in_browser,
            context.t('app.web.openInBrowser'),
          ),
        ),
        const PopupMenuDivider(),
        PopupMenuItem(
          value: 'restart',
          child: _Entry(Icons.restart_alt, context.t('app.web.restart')),
        ),
      ],
    ),
  );

  Future<void> _share() async {
    final url = controller.url;
    if (url == null) return;
    await SharePlus.instance.share(ShareParams(uri: url));
  }

  /// The system browser has no web session of the app: it may ask for the login.
  Future<void> _openInBrowser() async {
    final url = controller.url;
    if (url == null) return;
    await launchUrl(url, mode: LaunchMode.externalApplication);
  }
}

/// Back, forward and reload: an entry of its own, as a [PopupMenuItem] has one action only (and merges the
/// semantics of its content into one).
class _Navigation extends PopupMenuEntry<String> {
  const _Navigation({required this.controller});

  final WebScreenController controller;

  @override
  double get height => kMinInteractiveDimension;

  @override
  bool represents(String? value) => false;

  @override
  State<_Navigation> createState() => _NavigationState();
}

class _NavigationState extends State<_Navigation> {
  @override
  Widget build(BuildContext context) {
    final controller = widget.controller;
    void run(VoidCallback command) {
      Navigator.of(context).pop();
      command();
    }

    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        IconButton(
          tooltip: context.t('back'),
          icon: const Icon(Icons.arrow_back),
          onPressed: controller.canGoBack ? () => run(controller.goBack) : null,
        ),
        IconButton(
          tooltip: context.t('app.web.forward'),
          icon: const Icon(Icons.arrow_forward),
          onPressed: controller.canGoForward
              ? () => run(controller.goForward)
              : null,
        ),
        IconButton(
          tooltip: context.t('app.web.reload'),
          icon: const Icon(Icons.refresh),
          onPressed: controller.attached ? () => run(controller.reload) : null,
        ),
      ],
    );
  }
}

class _Entry extends StatelessWidget {
  const _Entry(this.icon, this.text);

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) =>
      Row(children: [Icon(icon), const SizedBox(width: 12), Text(text)]);
}
