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
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../core/api/api_exception.dart';
import '../../core/auth/auth_controller.dart';
import '../../core/i18n/translations.dart';
import '../../core/widgets/error_view.dart';
import 'web_download.dart';
import 'web_navigation.dart';
import 'web_session_repository.dart';

/// The ProjectForge pages (next, and the legacy pages they link to) in a WebView, logged in by a web session the
/// server opens for the app token ([WebSessionRepository]). The session is renewed when the pages land on the login
/// page (timeout, server restart).
///
/// Several tabs show a WebView each (the pages, the chat), all with the same web session.
///
/// The WebView gets no JavaScript handlers: the app token must stay out of reach of the pages.
class WebScreen extends ConsumerStatefulWidget {
  const WebScreen({super.key, required this.active, this.initialPath});

  /// The screen is the visible tab: the back gesture goes back in the WebView history.
  final bool active;

  /// The page shown first (e.g. `/next/chat`), default: the start page of the server.
  final String? initialPath;

  @override
  ConsumerState<WebScreen> createState() => _WebScreenState();
}

class _WebScreenState extends ConsumerState<WebScreen> {
  /// A renewal of the session at most this often, so a session that can't be opened doesn't loop.
  static const _renewalInterval = Duration(seconds: 30);

  InAppWebViewController? _controller;
  PullToRefreshController? _pullToRefresh;
  Uri? _startUrl;
  String? _error;
  double _progress = 0;
  bool _canGoBack = false;
  bool _opening = false;
  DateTime? _lastOpened;

  WebNavigation get _navigation => WebNavigation(
    Uri.parse(ref.read(authControllerProvider).session!.serverUrl),
  );

  @override
  void initState() {
    super.initState();
    _pullToRefresh = PullToRefreshController(
      onRefresh: () => _controller?.reload(),
    );
    _open(widget.initialPath);
  }

  /// Loads [path] (default: the start page of the server) with the web session of the app, a new one if [renew].
  Future<void> _open(String? path, {bool renew = false}) async {
    if (_opening) return;
    _opening = true;
    setState(() => _error = null);
    try {
      final sessions = ref.read(webSessionRepositoryProvider)!;
      final startPath = await (renew ? sessions.renew() : sessions.ensure());
      if (renew) _lastOpened = DateTime.now();
      final url = _navigation.url(path ?? startPath);
      if (_controller == null) {
        setState(() => _startUrl = url);
      } else {
        await _controller!.loadUrl(
          urlRequest: URLRequest(url: WebUri.uri(url)),
        );
      }
    } on ApiException catch (e) {
      if (mounted) _showError(e.message);
    } finally {
      _opening = false;
    }
  }

  /// The error replaces the WebView, a retry starts a new one.
  void _showError(String message) => setState(() {
    _error = message;
    _controller = null;
    _startUrl = null;
    _canGoBack = false;
  });

  /// The pages show the login: the web session is gone. Renewed once per [_renewalInterval], otherwise an error.
  void _onLoginPage(Uri url) {
    final lastOpened = _lastOpened;
    if (lastOpened != null &&
        DateTime.now().difference(lastOpened) < _renewalInterval) {
      _showError(context.t('app.web.failed'));
      return;
    }
    _open(_navigation.returnPath(url), renew: true);
  }

  /// A download of the pages (e.g. a chat file): loaded by the app and offered in the share sheet.
  Future<void> _onDownloadStart(DownloadStartRequest request) async {
    final messenger = ScaffoldMessenger.of(context);
    final failed = context.t('app.download.failed');
    try {
      final file = await WebDownload(
        navigation: _navigation,
        cookieHeader: ref.read(webCookiesProvider).cookieHeader,
      ).load(request.url, suggestedFilename: request.suggestedFilename);
      await SharePlus.instance.share(
        ShareParams(files: [XFile(file.path, mimeType: request.mimeType)]),
      );
    } on Exception {
      messenger.showSnackBar(SnackBar(content: Text(failed)));
    }
  }

  Future<NavigationActionPolicy> _shouldOverrideUrlLoading(
    InAppWebViewController controller,
    NavigationAction action,
  ) async {
    final url = action.request.url;
    if (url == null || !action.isForMainFrame) {
      return NavigationActionPolicy.ALLOW;
    }
    final navigation = _navigation;
    switch (navigation.decide(url)) {
      case WebNavigationTarget.webView:
        if (navigation.isLoginPage(url)) {
          _onLoginPage(url);
          return NavigationActionPolicy.CANCEL;
        }
        return NavigationActionPolicy.ALLOW;
      case WebNavigationTarget.external:
        await launchUrl(url, mode: LaunchMode.externalApplication);
        return NavigationActionPolicy.CANCEL;
      case WebNavigationTarget.cancel:
        return NavigationActionPolicy.CANCEL;
    }
  }

  /// Client side navigations of next (pushState) don't pass [_shouldOverrideUrlLoading].
  Future<void> _onUpdateVisitedHistory(
    InAppWebViewController controller,
    WebUri? url,
  ) async {
    if (url != null && _navigation.isLoginPage(url)) _onLoginPage(url);
    final canGoBack = await controller.canGoBack();
    if (mounted && canGoBack != _canGoBack) {
      setState(() => _canGoBack = canGoBack);
    }
  }

  @override
  Widget build(BuildContext context) {
    final error = _error;
    if (error != null) {
      return ErrorView(
        message: error,
        onRetry: () => _open(widget.initialPath, renew: true),
      );
    }
    final startUrl = _startUrl;
    if (startUrl == null) {
      return const Center(child: CircularProgressIndicator());
    }
    final appVersion =
        ref.read(deviceDescriptionProvider).value?.appVersion ?? '0';
    return PopScope(
      canPop: !widget.active || !_canGoBack,
      onPopInvokedWithResult: (didPop, _) {
        // Every WebView tab gets the gesture: only the visible one goes back.
        if (!didPop && widget.active) _controller?.goBack();
      },
      child: Stack(
        children: [
          InAppWebView(
            initialUrlRequest: URLRequest(url: WebUri.uri(startUrl)),
            initialSettings: InAppWebViewSettings(
              // Marks the app for next (lib/app-mode.ts), which then e.g. hides its logout.
              applicationNameForUserAgent: 'ProjectForgeApp/$appVersion',
              useShouldOverrideUrlLoading: true,
              allowsBackForwardNavigationGestures: true,
              javaScriptCanOpenWindowsAutomatically: false,
              supportMultipleWindows: false,
              useOnDownloadStart: true,
            ),
            pullToRefreshController: _pullToRefresh,
            onWebViewCreated: (controller) => _controller = controller,
            shouldOverrideUrlLoading: _shouldOverrideUrlLoading,
            onUpdateVisitedHistory: (controller, url, _) =>
                _onUpdateVisitedHistory(controller, url),
            onProgressChanged: (_, progress) {
              setState(() => _progress = progress / 100);
            },
            onLoadStop: (_, _) => _pullToRefresh?.endRefreshing(),
            onDownloadStartRequest: (_, request) => _onDownloadStart(request),
          ),
          if (_progress < 1) LinearProgressIndicator(value: _progress),
        ],
      ),
    );
  }
}
