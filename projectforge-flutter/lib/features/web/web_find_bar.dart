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

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

import '../../core/i18n/translations.dart';

/// The matches of a search in the page: the one shown (0 based) and their number.
class WebFindResult {
  const WebFindResult(this.active, this.count);

  final int active;
  final int count;
}

/// The search in the page where the WebView has no native one (Android: the matches are found and marked by the
/// WebView itself, the page's DOM stays untouched). On iOS the native find navigator of WebKit is used instead.
class WebFindBar extends StatefulWidget {
  const WebFindBar({
    super.key,
    required this.controller,
    required this.result,
    required this.onClose,
  });

  final FindInteractionController controller;

  /// The latest result of the WebView (`onFindResultReceived`), null before the first search.
  final ValueListenable<WebFindResult?> result;

  final VoidCallback onClose;

  @override
  State<WebFindBar> createState() => _WebFindBarState();
}

class _WebFindBarState extends State<WebFindBar> {
  /// Every key stroke would start a new search of the whole page otherwise.
  static const _debounce = Duration(milliseconds: 250);

  final _text = TextEditingController();
  Timer? _timer;

  @override
  void dispose() {
    _timer?.cancel();
    _text.dispose();
    super.dispose();
  }

  void _onChanged(String text) {
    _timer?.cancel();
    _timer = Timer(_debounce, () {
      if (text.isEmpty) {
        widget.controller.clearMatches();
      } else {
        widget.controller.findAll(find: text);
      }
    });
  }

  void _next({required bool forward}) {
    if (_text.text.isEmpty) return;
    widget.controller.findNext(forward: forward);
  }

  void _close() {
    _timer?.cancel();
    widget.controller.clearMatches();
    widget.onClose();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      elevation: 3,
      color: scheme.surfaceContainerHigh,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        child: Row(
          children: [
            Expanded(
              child: TextField(
                controller: _text,
                autofocus: true,
                textInputAction: TextInputAction.search,
                decoration: InputDecoration(
                  hintText: context.t('app.web.find'),
                  border: InputBorder.none,
                  isDense: true,
                ),
                onChanged: _onChanged,
                // The keyboard's search key: the next match, the field keeps the focus.
                onSubmitted: (_) => _next(forward: true),
              ),
            ),
            ValueListenableBuilder(
              valueListenable: widget.result,
              builder: (context, result, _) => Text(
                result == null || _text.text.isEmpty
                    ? ''
                    : result.count == 0
                    ? '0/0'
                    : '${result.active + 1}/${result.count}',
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ),
            IconButton(
              tooltip: context.t('app.web.find.previous'),
              icon: const Icon(Icons.keyboard_arrow_up),
              onPressed: () => _next(forward: false),
            ),
            IconButton(
              tooltip: context.t('app.web.find.next'),
              icon: const Icon(Icons.keyboard_arrow_down),
              onPressed: () => _next(forward: true),
            ),
            IconButton(
              tooltip: context.t('close'),
              icon: const Icon(Icons.close),
              onPressed: _close,
            ),
          ],
        ),
      ),
    );
  }
}
