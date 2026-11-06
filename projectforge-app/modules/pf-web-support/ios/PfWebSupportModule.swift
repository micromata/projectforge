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

import ExpoModulesCore
import WebKit

/// The cookie jar and the website data of the WebViews (WKWebsiteDataStore.default(), the store react-native-webview
/// uses without incognito). @react-native-cookies/cookies would do it, but doesn't support the New Architecture.
public class PfWebSupportModule: Module {
  /// A WebView without a page, alive as long as the app: iOS hands the cookies to the network process of the
  /// WebViews, which is torn down with the last WebView; setting cookies then waited for a new one (seen in the
  /// Flutter app, `WebCookies._keepStoreAlive`).
  private var storeKeeper: WKWebView?

  public func definition() -> ModuleDefinition {
    Name("PfWebSupport")

    /// The cookies of `Set-Cookie` header values (iOS joins several into one with commas, HTTPCookie splits them)
    /// for [url] into the WebViews. Answers the names set.
    AsyncFunction("setCookies") { (url: String, setCookieHeaders: [String]) async throws -> [String] in
      guard let target = URL(string: url) else { throw InvalidUrlException(url) }
      return await self.setCookies(target, setCookieHeaders)
    }

    /// The `Cookie` header the WebViews would send to [url], null without cookies.
    AsyncFunction("cookieHeader") { (url: String) async throws -> String? in
      guard let target = URL(string: url) else { throw InvalidUrlException(url) }
      return await self.cookieHeader(target)
    }

    /// Removes all cookies, caches and web storage (localStorage, IndexedDB, …) of the WebViews (logout).
    AsyncFunction("clearAll") { () async in
      await self.clearAll()
    }
  }

  @MainActor
  private func setCookies(_ url: URL, _ setCookieHeaders: [String]) async -> [String] {
    let cookies = setCookieHeaders.flatMap {
      HTTPCookie.cookies(withResponseHeaderFields: ["Set-Cookie": $0], for: url)
    }.filter { !$0.value.isEmpty }
    let store = cookieStore()
    for cookie in cookies {
      await store.setCookie(cookie)
    }
    return cookies.map { $0.name }
  }

  @MainActor
  private func cookieHeader(_ url: URL) async -> String? {
    let matching = await cookieStore().allCookies().filter { Self.matches($0, url) }
    if matching.isEmpty { return nil }
    return matching.map { "\($0.name)=\($0.value)" }.joined(separator: "; ")
  }

  @MainActor
  private func clearAll() async {
    let store = WKWebsiteDataStore.default()
    await store.removeData(ofTypes: WKWebsiteDataStore.allWebsiteDataTypes(), modifiedSince: .distantPast)
  }

  @MainActor
  private func cookieStore() -> WKHTTPCookieStore {
    if storeKeeper == nil {
      storeKeeper = WKWebView(frame: .zero)
    }
    return WKWebsiteDataStore.default().httpCookieStore
  }

  /// RFC 6265 domain and path matching, secure cookies only over https.
  private static func matches(_ cookie: HTTPCookie, _ url: URL) -> Bool {
    guard let host = url.host?.lowercased() else { return false }
    var domain = cookie.domain.lowercased()
    let hostOnly = !domain.hasPrefix(".")
    if !hostOnly { domain.removeFirst() }
    let domainOk = host == domain || (!hostOnly && host.hasSuffix(".\(domain)"))
    let path = url.path.isEmpty ? "/" : url.path
    let pathOk = path == cookie.path || path.hasPrefix(cookie.path.hasSuffix("/") ? cookie.path : "\(cookie.path)/")
    let secureOk = !cookie.isSecure || url.scheme == "https"
    let expired = cookie.expiresDate.map { $0 < Date() } ?? false
    return domainOk && pathOk && secureOk && !expired
  }
}

internal final class InvalidUrlException: GenericException<String>, @unchecked Sendable {
  override var reason: String { "Invalid url: \(param)" }
}
