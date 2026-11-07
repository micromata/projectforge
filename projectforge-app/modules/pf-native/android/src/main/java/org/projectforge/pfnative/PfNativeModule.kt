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

package org.projectforge.pfnative

import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import expo.modules.kotlin.Promise
import expo.modules.kotlin.functions.Queues
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

/**
 * The cookie jar and the web storage of the WebViews (android.webkit.CookieManager, shared by all WebViews of the
 * app; @react-native-cookies/cookies would do it, but doesn't support the New Architecture) and the import of a
 * contact.
 */
class PfNativeModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("PfNative")

    /** The cookies of `Set-Cookie` header values for [url] into the WebViews. Answers the names set. */
    AsyncFunction("setCookies") { url: String, setCookieHeaders: List<String>, promise: Promise ->
      val manager = CookieManager.getInstance()
      val names = mutableListOf<String>()
      val headers = setCookieHeaders.filter { header ->
        val pair = header.substringBefore(';')
        pair.contains('=') && pair.substringAfter('=').isNotBlank()
      }
      if (headers.isEmpty()) {
        promise.resolve(names)
        return@AsyncFunction
      }
      var pending = headers.size
      headers.forEach { header ->
        manager.setCookie(url, header) { ok ->
          if (ok) names.add(header.substringBefore('=').trim())
          if (--pending == 0) {
            manager.flush()
            promise.resolve(names)
          }
        }
      }
    }.runOnQueue(Queues.MAIN)

    /** The `Cookie` header the WebViews would send to [url], null without cookies. */
    AsyncFunction("cookieHeader") { url: String ->
      CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() }
    }.runOnQueue(Queues.MAIN)

    /**
     * Removes all cookies and the web storage (localStorage, IndexedDB, …) of the WebViews (logout). The http cache
     * of the WebView needs a WebView instance (WebView.clearCache), see the WebView screen.
     */
    AsyncFunction("clearAll") { promise: Promise ->
      WebStorage.getInstance().deleteAllData()
      val manager = CookieManager.getInstance()
      manager.removeAllCookies {
        manager.flush()
        promise.resolve(null)
      }
    }.runOnQueue(Queues.MAIN)

    /** Apple Wallet passes exist on the iPhone only: the app doesn't offer them here. */
    AsyncFunction("addPass") { _: String, promise: Promise ->
      promise.reject("ERR_WALLET", "Wallet is not available.", null)
    }

    /**
     * Opens the vCard at [contentUri] (a content uri of the app's FileProvider) with the contacts app, which offers to
     * import it.
     */
    AsyncFunction("addContact") { contentUri: String ->
      val activity = appContext.currentActivity ?: throw IllegalStateException("No activity.")
      val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(contentUri), "text/x-vcard")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      activity.startActivity(intent)
    }.runOnQueue(Queues.MAIN)

    /** The Flutter app was never released on Android: nothing to take over (see the iOS implementation). */
    Function("readLegacyKeychain") { _: String, _: String -> null as String? }

    Function("deleteLegacyKeychain") { _: String, _: String -> }

    // No APNs on Android.
    Function("apsEnvironment") { null as String? }

    // The search in the page of Android is the script of the app (findInPage.ts).
    AsyncFunction("presentFindNavigator") { false }
  }
}
