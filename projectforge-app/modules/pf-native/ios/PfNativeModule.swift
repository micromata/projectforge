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

import Contacts
import ContactsUI
import ExpoModulesCore
import PassKit
import WebKit

/// The cookie jar and the website data of the WebViews (WKWebsiteDataStore.default(), the store react-native-webview
/// uses without incognito; @react-native-cookies/cookies would do it, but doesn't support the New Architecture), the
/// Apple Wallet, the form of a new contact, the key chain items of the Flutter app and the APNs environment of the signing.
public class PfNativeModule: Module {
  /// A WebView without a page, alive as long as the app: iOS hands the cookies to the network process of the
  /// WebViews, which is torn down with the last WebView; setting cookies then waited for a new one (seen in the
  /// Flutter app, `WebCookies._keepStoreAlive`).
  private var storeKeeper: WKWebView?

  public func definition() -> ModuleDefinition {
    Name("PfNative")

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

    /// Shows the system sheet to add the pass (a `.pkpass` file at [fileUrl]) to the Wallet.
    AsyncFunction("addPass") { (fileUrl: String) async throws in
      guard let url = URL(string: fileUrl), let data = try? Data(contentsOf: url) else {
        throw WalletException("The pass can't be read.")
      }
      try await self.addPass(data)
    }

    /// Shows the form of the system for a new contact, filled with the vCard at [fileUrl]: the user saves it to the
    /// own contacts or cancels. Needs no access to the contacts (the form saves it itself).
    AsyncFunction("addContact") { (fileUrl: String) async throws in
      // The url of expo-file-system may come without percent encoding (a name with spaces).
      let url = URL(string: fileUrl) ?? URL(fileURLWithPath: fileUrl.replacingOccurrences(of: "file://", with: ""))
      let data: Data
      do {
        data = try Data(contentsOf: url)
      } catch {
        throw ContactException("The vCard file can't be read: \(error.localizedDescription)")
      }
      try await self.addContact(Self.contact(of: data))
    }

    /// A key chain item of the Flutter app (flutter_secure_storage: generic password, service [service], account
    /// [key] as a string), null if there is none. expo-secure-store can't read these: it adds kSecAttrGeneric to its
    /// queries and stores the account as data.
    Function("readLegacyKeychain") { (service: String, key: String) -> String? in
      var query = Self.legacyQuery(service, key)
      query[kSecMatchLimit as String] = kSecMatchLimitOne
      query[kSecReturnData as String] = kCFBooleanTrue
      var result: AnyObject?
      guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess, let data = result as? Data else {
        return nil
      }
      return String(data: data, encoding: .utf8)
    }

    /// Deletes a key chain item of the Flutter app, once taken over.
    Function("deleteLegacyKeychain") { (service: String, key: String) in
      SecItemDelete(Self.legacyQuery(service, key) as CFDictionary)
    }

    /// `SANDBOX` or `PRODUCTION`, as the app is signed: read from the provisioning profile (`aps-environment`), which
    /// a build started from Xcode has even in release mode. Builds from the store or TestFlight have none: null.
    Function("apsEnvironment") { () -> String? in
      Self.apsEnvironment()
    }

    /// The find navigator of WebKit (as in Safari) for the WebView on screen. Answers false if there is none.
    AsyncFunction("presentFindNavigator") { () async -> Bool in
      await self.presentFindNavigator()
    }
  }

  /// The search in the page by WebKit itself: the marks of the CSS Custom Highlight API (the script of the app,
  /// findInPage.ts) were painted wrong on iOS, whole words marked, the marks of earlier queries kept.
  @MainActor
  private func presentFindNavigator() -> Bool {
    guard let window = UIApplication.shared.connectedScenes
      .compactMap({ ($0 as? UIWindowScene)?.keyWindow }).first,
      let webView = Self.visibleWebView(in: window, window: window)
    else {
      return false
    }
    webView.isFindInteractionEnabled = true
    webView.findInteraction?.presentFindNavigator(showingReplace: false)
    return true
  }

  /// The WebView of the tab shown: the one covering most of the window (the other tabs are hidden or off screen).
  @MainActor
  private static func visibleWebView(in view: UIView, window: UIWindow) -> WKWebView? {
    var best: (WKWebView, CGFloat)?
    func visit(_ view: UIView) {
      if view.isHidden || view.alpha < 0.01 { return }
      if let webView = view as? WKWebView {
        let visible = webView.convert(webView.bounds, to: window).intersection(window.bounds)
        let area = visible.isNull ? 0 : visible.width * visible.height
        if area > 0 && area > (best?.1 ?? 0) { best = (webView, area) }
        return
      }
      view.subviews.forEach(visit)
    }
    visit(view)
    return best?.0
  }

  private static func apsEnvironment() -> String? {
    guard let url = Bundle.main.url(forResource: "embedded", withExtension: "mobileprovision"),
      let data = try? Data(contentsOf: url),
      let text = String(data: data, encoding: .isoLatin1),
      let key = text.range(of: "<key>aps-environment</key>"),
      let open = text.range(of: "<string>", range: key.upperBound..<text.endIndex),
      let close = text.range(of: "</string>", range: open.upperBound..<text.endIndex)
    else {
      return nil
    }
    return text[open.upperBound..<close.lowerBound] == "development" ? "SANDBOX" : "PRODUCTION"
  }

  private static func legacyQuery(_ service: String, _ key: String) -> [String: Any] {
    [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrService as String: service,
      kSecAttrAccount as String: key,
    ]
  }

  @MainActor
  private func addPass(_ data: Data) throws {
    guard PKAddPassesViewController.canAddPasses() else { throw WalletException("Wallet is not available.") }
    let pass: PKPass
    do {
      pass = try PKPass(data: data)
    } catch {
      throw WalletException(error.localizedDescription)
    }
    if PKPassLibrary().containsPass(pass) { throw WalletException("The pass is already in the Wallet.") }
    guard let controller = PKAddPassesViewController(pass: pass),
      var top = UIApplication.shared.connectedScenes
        .compactMap({ ($0 as? UIWindowScene)?.keyWindow?.rootViewController }).first
    else {
      throw WalletException("The pass can't be shown.")
    }
    while let presented = top.presentedViewController { top = presented }
    top.present(controller, animated: true)
  }

  /// The contact of the vCard [data]. iOS refuses some vCards without saying why (an empty list, or an error for a
  /// NOTE without the entitlement for notes, seen with a PGP KEY and a large PHOTO): then it is read once more with
  /// only the properties it knows, and the photo is set on its own.
  private static func contact(of data: Data) throws -> CNContact {
    var reason = "The vCard has no contact."
    do {
      if let contact = try CNContactVCardSerialization.contacts(with: data).first { return contact }
    } catch {
      reason = "The vCard can't be read: \(error.localizedDescription)"
    }
    guard let text = String(data: data, encoding: .utf8) else { throw ContactException(reason) }
    let (lines, photo) = Self.unfolded(text)
    let kept = lines.filter { line in
      let name = line.prefix { $0 != ":" && $0 != ";" }.uppercased()
      return !(name.hasPrefix("X-") || ["NOTE", "KEY", "PHOTO"].contains(name))
    }
    guard let contact = try? CNContactVCardSerialization.contacts(with: Data(kept.joined(separator: "\r\n").utf8)).first
    else {
      throw ContactException("\(reason) \(Self.diagnose(lines))")
    }
    guard let photo else { return contact }
    let withPhoto = contact.mutableCopy() as! CNMutableContact
    withPhoto.imageData = photo
    return withPhoto
  }

  /// For the log: the size of the vCard, its properties and the ones without which iOS reads it (each left out on its
  /// own); also written to the system log (`PfNative` in Console.app).
  private static func diagnose(_ lines: [String]) -> String {
    let names = lines.map { String($0.prefix { $0 != ":" && $0 != ";" }).uppercased() }
    let distinct = names.reduce(into: [String]()) { if !$0.contains($1) { $0.append($1) } }
    let parses = { (kept: [String]) -> Int in
      (try? CNContactVCardSerialization.contacts(with: Data(kept.joined(separator: "\r\n").utf8)).count) ?? -1
    }
    let culprits = distinct.filter { name in
      !["BEGIN", "END", "VERSION"].contains(name) && parses(zip(lines, names).filter { $0.1 != name }.map { $0.0 }) > 0
    }
    let minimal = parses(zip(lines, names).filter { ["BEGIN", "END", "VERSION", "N", "FN"].contains($0.1) }.map { $0.0 })
    let text = "[\(lines.count) lines, \(lines.reduce(0) { $0 + $1.utf8.count }) bytes; properties: "
      + "\(distinct.joined(separator: ",")); read without: \(culprits.isEmpty ? "none" : culprits.joined(separator: ","))"
      + "; with N and FN only: \(minimal)]"
    NSLog("PfNative addContact: %@", text)
    return text
  }

  /// The lines of the vCard [text] with their folded continuations joined, and the bytes of its embedded PHOTO.
  private static func unfolded(_ text: String) -> ([String], Data?) {
    var lines: [String] = []
    for line in text.components(separatedBy: "\r\n") {
      if (line.hasPrefix(" ") || line.hasPrefix("\t")), let last = lines.popLast() {
        lines.append(last + line.dropFirst())
      } else if !line.isEmpty {
        lines.append(line)
      }
    }
    let photo = lines.first { $0.uppercased().hasPrefix("PHOTO;") && $0.uppercased().contains("ENCODING=B") }
      .flatMap { $0.split(separator: ":", maxSplits: 1).last }
      .flatMap { Data(base64Encoded: String($0)) }
    return (lines, photo)
  }

  @MainActor
  private func addContact(_ contact: CNContact) throws {
    guard var top = UIApplication.shared.connectedScenes
      .compactMap({ ($0 as? UIWindowScene)?.keyWindow?.rootViewController }).first
    else {
      throw ContactException("The form can't be shown.")
    }
    while let presented = top.presentedViewController { top = presented }
    let form = CNContactViewController(forNewContact: contact)
    let delegate = ContactFormDelegate()
    form.delegate = delegate
    // The form keeps its delegate weak: it lives as long as the form.
    objc_setAssociatedObject(form, &ContactFormDelegate.key, delegate, .OBJC_ASSOCIATION_RETAIN)
    top.present(UINavigationController(rootViewController: form), animated: true)
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

/// A pass the Wallet refused or couldn't show, with the reason of iOS.
internal final class WalletException: GenericException<String>, @unchecked Sendable {
  override var reason: String { param }
}

internal final class ContactException: GenericException<String>, @unchecked Sendable {
  override var reason: String { param }
}

/// Closes the form of a new contact when the user saved or cancelled it (the form doesn't close itself when presented).
private final class ContactFormDelegate: NSObject, CNContactViewControllerDelegate {
  static var key = 0

  func contactViewController(_ viewController: CNContactViewController, didCompleteWith contact: CNContact?) {
    viewController.dismiss(animated: true)
  }
}
