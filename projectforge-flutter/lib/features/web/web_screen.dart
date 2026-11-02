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

  /// The WebView shows another site than the server: the login of an SSO proxy, see [WebNavigation.decide].
  bool _onForeignSite = false;

  /// The host of the other site the WebView went to last: its links stay in the WebView, links to a third site don't.
  String? _foreignHost;

  /// The pages came back from the login of the SSO proxy: its fresh cookies are taken once they loaded.
  bool _backFromForeignSite = false;

  /// A page of the server is loading (until `onLoadStop`): a redirect to another site now is one of the server or its
  /// proxy, not a script of a loaded page. A start without end (a load that became a download, a lost callback) counts
  /// only for [_maxLoadTime].
  DateTime? _serverPageLoadStart;

  static const _maxLoadTime = Duration(seconds: 30);

  bool get _loadingServerPage {
    final start = _serverPageLoadStart;
    return start != null && DateTime.now().difference(start) < _maxLoadTime;
  }

  /// The page of the server the current load was asked for, before any redirect: loaded again after a renewal.
  Uri? _requestedUrl;

  /// The renewal of the login for the proxy failed or was cancelled: its login happens in the WebView.
  bool _proxyLoginInWebView = false;
  bool _renewingProxy = false;
  DateTime? _lastProxyRenewal;

  /// The page of another site the WebView shows (e.g. the login of an SSO proxy): a bar names its host and offers
  /// it in the system browser, as the WebView has no address bar. Null on a page of the server.
  Uri? _foreignPage;

  /// The last page of the server, to return to from another site.
  Uri? _lastServerPage;

  /// The native page [_open] loaded itself (e.g. the editing of the business cards): it stays in the WebView until
  /// the user leaves it.
  NativePage? _requestedNative;

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

  @override
  void didUpdateWidget(WebScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    final target = widget.target;
    if (target != null && !identical(target, oldWidget.target)) {
      _open(target.path);
    }
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
      appLog.info('web', 'Load ${LogSanitizer.url(url)}');
      _requestedNative = _navigation.nativePage(url);
      if (_controller == null) {
        setState(() => _startUrl = url);
      } else {
        await _controller!.loadUrl(
          urlRequest: URLRequest(url: WebUri.uri(url)),
        );
      }
    } on ApiException catch (e) {
      appLog.warn('web', 'Pages not opened: $e');
      if (mounted) _showError(context.errorText(e));
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
    if (proxyLogin && !_proxyLoginInWebView) {
      appLog.info(
        'web',
        'Navigation to ${LogSanitizer.url(url)} → login of the SSO proxy (${_describe(action)})',
      );
      _renewForProxy();
      return NavigationActionPolicy.CANCEL;
    }
    final target = navigation.decide(
      url,
      redirect: proxyLogin,
      fromForeignSite: _onForeignSite,
      foreignHost: _foreignHost,
      tappedLink: _isTappedLink(action),
    );
    appLog.info(
      'web',
      'Navigation to ${LogSanitizer.url(url)} → ${target.name} (${_describe(action)})',
    );
    switch (target) {
      case WebNavigationTarget.webView:
        final foreign = !navigation.isSameOrigin(url);
        if (_onForeignSite && !foreign) {
          _backFromForeignSite = true;
          _proxyLoginInWebView = false;
        }
        _onForeignSite =
            foreign && (url.scheme == 'http' || url.scheme == 'https');
        _foreignHost = _onForeignSite ? url.host : null;
        if (foreign) return NavigationActionPolicy.ALLOW;
        if (navigation.isLoginPage(url)) {
          _onLoginPage(url);
          return NavigationActionPolicy.CANCEL;
        }
        if (_handOver(url)) return NavigationActionPolicy.CANCEL;
        // A redirect within the load keeps the page asked for.
        if (!_loadingServerPage || !redirect) {
          _requestedUrl = url;
          _serverPageLoadStart = DateTime.now();
        }
        return NavigationActionPolicy.ALLOW;
      case WebNavigationTarget.external:
        await _logCookies('Opened outside the app');
        await launchUrl(url, mode: LaunchMode.externalApplication);
        return NavigationActionPolicy.CANCEL;
      case WebNavigationTarget.cancel:
        return NavigationActionPolicy.CANCEL;
    }
  }

  /// The SSO proxy refused a page and redirects to its login: the login of the app is renewed in the system browser
  /// (AuthController.reauthenticate), which shares the session of the proxy's login with the browser (often no
  /// password then) and brings fresh proxy cookies, also into the WebView; then the page is loaded again. The
  /// password of the proxy's login isn't typed into a WebView of the app (RFC 8252). Only if that fails or is
  /// cancelled, or the proxy refuses again right after it, its login happens in the WebView.
  Future<void> _renewForProxy() async {
    if (_renewingProxy) return;
    final url = _requestedUrl ?? _navigation.url(WebNavigation.defaultPath);
    final last = _lastProxyRenewal;
    if (last != null && DateTime.now().difference(last) < _renewalInterval) {
      appLog.warn(
        'web',
        'Refused by the SSO proxy again right after a renewal: its login in the WebView',
      );
      _loginInWebView(url);
      return;
    }
    _renewingProxy = true;
    _lastProxyRenewal = DateTime.now();
    appLog.info(
      'web',
      'Login of the SSO proxy: renewal of the login in the system browser',
    );
    try {
      final renewed = await ref
          .read(authControllerProvider.notifier)
          .reauthenticate();
      if (!mounted) return;
      if (renewed) {
        appLog.info('web', 'Login renewed: load ${LogSanitizer.url(url)}');
        await _controller?.loadUrl(
          urlRequest: URLRequest(url: WebUri.uri(url)),
        );
      } else {
        appLog.info(
          'web',
          'Renewal not done: login of the SSO proxy in the WebView',
        );
        _loginInWebView(url);
      }
    } finally {
      _renewingProxy = false;
    }
  }

  void _loginInWebView(Uri url) {
    _proxyLoginInWebView = true;
    _controller?.loadUrl(urlRequest: URLRequest(url: WebUri.uri(url)));
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

  /// The user tapped a link: no redirect, script or form (iOS tells forms apart, Android only by their POST).
  static bool _isTappedLink(NavigationAction action) {
    if (_isRedirect(action)) return false;
    final type = action.navigationType;
    return type != null
        ? type == NavigationType.LINK_ACTIVATED
        : action.request.method?.toUpperCase() != 'POST';
  }

  /// Back from the login of the SSO proxy: the fresh cookies of the proxy in the WebView go into the session of the
  /// app, so its requests (native tabs, web session) get through again.
  Future<void> _takeProxyCookies() async {
    final serverUrl = _navigation.server.toString();
    final cookies = await ref.read(webCookiesProvider).cookies(serverUrl);
    appLog.info(
      'web',
      'Back from the login of the SSO proxy, cookies of the server: ${LogSanitizer.cookieNames(cookies.map((it) => it.name))}',
    );
    await ref.read(authControllerProvider.notifier).takeProxyCookies(cookies);
  }

  /// How a navigation came about: a tapped link or a redirect, told differently by iOS and Android.
  static String _describe(NavigationAction action) => [
    'type: ${action.navigationType ?? '-'}',
    if (action.isRedirect != null) 'redirect: ${action.isRedirect}',
    if (action.hasGesture != null) 'gesture: ${action.hasGesture}',
    'method: ${action.request.method ?? '-'}',
  ].join(', ');

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

  /// The WebView shows [url]: the bar for another site is shown or hidden.
  void _onPageShown(Uri? url) {
    if (url == null || !mounted) return;
    final http = url.scheme == 'http' || url.scheme == 'https';
    if (!http) return;
    if (_navigation.isSameOrigin(url)) {
      _lastServerPage = url;
      if (_foreignPage != null) setState(() => _foreignPage = null);
    } else if (_foreignPage != url) {
      setState(() => _foreignPage = url);
    }
  }

  Future<void> _openInBrowser(Uri url) async {
    appLog.info('web', 'Opened in the browser: ${LogSanitizer.url(url)}');
    await launchUrl(url, mode: LaunchMode.externalApplication);
  }

  /// From another site back to the last page of the server (or the start page).
  Future<void> _backToServer() async {
    final url = _lastServerPage ?? _startUrl;
    if (url == null) return;
    appLog.info('web', 'Back to the pages: ${LogSanitizer.url(url)}');
    _onForeignSite = false;
    _foreignHost = null;
    _proxyLoginInWebView = false;
    setState(() => _foreignPage = null);
    await _controller?.loadUrl(urlRequest: URLRequest(url: WebUri.uri(url)));
  }

  /// The bar above another site: its host (the pages have no address bar, the site mustn't pass for the server),
  /// "open in the browser" and "back to the pages".
  Widget _foreignSiteBar(BuildContext context) {
    final page = _foreignPage;
    if (page == null) return const SizedBox.shrink();
    final theme = Theme.of(context);
    return Material(
      color: theme.colorScheme.surfaceContainerHighest,
      child: Padding(
        padding: const EdgeInsets.only(left: 16),
        child: Row(
          children: [
            Icon(
              page.scheme == 'https' ? Icons.lock_outline : Icons.public,
              size: 18,
            ),
            const SizedBox(width: 8),
            // The whole host, scaled down if need be: cut off at its end, `pf.example.org.evil.tld` would pass for
            // the server.
            Expanded(
              child: FittedBox(
                fit: BoxFit.scaleDown,
                alignment: AlignmentDirectional.centerStart,
                child: Text(page.host, style: theme.textTheme.bodyMedium),
              ),
            ),
            IconButton(
              tooltip: context.t('app.web.openInBrowser'),
              icon: const Icon(Icons.open_in_browser),
              onPressed: () => _openInBrowser(page),
            ),
            IconButton(
              tooltip: context.t('app.web.backToPages'),
              icon: const Icon(Icons.close),
              onPressed: _backToServer,
            ),
          ],
        ),
      ),
    );
  }

  /// Client side navigations of next (pushState) don't pass [_shouldOverrideUrlLoading].
  Future<void> _onUpdateVisitedHistory(
    InAppWebViewController controller,
    WebUri? url,
  ) async {
    _onPageShown(url);
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
      // The bar stays the first child, so the WebView is never rebuilt when it shows up.
      child: Column(
        children: [
          _foreignSiteBar(context),
          Expanded(
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
                    underPageBackgroundColor: Theme.of(context)
                        .colorScheme
                        .surface,
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
                  onLoadStop: (_, url) {
                    _serverPageLoadStart = null;
                    _pullToRefresh?.endRefreshing();
                    appLog.debug('web', 'Loaded ${LogSanitizer.url(url)}');
                    _onPageShown(url);
                    if (_backFromForeignSite &&
                        url != null &&
                        _navigation.isSameOrigin(url)) {
                      _backFromForeignSite = false;
                      _takeProxyCookies();
                    }
                  },
                  onReceivedError: (_, request, error) {
                    if (request.isForMainFrame != false) {
                      _serverPageLoadStart = null;
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
          ),
        ],
      ),
    );
  }
}
