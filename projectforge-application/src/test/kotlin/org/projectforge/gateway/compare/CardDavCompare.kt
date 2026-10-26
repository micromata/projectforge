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

package org.projectforge.gateway.compare

import org.projectforge.gateway.compare.GatewayCompare.Answer
import org.projectforge.gateway.compare.GatewayCompare.Companion.basicAuth
import org.projectforge.gateway.compare.GatewayCompare.Companion.lineDiff
import org.projectforge.gateway.compare.GatewayCompare.Companion.normalizeContentType
import org.projectforge.gateway.compare.GatewayCompare.Companion.sha256

/**
 * The requests of a CardDAV client (discovery, address book, sync, multiget, single vCard and photo), sent to the
 * main instance and to the gateway. The hrefs of the vCards differ (own ids on the gateway), so each instance gets
 * the hrefs of its own previous answer.
 *
 * ETags, CTags and sync tokens may differ between the instances, but must be stable on each of them: an address book
 * whose CTag or ETags change without a change of the addresses is downloaded completely by the clients on every sync.
 */
internal class CardDavCompare(
    private val compare: GatewayCompare,
    private val username: String,
    token: String,
    private val negativeEnabled: Boolean,
) {
    private val auth = mapOf("Authorization" to basicAuth(username, token))
    private val baseUrls = listOf(compare.mainUrl, compare.gatewayUrl)
    private val addressBookPath = "/carddav/users/$username/addressbooks/"

    fun run() {
        // Discovery
        compareRequest("carddav-options", "OPTIONS", "/carddav/", headers = auth)
        compareRequest("carddav-wellknown", "PROPFIND", "/.well-known/carddav", headers = mapOf("Depth" to "0"), body = PROPFIND_PRINCIPAL)
        compareRequest("carddav-root-principal", "PROPFIND", "/carddav/", headers = auth + ("Depth" to "0"), body = PROPFIND_PRINCIPAL)
        compareRequest("carddav-principal-home", "PROPFIND", "/carddav/principals/users/$username/", headers = auth + ("Depth" to "0"), body = PROPFIND_HOME)
        compareRequest("carddav-home", "PROPFIND", "/carddav/users/$username/", headers = auth + ("Depth" to "1"), body = PROPFIND_ADDRESSBOOK)
        val addressBook = compareRequest("carddav-addressbook", "PROPFIND", addressBookPath, headers = auth + ("Depth" to "0"), body = PROPFIND_ADDRESSBOOK)
        // Initial sync
        val sync = compareRequest("carddav-sync-initial", "REPORT", addressBookPath, headers = auth + ("Depth" to "1"), body = syncCollection(null))
        if (sync != null) {
            multiget(sync)
            getVCardAndPhoto(sync)
            syncWithToken(sync)
            checkStability(addressBook, sync)
        }
        // Refused. A wrong token is a failed login on the main instance (LoginProtection), so only on demand.
        if (negativeEnabled) {
            compare.expectRefusedOnBoth("carddav-wrong-token", "PROPFIND", compare.mainUrl + addressBookPath,
                headers = mapOf("Authorization" to basicAuth(username, "wrong-token"), "Depth" to "0"), body = PROPFIND_ADDRESSBOOK)
        }
        compare.expectRefusedOnBoth("carddav-no-auth", "PROPFIND", compare.mainUrl + addressBookPath,
            headers = mapOf("Depth" to "0"), body = PROPFIND_ADDRESSBOOK)
    }

    private fun multiget(sync: Pair<Answer, Answer>) {
        val mainHrefs = CardDavNormalizer.vcfHrefs(sync.first.text).take(MAX_MULTIGET)
        val gatewayHrefs = CardDavNormalizer.vcfHrefs(sync.second.text).take(MAX_MULTIGET)
        if (mainHrefs.isEmpty() && gatewayHrefs.isEmpty()) {
            compare.add("carddav-multiget", emptyList(), "no vCards (no favorites of $username?)")
            return
        }
        val main = compare.sendTo("main", "carddav-multiget", "REPORT", compare.mainUrl + addressBookPath, auth + ("Depth" to "1"), multigetBody(mainHrefs))
        val gateway = compare.sendTo("gateway", "carddav-multiget", "REPORT", compare.gatewayUrl + addressBookPath, auth + ("Depth" to "1"), multigetBody(gatewayHrefs))
        if (main != null && gateway != null) {
            evaluate("carddav-multiget", main, gateway, "vCards main=${mainHrefs.size}, gateway=${gatewayHrefs.size}")
        }
    }

    private fun getVCardAndPhoto(sync: Pair<Answer, Answer>) {
        // Same address on both instances: the gateway has other ids, so look up the gateway's href by the UID.
        val mainHref = CardDavNormalizer.vcfHrefs(sync.first.text).firstOrNull() ?: return
        val main = compare.sendTo("main", "carddav-get-vcard", "GET", absolute(compare.mainUrl, mainHref), auth) ?: return
        val uid = Regex("(?m)^UID:(.+)$").find(main.text.replace("\r", ""))?.groupValues?.get(1)?.trim()
        val gatewayHref = findGatewayHref(sync.second, uid) ?: run {
            compare.add("carddav-get-vcard", listOf("address with UID $uid not found on the gateway"), "GET $mainHref")
            return
        }
        val gateway = compare.sendTo("gateway", "carddav-get-vcard", "GET", absolute(compare.gatewayUrl, gatewayHref), auth) ?: return
        evaluate("carddav-get-vcard", main, gateway, "GET of one vCard")
        // Photo of this address, if any (relative url /carddav/photos/contact-<id>.<ext>)
        val photoRegex = Regex("/carddav/photos/contact-\\d+\\.\\w+")
        val mainPhoto = photoRegex.find(main.text)?.value
        val gatewayPhoto = photoRegex.find(gateway.text)?.value
        if (mainPhoto == null && gatewayPhoto == null) return
        if (mainPhoto == null || gatewayPhoto == null) {
            compare.add("carddav-get-photo", listOf("photo main=$mainPhoto, gateway=$gatewayPhoto"), "photo of $uid")
            return
        }
        val mainImage = compare.sendTo("main", "carddav-get-photo", "GET", compare.mainUrl + mainPhoto, auth) ?: return
        val gatewayImage = compare.sendTo("gateway", "carddav-get-photo", "GET", compare.gatewayUrl + gatewayPhoto, auth) ?: return
        val problems = mutableListOf<String>()
        if (mainImage.status != gatewayImage.status) problems += "status main=${mainImage.status}, gateway=${gatewayImage.status}"
        if (normalizeContentType(mainImage.header("Content-Type")) != normalizeContentType(gatewayImage.header("Content-Type"))) {
            problems += "Content-Type main=${mainImage.header("Content-Type")}, gateway=${gatewayImage.header("Content-Type")}"
        }
        if (sha256(mainImage.body) != sha256(gatewayImage.body)) problems += "image bytes differ (${mainImage.body.size} / ${gatewayImage.body.size} bytes)"
        compare.add("carddav-get-photo", problems, "photo ${mainImage.body.size} bytes")
    }

    private fun syncWithToken(sync: Pair<Answer, Answer>) {
        val mainToken = CardDavNormalizer.values(sync.first.text, "sync-token").firstOrNull()
        val gatewayToken = CardDavNormalizer.values(sync.second.text, "sync-token").firstOrNull()
        if (mainToken == null || gatewayToken == null) {
            compare.add("carddav-sync-delta", listOf("no sync-token: main=${mainToken != null}, gateway=${gatewayToken != null}"), "")
            return
        }
        val main = compare.sendTo("main", "carddav-sync-delta", "REPORT", compare.mainUrl + addressBookPath, auth + ("Depth" to "1"), syncCollection(mainToken)) ?: return
        val gateway = compare.sendTo("gateway", "carddav-sync-delta", "REPORT", compare.gatewayUrl + addressBookPath, auth + ("Depth" to "1"), syncCollection(gatewayToken)) ?: return
        evaluate("carddav-sync-delta", main, gateway,
            "changes since the initial sync: main=${CardDavNormalizer.vcfHrefs(main.text).size}, gateway=${CardDavNormalizer.vcfHrefs(gateway.text).size}")
    }

    /**
     * Requests the address book and the sync without token a second time on each instance: CTag and ETags must be
     * the same as in the first answers.
     */
    private fun checkStability(addressBook: Pair<Answer, Answer>?, sync: Pair<Answer, Answer>) {
        for ((side, baseUrl) in listOf("main" to compare.mainUrl, "gateway" to compare.gatewayUrl)) {
            val first = if (side == "main") sync.first else sync.second
            val second = compare.sendTo(side, "carddav-sync-repeat-$side", "REPORT", baseUrl + addressBookPath, auth + ("Depth" to "1"), syncCollection(null)) ?: continue
            val problems = mutableListOf<String>()
            if (etagsByHref(first) != etagsByHref(second)) problems += "ETags changed without a change of the addresses"
            val empty = etagsByHref(first).isEmpty()
            addressBook?.takeIf { !empty }?.let { ab ->
                val firstBook = if (side == "main") ab.first else ab.second
                val secondBook = compare.sendTo(side, "carddav-addressbook-repeat-$side", "PROPFIND", baseUrl + addressBookPath, auth + ("Depth" to "0"), PROPFIND_ADDRESSBOOK)
                if (secondBook != null && CardDavNormalizer.values(firstBook.text, "getctag") != CardDavNormalizer.values(secondBook.text, "getctag")) {
                    problems += "CTag changed without a change of the addresses"
                }
            }
            // The CTag of an empty address book is the current time (CardDavUtils.getEtag), not comparable.
            val details = if (empty) "empty address book, CTag not checked" else "ETags (${etagsByHref(first).size}) and CTag"
            compare.add("carddav-stable-$side", problems, "$details on $side")
        }
    }

    private fun etagsByHref(answer: Answer): Map<String, String> {
        return Regex("<([A-Za-z0-9]+:)?response>.*?</\\1response>", RegexOption.DOT_MATCHES_ALL).findAll(answer.text)
            .mapNotNull { match ->
                val href = CardDavNormalizer.values(match.value, "href").firstOrNull() ?: return@mapNotNull null
                href to (CardDavNormalizer.values(match.value, "getetag").firstOrNull() ?: "")
            }.toMap()
    }

    private fun findGatewayHref(gatewaySync: Answer, uid: String?): String? {
        uid ?: return null
        val hrefs = CardDavNormalizer.vcfHrefs(gatewaySync.text).take(MAX_MULTIGET)
        val multiget = compare.sendTo("gateway", "carddav-find-uid", "REPORT", compare.gatewayUrl + addressBookPath, auth + ("Depth" to "1"), multigetBody(hrefs)) ?: return null
        return Regex("<([A-Za-z0-9]+:)?response>.*?</\\1response>", RegexOption.DOT_MATCHES_ALL).findAll(multiget.text)
            .firstOrNull { it.value.contains("UID:$uid") }
            ?.let { CardDavNormalizer.values(it.value, "href").firstOrNull() }
    }

    private fun compareRequest(name: String, method: String, path: String, headers: Map<String, String>, body: String? = null): Pair<Answer, Answer>? {
        val answers = compare.send(name, method, compare.mainUrl + path, headers, body) ?: return null
        evaluate(name, answers.first, answers.second, "$method $path")
        return answers
    }

    private fun evaluate(name: String, main: Answer, gateway: Answer, details: String) {
        val problems = mutableListOf<String>()
        if (main.status != gateway.status) problems += "status main=${main.status}, gateway=${gateway.status}"
        for (header in listOf("DAV", "Allow", "Location")) {
            val m = main.header(header)?.let { CardDavNormalizer.normalize(it, baseUrls) }
            val g = gateway.header(header)?.let { CardDavNormalizer.normalize(it, baseUrls) }
            if (m != g) problems += "$header main=${main.header(header)}, gateway=${gateway.header(header)}"
        }
        val mainNorm = CardDavNormalizer.normalize(main.text, baseUrls)
        val gatewayNorm = CardDavNormalizer.normalize(gateway.text, baseUrls)
        compare.writeNormalized(name, mainNorm, gatewayNorm)
        val diff = lineDiff(mainNorm, gatewayNorm)
        if (diff.isNotEmpty()) problems += "content differs (${diff.size} lines)"
        compare.add(name, problems, "$details: status ${main.status}", diff)
    }

    private fun absolute(baseUrl: String, href: String): String = if (href.startsWith("http")) href else baseUrl + href

    companion object {
        private const val MAX_MULTIGET = 500

        private val PROPFIND_PRINCIPAL = """
            <?xml version="1.0" encoding="UTF-8"?>
            <d:propfind xmlns:d="DAV:">
              <d:prop>
                <d:current-user-principal/>
                <d:principal-URL/>
                <d:resourcetype/>
              </d:prop>
            </d:propfind>""".trimIndent()

        private val PROPFIND_HOME = """
            <?xml version="1.0" encoding="UTF-8"?>
            <d:propfind xmlns:d="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav">
              <d:prop>
                <card:addressbook-home-set/>
                <d:displayname/>
                <d:resourcetype/>
              </d:prop>
            </d:propfind>""".trimIndent()

        private val PROPFIND_ADDRESSBOOK = """
            <?xml version="1.0" encoding="UTF-8"?>
            <d:propfind xmlns:d="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav" xmlns:cs="http://calendarserver.org/ns/">
              <d:prop>
                <d:resourcetype/>
                <d:displayname/>
                <d:current-user-privilege-set/>
                <cs:getctag/>
                <d:sync-token/>
                <card:supported-address-data/>
              </d:prop>
            </d:propfind>""".trimIndent()

        private fun syncCollection(syncToken: String?): String = """
            <?xml version="1.0" encoding="UTF-8"?>
            <d:sync-collection xmlns:d="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav">
              <d:sync-token>${syncToken ?: ""}</d:sync-token>
              <d:sync-level>1</d:sync-level>
              <d:prop>
                <d:getetag/>
              </d:prop>
            </d:sync-collection>""".trimIndent()

        // Not by trimIndent: the interpolated hrefs would keep the indentation, and the XML declaration must come first.
        private fun multigetBody(hrefs: List<String>): String = buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<card:addressbook-multiget xmlns:d="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav">""")
            appendLine("  <d:prop><d:getetag/><card:address-data/></d:prop>")
            hrefs.forEach { appendLine("  <d:href>$it</d:href>") }
            append("</card:addressbook-multiget>")
        }
    }
}
