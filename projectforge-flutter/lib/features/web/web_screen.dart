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

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/auth/auth_controller.dart';
import '../../core/i18n/translations.dart';
import '../../core/log/app_log.dart';
import '../../core/log/log_sanitizer.dart';
import '../../core/wallet/wallet_pass.dart';
import '../../core/web/native_page.dart';
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
  const WebScreen({
    super.key,
    required this.active,
    this.initialPath,
    this.target,
    this.onNativePage,
  });

  /// The screen is the visible tab: the back gesture goes back in the WebView history.
  final bool active;

  /// The page shown first (e.g. `/next/chat`), default: the start page of the server.
  final String? initialPath;

  /// A page other tabs ask this one to show; a new instance loads its path again, even the same one.
  final WebTarget? target;

  /// The pages navigate to a page with a native tab (e.g. the chat of the menu): true if the app shows its tab
  /// instead, the WebView then stays on its page. Not called for a page this screen was asked to load itself.
  final bool Function(NativePage page)? onNativePage;

  @override
  ConsumerState<WebScreen> createState() => _WebScreenState();
}

/// A request to show [path] (e.g. `/next/myBusinessCards`) in a [WebScreen].
///
/// Not const: const instances with the same path would be identical, and the same page wouldn't be loaded again.
class WebTarget {
  WebTarget(this.path);

  final String path;
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

  /// A page of the server is loading and not yet committed (`onPageCommitVisible`): a redirect to another site now is
  /// one of the server or its proxy, not a script of a loaded page. Set for every load of the app itself too
  /// ([_load]), as Android doesn't pass these through [_shouldOverrideUrlLoading]. A start without end (a load that
  /// became a download, a lost callback) counts only for [_maxLoadTime].
  DateTime? _serverPageLoadStart;

  static const _maxLoadTime = Duration(seconds: 30);

  bool get _loadingServerPage {
    final start = _serverPageLoadStart;
    return start != null && DateTime.now().difference(start) < _maxLoadTime;
  }

  /// The page of the server the current load was asked for, before any redirect: loaded again after a renewal.
  Uri? _requestedUrl;

  bool _renewingProxy = false;
  DateTime? _lastProxyRenewal;

  /// The error shown is a refusal of the SSO proxy: a retry renews the login in the system browser itself (the
  /// [ApiClient] may not, see its `_uselessProxyCookies`) and loads the refused page again.
  bool _proxyRefusalShown = false;

  /// The native page [_open] loaded itself (e.g. the editing of the business cards): it stays in the WebView until
  /// the user leaves it.
  NativePage? _requestedNative;

  WebNavigation get _navigation => WebNavigation(
    Uri.parse(ref.read(authControllerProvider).session!.serverUrl),
  );

  @override
  void initState() {
    super.initState();
    _pullToRefresh = PullToRefreshController(onRefresh: _reload);
    _open(widget.initialPath);
  }

  @override
  void didUpdateWidget(WebScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    final target = widget.target;
    if (target != null && !identical(target, oldWidget.target)) {
      _open(target.path);
    }
  }

  /// Loads [path] (default: the start page of the server) with the web session of the app, a new one if [renew].
  /// Only a page of the server: the app's own loads don't pass [_shouldOverrideUrlLoading] on Android.
  ///
  /// @param renewLogin False: a refusal of the web session doesn't start a new login in the browser (just done).
  Future<void> _open(
    String? path, {
    bool renew = false,
    bool renewLogin = true,
  }) async {
    if (_opening) return;
    _opening = true;
    setState(() => _error = null);
    try {
      final sessions = ref.read(webSessionRepositoryProvider)!;
      final startPath = await (renew
          ? sessions.renew(renewLogin: renewLogin)
          : sessions.ensure());
      if (renew) _lastOpened = DateTime.now();
      final url = _navigation.pageUrl(path ?? startPath);
      if (url == null) {
        appLog.warn(
          'web',
          'Not a page of the server: ${LogSanitizer.urlString(path ?? startPath)}, not loaded',
        );
        if (mounted) _showError(context.t('app.web.failed'));
        return;
      }
      appLog.info('web', 'Load ${LogSanitizer.url(url)}');
      _requestedNative = _navigation.nativePage(url);
      if (_controller == null) {
        _startServerPageLoad(url);
        setState(() => _startUrl = url);
      } else {
        await _load(url);
      }
    } on ApiException catch (e) {
      appLog.warn('web', 'Pages not opened: $e');
      if (mounted) {
        _showError(
          context.errorText(e),
          proxyRefusal: e.messageKey == ApiClient.proxyRefusedKey,
        );
      }
    } finally {
      _opening = false;
    }
  }

  /// Loads [url], a page of the server, as a load of its own (see [_serverPageLoadStart]).
  Future<void> _load(Uri url) async {
    _startServerPageLoad(url);
    await _controller?.loadUrl(urlRequest: URLRequest(url: WebUri.uri(url)));
  }

  void _startServerPageLoad(Uri url) {
    _requestedUrl = url;
    _serverPageLoadStart = DateTime.now();
  }

  /// The page again (pull to refresh), as a load of its own: Android doesn't pass a reload through
  /// [_shouldOverrideUrlLoading], a redirect of the proxy wouldn't be recognized otherwise.
  Future<void> _reload() async {
    final controller = _controller;
    if (controller == null) return;
    final url = await controller.getUrl();
    if (url != null && _navigation.isSameOrigin(url)) {
      _startServerPageLoad(url);
    }
    await controller.reload();
  }

  /// The error replaces the WebView, a retry starts a new one.
  void _showError(String message, {bool proxyRefusal = false}) => setState(() {
    _error = message;
    _proxyRefusalShown = proxyRefusal;
    _controller = null;
    _startUrl = null;
    _canGoBack = false;
  });

  /// The pages show the login: the web session is gone. Renewed once per [_renewalInterval], otherwise an error.
  void _onLoginPage(Uri url) {
    final lastOpened = _lastOpened;
    if (lastOpened != null &&
        DateTime.now().difference(lastOpened) < _renewalInterval) {
      appLog.warn(
        'web',
        'Login page again right after a renewal of the web session: error shown',
      );
      _showError(context.t('app.web.failed'));
      return;
    }
    appLog.info('web', 'Login page of the server: renewal of the web session');
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
      if (walletAvailable &&
          WebDownload.isWalletPass(
            mimeType: request.mimeType,
            fileName: file.uri.pathSegments.last,
          )) {
        // The pass of a business card ("Add to Wallet" of the page): the Wallet, not the share sheet.
        await showWalletPass(await file.readAsBytes());
        return;
      }
      await SharePlus.instance.share(
        ShareParams(files: [XFile(file.path, mimeType: request.mimeType)]),
      );
    } on WalletException catch (e) {
      messenger.showSnackBar(SnackBar(content: Text(e.message)));
    } on Exception {
      messenger.showSnackBar(SnackBar(content: Text(failed)));
    }
  }

  /// True if the app shows [url] in its native tab instead (see [WebScreen.onNativePage]).
  bool _handOver(Uri url) {
    final page = _navigation.nativePage(url);
    if (page == _requestedNative) return false;
    _requestedNative = null;
    return page != null && (widget.onNativePage?.call(page) ?? false);
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
    final redirect = _isRedirect(action);
    final proxyLogin = navigation.isProxyLogin(
      url,
      redirect: redirect,
      loadingServerPage: _loadingServerPage,
      behindProxy:
          ref.read(authControllerProvider).session?.proxyCookies.isNotEmpty ??
          false,
    );
    if (proxyLogin) {
      appLog.info(
        'web',
        'Navigation to ${LogSanitizer.url(url)} → login of the SSO proxy? (${_describe(action)})',
      );
      _serverPageLoadStart = null;
      _renewForProxy(url);
      return NavigationActionPolicy.CANCEL;
    }
    final target = navigation.decide(url);
    appLog.info(
      'web',
      'Navigation to ${LogSanitizer.url(url)} → ${target.name} (${_describe(action)})',
    );
    switch (target) {
      case WebNavigationTarget.webView:
        if (!navigation.isSameOrigin(url)) {
          // An inline page (about:, blob:), no longer a page of the server: its redirects aren't the proxy's.
          _serverPageLoadStart = null;
          return NavigationActionPolicy.ALLOW;
        }
        if (navigation.isLoginPage(url)) {
          _cancelled(redirect);
          _onLoginPage(url);
          return NavigationActionPolicy.CANCEL;
        }
        if (_handOver(url)) return _cancelled(redirect);
        // A redirect within the load keeps the page asked for.
        if (!_loadingServerPage || !redirect) _startServerPageLoad(url);
        return NavigationActionPolicy.ALLOW;
      case WebNavigationTarget.external:
        _cancelled(redirect);
        await _logCookies('Opened outside the app');
        await launchUrl(url, mode: LaunchMode.externalApplication);
        return NavigationActionPolicy.CANCEL;
      case WebNavigationTarget.cancel:
        return _cancelled(redirect);
    }
  }

  /// A redirect cancelled here ends the load it belongs to; Android then reports neither its end nor an error.
  NavigationActionPolicy _cancelled(bool redirect) {
    if (redirect) _serverPageLoadStart = null;
    return NavigationActionPolicy.CANCEL;
  }

  /// The SSO proxy refused a page and redirects to its login: the login of the app is renewed in the system browser
  /// (AuthController.reauthenticate), which shares the session of the proxy's login with the browser (often no
  /// password then) and brings fresh proxy cookies, also into the WebView; then the page is loaded again. The
  /// password of the proxy's login isn't typed into a WebView of the app (RFC 8252): if the renewal fails or is
  /// cancelled, or the proxy refuses again right after it, the refusal is shown (a retry renews again).
  ///
  /// Only if the proxy refuses the app too ([ApiClient.proxyRefuses]): another site the server redirects to (an open
  /// redirect) is opened outside the app, it never gets a login of the app nor the WebView.
  Future<void> _renewForProxy(Uri target) async {
    if (_renewingProxy) return;
    _renewingProxy = true;
    final url = _requestedUrl ?? _navigation.url(WebNavigation.defaultPath);
    try {
      final api = ref.read(apiClientProvider);
      if (api == null || !await api.proxyRefuses()) {
        appLog.warn(
          'web',
          'Redirected to ${LogSanitizer.url(target)}, but the SSO proxy lets the app through: opened outside the app',
        );
        await launchUrl(target, mode: LaunchMode.externalApplication);
        return;
      }
      if (!mounted) return;
      if (!widget.active) {
        // No login the user didn't cause (e.g. the chat reloading in the background): the retry of the error renews.
        appLog.info(
          'web',
          'Refused by the SSO proxy in a tab not shown: error shown, no renewal',
        );
        _showError(context.t(ApiClient.proxyRefusedKey), proxyRefusal: true);
        return;
      }
      final last = _lastProxyRenewal;
      if (last != null && DateTime.now().difference(last) < _renewalInterval) {
        appLog.warn(
          'web',
          'Refused by the SSO proxy again right after a renewal: error shown',
        );
        _showError(context.t(ApiClient.proxyRefusedKey), proxyRefusal: true);
        return;
      }
      _lastProxyRenewal = DateTime.now();
      appLog.info(
        'web',
        'Login of the SSO proxy: renewal of the login in the system browser',
      );
      final renewed = await ref
          .read(authControllerProvider.notifier)
          .reauthenticate();
      if (!mounted) return;
      if (renewed) {
        appLog.info('web', 'Login renewed: load ${LogSanitizer.url(url)}');
        await _load(url);
      } else {
        appLog.info('web', 'Renewal not done: refusal of the SSO proxy shown');
        _showError(context.t(ApiClient.proxyRefusedKey), proxyRefusal: true);
      }
    } finally {
      _renewingProxy = false;
    }
  }

  /// The navigation wasn't a link the user tapped: a redirect of a server or a script of the page. iOS tells the
  /// type of the navigation, Android whether it is a redirect and whether the user touched the page.
  static bool _isRedirect(NavigationAction action) {
    final type = action.navigationType;
    if (type != null) {
      return type != NavigationType.LINK_ACTIVATED &&
          type != NavigationType.FORM_SUBMITTED &&
          type != NavigationType.FORM_RESUBMITTED;
    }
    return action.isRedirect == true || action.hasGesture == false;
  }

  /// How a navigation came about: a tapped link or a redirect, told differently by iOS and Android.
  static String _describe(NavigationAction action) => [
    'type: ${action.navigationType ?? '-'}',
    if (action.isRedirect != null) 'redirect: ${action.isRedirect}',
    if (action.hasGesture != null) 'gesture: ${action.hasGesture}',
    'method: ${action.request.method ?? '-'}',
  ].join(', ');

  /// The last guard against another site in the WebView: one that got past [_shouldOverrideUrlLoading] (on Android
  /// e.g. a load of the app itself or a back navigation) is stopped and opened outside the app instead, the WebView
  /// is replaced by the error.
  Future<void> _onPageCommitted(Uri url) async {
    if (!_navigation.isForeignPage(url)) return;
    appLog.warn(
      'web',
      'Another site in the WebView: ${LogSanitizer.url(url)}, stopped and opened outside the app',
    );
    await _controller?.stopLoading();
    if (!mounted) return;
    _showError(context.t('app.web.failed'));
    await launchUrl(url, mode: LaunchMode.externalApplication);
  }

  /// The names of the cookies the WebView has for the server (e.g. whether the cookie of an SSO proxy is there).
  Future<void> _logCookies(String reason) async {
    try {
      final cookies = await CookieManager.instance().getCookies(
        url: WebUri.uri(_navigation.server),
      );
      appLog.debug(
        'web',
        '$reason, cookies of the server: ${LogSanitizer.cookieNames(cookies.map((it) => it.name))}',
      );
    } on Object {
      // Only for the log.
    }
  }

  /// Client side navigations of next (pushState) don't pass [_shouldOverrideUrlLoading].
  Future<void> _onUpdateVisitedHistory(
    InAppWebViewController controller,
    WebUri? url,
  ) async {
    if (url != null && _navigation.isLoginPage(url)) _onLoginPage(url);
    if (url != null && _handOver(url) && await controller.canGoBack()) {
      // Back to the page the user came from, the native page is shown in its tab.
      await controller.goBack();
      return;
    }
    final canGoBack = await controller.canGoBack();
    if (mounted && canGoBack != _canGoBack) {
      setState(() => _canGoBack = canGoBack);
    }
  }

  /// The retry of the error view. After a refusal of the SSO proxy: a new login in the system browser (the user asked
  /// for it), then the refused page; otherwise a new web session.
  Future<void> _retry() async {
    if (!_proxyRefusalShown) {
      await _open(widget.initialPath, renew: true);
      return;
    }
    // The refused url as it was (a path rebuilt of it could resolve to another site, e.g. of `//other.host/x`).
    final path = _requestedUrl?.toString() ?? widget.initialPath;
    appLog.info('web', 'Retry after a refusal of the SSO proxy: new login');
    if (!await ref.read(authControllerProvider.notifier).reauthenticate()) {
      appLog.info('web', 'Renewal not done: refusal of the SSO proxy shown');
      if (mounted) {
        _showError(context.t(ApiClient.proxyRefusedKey), proxyRefusal: true);
      }
      return;
    }
    _lastProxyRenewal = DateTime.now();
    // The login was just renewed: a refusal now opens no second one.
    if (mounted) await _open(path, renew: true, renewLogin: false);
  }

  @override
  Widget build(BuildContext context) {
    final error = _error;
    if (error != null) {
      return ErrorView(message: error, onRetry: _retry);
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
              // Until the page paints, the background of the app (dark or light) instead of white.
              transparentBackground: true,
              underPageBackgroundColor: Theme.of(context).colorScheme.surface,
              // Only pages of the server, never files of the device.
              allowFileAccess: false,
              allowContentAccess: false,
            ),
            pullToRefreshController: _pullToRefresh,
            onWebViewCreated: (controller) => _controller = controller,
            shouldOverrideUrlLoading: _shouldOverrideUrlLoading,
            onUpdateVisitedHistory: (controller, url, _) =>
                _onUpdateVisitedHistory(controller, url),
            onProgressChanged: (_, progress) {
              setState(() => _progress = progress / 100);
            },
            onPageCommitVisible: (_, url) {
              // The page is there: further navigations are no redirects of its load.
              _serverPageLoadStart = null;
              if (url != null) _onPageCommitted(url);
            },
            onLoadStop: (_, url) {
              _serverPageLoadStart = null;
              _pullToRefresh?.endRefreshing();
              appLog.debug('web', 'Loaded ${LogSanitizer.url(url)}');
            },
            onReceivedError: (_, request, error) {
              if (request.isForMainFrame != false) {
                // A load cancelled by the next one (e.g. by the app after a renewal): that one keeps its start.
                if (error.type != WebResourceErrorType.CANCELLED) {
                  _serverPageLoadStart = null;
                }
                appLog.warn(
                  'web',
                  'Error ${error.type} loading ${LogSanitizer.url(request.url)}: ${error.description}',
                );
              }
            },
            onReceivedHttpError: (_, request, response) {
              if (request.isForMainFrame != false) {
                appLog.warn(
                  'web',
                  'HTTP ${response.statusCode} loading ${LogSanitizer.url(request.url)} '
                      '(content-type: ${response.contentType ?? '-'})',
                );
                _logCookies('HTTP ${response.statusCode}');
              }
            },
            onDownloadStartRequest: (_, request) {
              // The load became a download, its page won't stop loading.
              _serverPageLoadStart = null;
              _onDownloadStart(request);
            },
          ),
          if (_progress < 1) LinearProgressIndicator(value: _progress),
        ],
      ),
    );
  }
}
