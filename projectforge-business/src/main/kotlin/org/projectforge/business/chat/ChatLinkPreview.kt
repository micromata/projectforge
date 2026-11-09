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


package org.projectforge.business.chat

import org.jsoup.Jsoup
import org.projectforge.framework.integration.PublicUrlFetcher
import java.net.URI
import java.nio.charset.Charset

/**
 * The preview of the first link of a chat message (title, description, image of the page), fetched after the message
 * was sent ([ChatLinkPreviewService]). Kept with the message as JSON ([ChatMessageDO.linkPreview]); the image is a
 * thumbnail (JPEG) in the file store.
 */
class ChatLinkPreview(
    /** The link of the message the preview is of. */
    var url: String = "",
    var title: String? = null,
    var description: String? = null,
    var siteName: String? = null,
    /** The thumbnail in the file store ([ChatLinkPreviewService.readImage]), null for none. */
    var imageFileId: String? = null,
    /** The size of the thumbnail in pixels: a large, wide one is shown as a banner, a small one (a logo) beside the text. */
    var imageWidth: Int? = null,
    var imageHeight: Int? = null,
    /** Removed by the author: nothing is shown (and fetched again) for this link. */
    var dismissed: Boolean = false,
) {
    /** The text of a page, as given by its meta data (Open Graph, Twitter card, title, description). */
    class Page(
        val title: String?,
        val description: String?,
        val siteName: String?,
        /** The absolute url of the image of the page, null for none. */
        val imageUrl: String?,
    )

    companion object {
        const val MAX_TITLE_LENGTH = 200
        const val MAX_DESCRIPTION_LENGTH = 300
        const val MAX_SITE_NAME_LENGTH = 100

        /**
         * A url in plain text, if the client sent no link (e.g. pasted into a message without rich text, or with
         * www. only, linked by the clients when shown).
         */
        private val URL_REGEX = Regex("""\b(?:https?://|www\.)[^\s<>"]+""", RegexOption.IGNORE_CASE)

        private val CHARSET_REGEX = Regex("""charset\s*=\s*"?([A-Za-z0-9._:-]+)""", RegexOption.IGNORE_CASE)

        /**
         * The first link of the sanitized html of a message ([ChatHtmlSanitizer]) to show a preview of: http or https,
         * not of code or of the given own host (a page of ProjectForge needs a login). Null if none.
         */
        fun firstUrl(html: String?, ownHost: String? = null): String? {
            if (html.isNullOrBlank()) {
                return null
            }
            val body = Jsoup.parseBodyFragment(html).body()
            body.select("code, pre").remove()
            val candidates = body.select("a[href]").map { it.attr("href") } +
                    URL_REGEX.findAll(body.text()).map { match ->
                        val url = match.value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '\'')
                        if (url.startsWith("www.", ignoreCase = true)) "https://$url" else url
                    }
            return candidates.firstOrNull { url ->
                val uri = PublicUrlFetcher.checkUrl(url) ?: return@firstOrNull false
                ownHost.isNullOrBlank() || !uri.host.equals(ownHost, ignoreCase = true)
            }?.trim()
        }

        /** The meta data of the html page, null if it has neither title nor description. */
        fun parse(body: ByteArray, contentType: String?, url: URI): Page? {
            val charset = CHARSET_REGEX.find(contentType ?: "")?.groupValues?.get(1)
                ?.takeIf { runCatching { Charset.isSupported(it) }.getOrDefault(false) }
            val doc = Jsoup.parse(body.inputStream(), charset, url.toString())
            val metas = doc.select("meta[content]")
            fun meta(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
                metas.firstOrNull { it.attr("property").equals(key, true) || it.attr("name").equals(key, true) }
                    ?.attr("content")?.let { clean(it) }
            }
            val title = (meta("og:title", "twitter:title") ?: clean(doc.title()))?.take(MAX_TITLE_LENGTH)
            val description = meta("og:description", "twitter:description", "description")?.take(MAX_DESCRIPTION_LENGTH)
            if (title == null && description == null) {
                return null
            }
            val imageUrl = meta("og:image:secure_url", "og:image", "og:image:url", "twitter:image", "twitter:image:src")
                ?.let { runCatching { url.resolve(it.replace(" ", "%20")).toString() }.getOrNull() }
                ?.takeIf { PublicUrlFetcher.checkUrl(it) != null }
            return Page(title, description, meta("og:site_name")?.take(MAX_SITE_NAME_LENGTH), imageUrl)
        }

        /** Whitespace collapsed, null if blank. */
        private fun clean(text: String?): String? {
            return text?.replace(Regex("\\s+"), " ")?.trim()?.takeIf { it.isNotEmpty() }
        }
    }
}
