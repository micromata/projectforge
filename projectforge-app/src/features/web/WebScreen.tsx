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

import { forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, Alert, BackHandler, Linking, Platform, Share, StyleSheet, View } from 'react-native';
import RNWebView, { WebViewMessageEvent, WebViewNavigation } from 'react-native-webview';

import PfNative from '../../../modules/pf-native/src/PfNativeModule';
import type {
  FileDownloadEvent,
  ShouldStartLoadRequest,
  WebViewErrorEvent,
  WebViewHttpErrorEvent,
} from 'react-native-webview/lib/WebViewTypes';

import { ApiException, PROXY_REFUSED_KEY } from '../../core/api/apiException';
import { authController } from '../../core/auth/authController';
import { SharedFiles } from '../../core/files/sharedFiles';
import { errorText, t } from '../../core/i18n/translations';
import { appLog, LogSanitizer } from '../../core/log/appLog';
import { useColors } from '../../core/ui/colors';
import { ErrorView } from '../../core/ui/ErrorView';
import { appVersion, platformHttp } from '../../core/platform';
import { parseUrl } from '../../core/url';
import { showWalletPass, walletAvailable, WalletException } from '../../core/wallet/walletPass';
import { FindBar, FindResult } from './FindBar';
import { findScript } from './findInPage';
import { BLOB_SCRIPT, blobFile, DownloadedFile, isWalletPass, WebDownload } from './webDownload';
import { NativePage, WebNavigation } from './webNavigation';

/** The browser functions of a tab (the menu of the home screen). */
export interface WebScreenHandle {
  /** Shows [path] (a page of the server), with the web session of the app. */
  open(path: string): void;
  goBack(): void;
  goForward(): void;
  reload(): void;
  openFind(): void;
  share(): void;
  openInBrowser(): void;
  restart(): void;
}

export interface WebScreenState {
  canGoBack: boolean;
  canGoForward: boolean;
  /** A page of the server is shown: it can be shared or opened in the browser. */
  hasPage: boolean;
}

interface Props {
  /** The page shown first (e.g. `/next/chat`), default: the start page of the server. */
  initialPath?: string;
  /** The screen is the visible tab: only it renews a login refused by the SSO proxy. */
  active: boolean;
  /**
   * The pages navigate to a page with a native tab: true if the app shows that tab instead. Not called for a page this
   * screen was asked to load itself.
   */
  onNativePage?: (page: NativePage) => boolean;
  onStateChange?: (state: WebScreenState) => void;
}

/**
 * The typings of react-native-webview default their props generic to `undefined`, and `WebViewProps & undefined` is
 * `never` with TypeScript 6: fixed to `object`.
 */
const WebView = RNWebView<object>;
type WebViewInstance = RNWebView<object>;

/** A renewal of the session at most this often, so a session that can't be opened doesn't loop. */
const RENEWAL_INTERVAL = 30_000;
/** A load that never commits (became a download, lost callback) counts as loading for this long only. */
const MAX_LOAD_TIME = 30_000;
const STALL_TIME = 20_000;

/** NSURLErrorDomain codes of a server not reached (no network, host unknown, timeout, …). */
const IOS_CONNECTION_ERRORS = new Set([-1001, -1003, -1004, -1005, -1006, -1009, -1018, -1020]);
/** android.webkit.WebViewClient.ERROR_* of a server not reached. */
const ANDROID_CONNECTION_ERRORS = new Set([-2, -6, -7, -8]);
const IOS_CANCELLED = -999;

/**
 * The pages of the server in a WebView, the port of the Flutter app's `lib/features/web/web_screen.dart`:
 * the web session comes from the app token, only pages of the own server stay in the WebView, the login of an SSO
 * proxy is renewed in the system browser, the web session is renewed when next shows its login page.
 *
 * The commit of a page (no `onPageCommitVisible` in react-native-webview) is told by a script at document start
 * (`injectedJavaScriptBeforeContentLoaded`), which posts `{pf: 'commit'}`; its url is the one of the native event,
 * not of the message. That bridge is open to every page, so a message is never more than this signal, the results
 * of the search and a file the page made itself (a blob, BLOB_SCRIPT), which only goes to the share sheet.
 */
export const WebScreen = forwardRef<WebScreenHandle, Props>(function WebScreen(
  { initialPath, active, onNativePage, onStateChange },
  ref,
) {
  const colors = useColors();
  const webView = useRef<WebViewInstance>(null);
  const [source, setSource] = useState<{ uri: string } | null>(null);
  const [webViewKey, setWebViewKey] = useState(0);
  const [error, setError] = useState<{ message: string; proxyRefusal: boolean } | null>(null);
  const [progress, setProgress] = useState(0);
  const [pageShown, setPageShown] = useState(false);
  const [stalled, setStalled] = useState(false);
  const [pullToRefresh, setPullToRefresh] = useState(false);
  const [finding, setFinding] = useState(false);
  const [findResult, setFindResult] = useState<FindResult | null>(null);

  const serverPageLoadStart = useRef<number | null>(null);
  const requestedUrl = useRef<string | null>(null);
  const shownUrl = useRef<string | null>(null);
  const lastOpened = useRef<number | null>(null);
  /** The native page this screen was asked to load itself (e.g. the web version of the business cards): not handed over. */
  const requestedNative = useRef<NativePage | null>(null);
  /** The url of the last client side navigation handed over: its repeated state changes aren't handed over again. */
  const handedOver = useRef<string | null>(null);
  const canGoBack = useRef(false);
  const lastProxyRenewal = useRef<number | null>(null);
  const renewingProxy = useRef(false);
  const openings = useRef(0);
  const stallTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const activeRef = useRef(active);
  activeRef.current = active;
  const pageShownRef = useRef(false);

  const navigation = useMemo(() => new WebNavigation(authController.getState().session!.serverUrl), []);

  const loadingServerPage = () =>
    serverPageLoadStart.current != null && Date.now() - serverPageLoadStart.current < MAX_LOAD_TIME;

  const watchStall = useCallback(() => {
    if (stallTimer.current) clearTimeout(stallTimer.current);
    setStalled(false);
    stallTimer.current = setTimeout(() => {
      if (pageShownRef.current) return;
      appLog.warn('web', `No page shown after ${STALL_TIME / 1000} s`);
      setStalled(true);
    }, STALL_TIME);
  }, []);

  const startServerPageLoad = useCallback(
    (url: string) => {
      requestedUrl.current = url;
      serverPageLoadStart.current = Date.now();
      if (!pageShownRef.current) watchStall();
    },
    [watchStall],
  );

  /** Removes the WebView (the next one starts empty, on its own start url). */
  const replaceWebView = useCallback(() => {
    if (stallTimer.current) clearTimeout(stallTimer.current);
    pageShownRef.current = false;
    setSource(null);
    setWebViewKey((it) => it + 1);
    setPageShown(false);
    setStalled(false);
    setFinding(false);
    setProgress(0);
    onStateChange?.({ canGoBack: false, canGoForward: false, hasPage: false });
  }, [onStateChange]);

  const showError = useCallback(
    (message: string, proxyRefusal = false) => {
      replaceWebView();
      setError({ message, proxyRefusal });
    },
    [replaceWebView],
  );

  /**
   * Loads [url], a page of the server, as a load of its own (see serverPageLoadStart). A new entry of the history, so
   * Back returns to the page shown before; [replace] replaces the current one (the login page of a renewal, which
   * Back would show again).
   */
  const load = useCallback(
    (url: string, replace = false) => {
      startServerPageLoad(url);
      if (webView.current && source) {
        // A new source with the same uri isn't loaded again by react-native-webview.
        const method = replace ? 'replace' : 'assign';
        webView.current.injectJavaScript(`window.location.${method}(${JSON.stringify(url)}); true;`);
      } else {
        setSource({ uri: url });
      }
    },
    [source, startServerPageLoad],
  );

  /** Loads [path] (default: the start page) with the web session of the app, a new one if [renew]. */
  const open = useCallback(
    async (path: string | undefined | null, renew = false, renewLogin = true) => {
      const opening = ++openings.current;
      setError(null);
      const sessions = authController.webSessions;
      if (sessions == null) return;
      try {
        const startPath = await (renew ? sessions.renew(renewLogin) : sessions.ensure());
        if (opening !== openings.current) return;
        if (renew) lastOpened.current = Date.now();
        const url = navigation.pageUrl(path ?? startPath);
        if (url == null) {
          appLog.warn('web', `Not a page of the server: ${LogSanitizer.url(path ?? startPath)}, not loaded`);
          showError(t('app.web.failed'));
          return;
        }
        appLog.info('web', `Load ${LogSanitizer.url(url)}`);
        requestedNative.current = navigation.nativePage(url);
        load(url.href, renew);
      } catch (e) {
        appLog.warn('web', `Pages not opened: ${String(e)}`);
        if (opening !== openings.current) return;
        const proxy = e instanceof ApiException && e.messageKey === PROXY_REFUSED_KEY;
        showError(proxy ? t('app.proxy.refused') : errorText(e), proxy);
      }
    },
    [load, navigation, showError],
  );

  const restart = useCallback(
    (renew: boolean) => {
      const path = shownUrl.current ?? requestedUrl.current ?? initialPath;
      appLog.info('web', `Restart of the WebView${renew ? ' with a new web session' : ''}`);
      replaceWebView();
      void open(path, renew);
    },
    [initialPath, open, replaceWebView],
  );

  useEffect(() => {
    void open(initialPath);
    return () => {
      if (stallTimer.current) clearTimeout(stallTimer.current);
    };
    // Only on mount: later loads are asked for by the handle.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /** The pages show the login: the web session is gone. Renewed once per RENEWAL_INTERVAL, else an error. */
  const onLoginPage = (url: string) => {
    if (lastOpened.current != null && Date.now() - lastOpened.current < RENEWAL_INTERVAL) {
      appLog.warn('web', 'Login page again right after a renewal of the web session: error shown');
      showError(t('app.web.failed'));
      return;
    }
    appLog.info('web', 'Login page of the server: renewal of the web session');
    void open(navigation.returnPath(url), true);
  };

  /**
   * The SSO proxy refused a page and redirects to its login: the login of the app is renewed in the system browser,
   * then the page is loaded again. Only if the proxy refuses the app too (ApiClient.proxyRefuses): another site the
   * server redirects to is opened outside the app.
   */
  const renewForProxy = async (target: string) => {
    if (renewingProxy.current) return;
    renewingProxy.current = true;
    const url = requestedUrl.current ?? navigation.url(WebNavigation.defaultPath).href;
    try {
      const api = authController.api;
      if (api == null || !(await api.proxyRefuses())) {
        appLog.warn(
          'web',
          `Redirected to ${LogSanitizer.url(target)}, but the SSO proxy lets the app through: opened outside the app`,
        );
        await Linking.openURL(target);
        return;
      }
      if (!activeRef.current) {
        appLog.info('web', 'Refused by the SSO proxy in a tab not shown: error shown, no renewal');
        showError(t('app.proxy.refused'), true);
        return;
      }
      if (lastProxyRenewal.current != null && Date.now() - lastProxyRenewal.current < RENEWAL_INTERVAL) {
        appLog.warn('web', 'Refused by the SSO proxy again right after a renewal: error shown');
        showError(t('app.proxy.refused'), true);
        return;
      }
      lastProxyRenewal.current = Date.now();
      appLog.info('web', 'Login of the SSO proxy: renewal of the login in the system browser');
      if (await authController.reauthenticate()) {
        appLog.info('web', `Login renewed: load ${LogSanitizer.url(url)}`);
        load(url);
      } else {
        appLog.info('web', 'Renewal not done: refusal of the SSO proxy shown');
        showError(t('app.proxy.refused'), true);
      }
    } finally {
      renewingProxy.current = false;
    }
  };

  const handOver = (url: string) => {
    const page = navigation.nativePage(url);
    if (page === requestedNative.current) return false;
    requestedNative.current = null;
    return page != null && (onNativePage?.(page) ?? false);
  };

  /**
   * The navigation wasn't a link the user tapped: a redirect of a server or a script of the page. iOS tells the type
   * of the navigation; Android only with the patch of react-native-webview (isRedirect, hasGesture), without it every
   * navigation is `other` and counts as a redirect.
   */
  const isRedirectNavigation = (request: ShouldStartLoadRequest & { isRedirect?: boolean; hasGesture?: boolean }) => {
    if (Platform.OS === 'android') {
      if (request.isRedirect != null || request.hasGesture != null) {
        return request.isRedirect === true || request.hasGesture === false;
      }
      return true;
    }
    return !['click', 'formsubmit', 'formresubmit'].includes(request.navigationType);
  };

  const describe = (request: ShouldStartLoadRequest & { isRedirect?: boolean; hasGesture?: boolean }) =>
    [
      `type: ${request.navigationType ?? '-'}`,
      request.isRedirect != null ? `redirect: ${request.isRedirect}` : null,
      request.hasGesture != null ? `gesture: ${request.hasGesture}` : null,
      `top: ${request.isTopFrame}`,
      `loading: ${loadingServerPage()}`,
    ]
      .filter(Boolean)
      .join(', ');

  /** Synchronous: the answer goes back to the WebView at once (Android allows the load after 250 ms without it). */
  const onShouldStartLoadWithRequest = (request: ShouldStartLoadRequest): boolean => {
    try {
      const url = request.url;
      if (request.isTopFrame === false) return true;
      const redirect = isRedirectNavigation(request);
      const behindProxy = Object.keys(authController.getState().session?.proxyCookies ?? {}).length > 0;
      if (navigation.isProxyLogin(url, { redirect, loadingServerPage: loadingServerPage(), behindProxy })) {
        appLog.info('web', `Navigation to ${LogSanitizer.url(url)} → login of the SSO proxy? (${describe(request)})`);
        serverPageLoadStart.current = null;
        void renewForProxy(url);
        return false;
      }
      const target = navigation.decide(url);
      appLog.info('web', `Navigation to ${LogSanitizer.url(url)} → ${target} (${describe(request)})`);
      switch (target) {
        case 'webView':
          if (!navigation.isSameOrigin(url)) {
            serverPageLoadStart.current = null;
            return true;
          }
          if (navigation.isLoginPage(url)) {
            if (redirect) serverPageLoadStart.current = null;
            onLoginPage(url);
            return false;
          }
          if (handOver(url)) {
            if (redirect) serverPageLoadStart.current = null;
            return false;
          }
          if (!loadingServerPage() || !redirect) startServerPageLoad(url);
          return true;
        case 'external':
          if (redirect) serverPageLoadStart.current = null;
          void Linking.openURL(url).catch((e) => appLog.warn('web', `Not opened outside the app: ${String(e)}`));
          return false;
        case 'cancel':
          if (redirect) serverPageLoadStart.current = null;
          return false;
      }
    } catch (e) {
      appLog.error('web', 'Navigation refused after an error', e);
      return false;
    }
  };

  /** The last guard against another site in the WebView: stopped, error shown, opened outside the app. */
  const onPageCommitted = (url: string) => {
    serverPageLoadStart.current = null;
    if (navigation.isForeignPage(url)) {
      appLog.warn('web', `Another site in the WebView: ${LogSanitizer.url(url)}, stopped and opened outside the app`);
      webView.current?.stopLoading();
      showError(t('app.web.failed'));
      void Linking.openURL(url);
      return;
    }
    if (!pageShownRef.current && navigation.isSameOrigin(url)) {
      pageShownRef.current = true;
      if (stallTimer.current) clearTimeout(stallTimer.current);
      setPageShown(true);
      setStalled(false);
    }
  };

  const onMessage = (event: WebViewMessageEvent) => {
    let data: { pf?: string; count?: number; index?: number; supported?: boolean; name?: unknown; mime?: unknown; data?: unknown };
    try {
      data = JSON.parse(event.nativeEvent.data);
    } catch {
      return;
    }
    if (data?.pf === 'commit') {
      onPageCommitted(event.nativeEvent.url);
    } else if (data?.pf === 'find') {
      setFindResult({ count: data.count ?? 0, index: data.index ?? -1, supported: data.supported !== false });
    } else if (data?.pf === 'blob') {
      onBlobDownload(event.nativeEvent.url, data);
    }
  };

  /** A file the page made itself (an export of next): offered as a download, only from a page of the server. */
  const onBlobDownload = (pageUrl: string, message: { name?: unknown; mime?: unknown; data?: unknown }) => {
    if (!navigation.isSameOrigin(pageUrl)) {
      appLog.warn('web', `File of another site refused: ${LogSanitizer.url(pageUrl)}`);
      return;
    }
    const file = blobFile(message);
    if (file == null) {
      appLog.warn('web', 'File of the page not taken (none or too large)');
      Alert.alert(t('app.download.failed'));
      return;
    }
    void offer(file, 'File of the page');
  };

  /** Also the client side navigations of next (pushState). */
  const onNavigationStateChange = (state: WebViewNavigation) => {
    const url = state.url;
    if (navigation.isLoginPage(url)) {
      onLoginPage(url);
    } else if (navigation.isSameOrigin(url)) {
      if (shownUrl.current != null && parseUrl(url)?.pathname !== parseUrl(shownUrl.current)?.pathname) closeFind();
      shownUrl.current = url;
    }
    setPullToRefresh(!navigation.isNextPage(url));
    if (!state.loading) serverPageLoadStart.current = null;
    if (url !== handedOver.current) {
      handedOver.current = null;
      if (state.canGoBack && navigation.isSameOrigin(url) && handOver(url)) {
        // A link of next to a page with a tab of its own: shown there, back to the page the user came from.
        handedOver.current = url;
        webView.current?.goBack();
        return;
      }
    }
    canGoBack.current = state.canGoBack;
    onStateChange?.({
      canGoBack: state.canGoBack,
      canGoForward: state.canGoForward,
      hasPage: shownUrl.current != null,
    });
  };

  const onError = (event: WebViewErrorEvent) => {
    const { code, description, url } = event.nativeEvent;
    if (code === IOS_CANCELLED && Platform.OS === 'ios') return;
    serverPageLoadStart.current = null;
    appLog.warn('web', `Error ${code} loading ${LogSanitizer.url(url)}: ${description}`);
    const connection = Platform.OS === 'ios' ? IOS_CONNECTION_ERRORS : ANDROID_CONNECTION_ERRORS;
    if (connection.has(code)) showError(t('app.web.offline'));
  };

  const onHttpError = (event: WebViewHttpErrorEvent) => {
    appLog.warn('web', `HTTP ${event.nativeEvent.statusCode} loading ${LogSanitizer.url(event.nativeEvent.url)}`);
  };

  /** A download of the pages: loaded by the app (session cookie, own server only) and offered in the share sheet. */
  const onFileDownload = async (event: FileDownloadEvent) => {
    serverPageLoadStart.current = null;
    const nativeEvent = event.nativeEvent as FileDownloadEvent['nativeEvent'] & { suggestedFilename?: string };
    try {
      const download = await new WebDownload(
        navigation,
        (url) => authController.webCookies.cookieHeader(url),
        platformHttp,
      ).load(nativeEvent.downloadUrl, nativeEvent.suggestedFilename);
      await offer(download, 'Downloaded');
    } catch (e) {
      appLog.warn('web', `Download failed: ${String(e)}`);
      Alert.alert(t('app.download.failed'));
    }
  };

  /** A Wallet pass (of a business card, "Add to Wallet" of the page) into the Wallet, any other file to share. */
  const offer = async (file: DownloadedFile, what: string) => {
    try {
      if (walletAvailable && isWalletPass(file.mimeType, file.name)) {
        appLog.info('web', `${what}: ${file.bytes.length} bytes: Wallet`);
        await showWalletPass(file.bytes);
        return;
      }
      appLog.info('web', `${what}: ${file.bytes.length} bytes: share sheet`);
      await SharedFiles.share(file.name, file.bytes, file.mimeType);
    } catch (e) {
      appLog.warn('web', `${what} not offered: ${String(e)}`);
      Alert.alert(e instanceof WalletException ? e.message : t('app.download.failed'));
    }
  };

  const retry = async () => {
    if (!error?.proxyRefusal) {
      await open(requestedUrl.current ?? shownUrl.current ?? initialPath, true);
      return;
    }
    const path = requestedUrl.current ?? initialPath;
    appLog.info('web', 'Retry after a refusal of the SSO proxy: new login');
    if (!(await authController.reauthenticate())) {
      appLog.info('web', 'Renewal not done: refusal of the SSO proxy shown');
      showError(t('app.proxy.refused'), true);
      return;
    }
    lastProxyRenewal.current = Date.now();
    await open(path, true, false);
  };

  // The hardware back of Android goes back in the pages of the tab shown.
  useEffect(() => {
    if (!active || Platform.OS !== 'android') return;
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      if (!canGoBack.current) return false;
      webView.current?.goBack();
      return true;
    });
    return () => subscription.remove();
  }, [active]);

  // The search belongs to the tab shown: closed once another tab is shown.
  const [shownActive, setShownActive] = useState(active);
  if (shownActive !== active) {
    setShownActive(active);
    if (!active) setFinding(false);
  }
  useEffect(() => {
    if (!active) webView.current?.injectJavaScript(findScript('', 'clear'));
  }, [active]);

  const closeFind = () => {
    if (!finding) return;
    webView.current?.injectJavaScript(findScript('', 'clear'));
    setFinding(false);
  };

  useImperativeHandle(ref, () => ({
    open: (path: string) => void open(path),
    goBack: () => webView.current?.goBack(),
    goForward: () => webView.current?.goForward(),
    reload: () => {
      if (shownUrl.current && navigation.isSameOrigin(shownUrl.current)) startServerPageLoad(shownUrl.current);
      webView.current?.reload();
    },
    // iOS: the find navigator of WebKit (findInPage.ts marked wrong there), else the bar with the script.
    openFind: () => {
      const showBar = () => {
        setFindResult(null);
        setFinding(true);
      };
      if (Platform.OS !== 'ios') return showBar();
      PfNative.presentFindNavigator()
        .then((shown) => shown || showBar())
        .catch(showBar);
    },
    share: () => {
      if (shownUrl.current) void Share.share({ url: shownUrl.current, message: shownUrl.current });
    },
    openInBrowser: () => {
      if (shownUrl.current) void Linking.openURL(shownUrl.current);
    },
    restart: () => restart(true),
  }));

  if (error) {
    return <ErrorView message={error.message} onRetry={retry} />;
  }
  if (source == null) {
    return (
      <View style={styles.center}>
        <ActivityIndicator />
      </View>
    );
  }
  return (
    <View style={styles.fill}>
      <WebView
        key={webViewKey}
        ref={webView}
        source={source}
        // Every url is decided by onShouldStartLoadWithRequest. Without '*' react-native-webview opens any url of
        // another scheme itself (Linking.openURL), before asking it.
        originWhitelist={['*']}
        onShouldStartLoadWithRequest={onShouldStartLoadWithRequest}
        // Marks the app for next (lib/app-mode.ts), which then e.g. hides its logout.
        applicationNameForUserAgent={`ProjectForgeApp/${appVersion}`}
        injectedJavaScriptBeforeContentLoaded={COMMIT_SCRIPT + BLOB_SCRIPT}
        injectedJavaScriptBeforeContentLoadedForMainFrameOnly
        onMessage={onMessage}
        onNavigationStateChange={onNavigationStateChange}
        onLoadProgress={(event) => setProgress(event.nativeEvent.progress)}
        onLoadEnd={() => (serverPageLoadStart.current = null)}
        onError={onError}
        renderError={() => <View />}
        onHttpError={onHttpError}
        onFileDownload={onFileDownload}
        // The content process is gone (iOS: killed in the background; Android: crashed): a new WebView.
        onContentProcessDidTerminate={() => {
          appLog.warn('web', 'Content process of the WebView terminated');
          restart(false);
        }}
        onRenderProcessGone={(event) => {
          appLog.warn('web', `Render process of the WebView gone (crashed: ${event.nativeEvent.didCrash})`);
          restart(false);
        }}
        allowsBackForwardNavigationGestures
        javaScriptCanOpenWindowsAutomatically={false}
        setSupportMultipleWindows={false}
        // Only pages of the server, never files of the device.
        allowFileAccess={false}
        allowFileAccessFromFileURLs={false}
        allowUniversalAccessFromFileURLs={false}
        mixedContentMode="never"
        // The pages of next bring their own pull to refresh; iOS only (react-native-webview has none on Android).
        pullToRefreshEnabled={pullToRefresh}
        webviewDebuggingEnabled={__DEV__}
        // No white page before the first one is painted (dark theme).
        style={[styles.fill, { backgroundColor: colors.surface }]}
      />
      {progress < 1 && <View style={[styles.progress, { width: `${Math.round(progress * 100)}%` }]} />}
      {finding && (
        <FindBar
          result={findResult}
          onSearch={(query, direction) => webView.current?.injectJavaScript(findScript(query, direction))}
          onClose={closeFind}
        />
      )}
      {stalled ? (
        <View style={StyleSheet.absoluteFill}>
          <ErrorView message={t('app.web.stalled')} onRetry={() => restart(true)} retryLabel={t('app.web.reload')} />
        </View>
      ) : (
        !pageShown && (
          <View style={[StyleSheet.absoluteFill, styles.center]} pointerEvents="none">
            <ActivityIndicator />
          </View>
        )
      )}
    </View>
  );
});

/** Posts the commit of each page (main frame) at document start, see WebScreen. */
const COMMIT_SCRIPT = `(function () {
  window.ReactNativeWebView && window.ReactNativeWebView.postMessage('{"pf":"commit"}');
})(); true;`;

const styles = StyleSheet.create({
  fill: { flex: 1 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  surface: { backgroundColor: '#fff' },
  progress: { position: 'absolute', top: 0, left: 0, height: 2, backgroundColor: '#1976d2' },
  errorText: { fontSize: 16, textAlign: 'center', marginBottom: 16 },
  button: { backgroundColor: '#1976d2', borderRadius: 6, paddingHorizontal: 20, paddingVertical: 10 },
  buttonText: { color: '#fff', fontSize: 16 },
});
