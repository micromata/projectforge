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

import 'package:flutter/foundation.dart';

/// What a [WebScreen] does for its browser menu ([WebBrowserMenu]), implemented by its state.
abstract interface class WebScreenCommands {
  void goBack();

  void goForward();

  /// The page again (as pull to refresh).
  void reload();

  /// A new WebView with a new web session on the page shown, as a restart of the app would.
  void restart();

  /// The search in the page is available (not on iOS before 16).
  bool get canFind;

  void openFind();
}

/// The browser functions of one WebView tab: owned by the home screen (its browser menu lives in the navigation),
/// filled by the [WebScreen] of the tab while it is there.
class WebScreenController extends ChangeNotifier {
  WebScreenCommands? _commands;
  bool _canGoBack = false;
  bool _canGoForward = false;
  Uri? _url;

  bool get canGoBack => _canGoBack;

  bool get canGoForward => _canGoForward;

  /// The page of the server shown, null before the first one.
  Uri? get url => _url;

  bool get canFind => _commands?.canFind ?? false;

  /// The WebView is there: the commands can be run.
  bool get attached => _commands != null;

  /// Without notifying: called while the tree is built (initState, dispose). The menu reads the state when opened.
  void attach(WebScreenCommands commands) {
    _commands = commands;
  }

  void detach(WebScreenCommands commands) {
    if (!identical(_commands, commands)) return;
    _commands = null;
    _canGoBack = false;
    _canGoForward = false;
  }

  /// Called by the [WebScreen] on every change of its history.
  void update({required bool canGoBack, required bool canGoForward, Uri? url}) {
    if (canGoBack == _canGoBack &&
        canGoForward == _canGoForward &&
        (url == null || url == _url)) {
      return;
    }
    _canGoBack = canGoBack;
    _canGoForward = canGoForward;
    if (url != null) _url = url;
    notifyListeners();
  }

  void goBack() => _commands?.goBack();

  void goForward() => _commands?.goForward();

  void reload() => _commands?.reload();

  void restart() => _commands?.restart();

  void openFind() => _commands?.openFind();
}
