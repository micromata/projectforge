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
import org.jsoup.nodes.Element
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist
import org.projectforge.business.utils.HtmlHelper

/**
 * Sanitizes the HTML of a chat message. Allows the same as [org.projectforge.framework.utils.HtmlSanitizer] (and
 * `sanitizeChatHtml` of `projectforge-next/components/features/chat/chat-html.ts`): paragraphs, line breaks, bold,
 * italic, underline, strike-through, lists, links (http, https, mailto) and text colours, plus code (`<code>` inline,
 * `<pre><code>` as a block of several lines, without a language) and mentions:
 * `<span data-type="mention" data-id="{userId}">@{name}</span>`.
 *
 * A mention is kept only for a user accepted by the caller (the readers of the conversation); its text is taken from
 * the user, not from the request. Every other mention becomes plain text.
 */
object ChatHtmlSanitizer {
    class Result(
        /** The sanitized HTML, empty if the message has no content. */
        val html: String,
        /** The plain text, for the search and notifications. */
        val text: String,
        /** The users mentioned, in their order. */
        val mentionedIds: List<Long>,
    )

    private val safelist: Safelist = Safelist()
        .addTags("p", "br", "strong", "b", "em", "i", "u", "s", "ul", "ol", "li", "a", "span", "code", "pre")
        .addAttributes("a", "href")
        .addProtocols("a", "href", "http", "https", "mailto")
        .addAttributes("span", "style", "data-type", "data-id")

    private val COLOR_REGEX = """^\s*color\s*:\s*(#[0-9a-fA-F]{3,8}|rgba?\([0-9.,\s%]+\)|[a-zA-Z]+)\s*;?\s*$""".toRegex()

    private val TAG_REGEX = """<[a-zA-Z/][^>]*>""".toRegex()

    /**
     * @param mentionName The name of a user who may be mentioned (shown as `@name`), null if the user may not be
     * mentioned.
     */
    fun sanitize(html: String?, mentionName: (Long) -> String?): Result {
        if (html.isNullOrBlank()) {
            return Result("", "", emptyList())
        }
        val source = if (TAG_REGEX.containsMatchIn(html)) html else HtmlHelper.escapeHtml(html, true)
        val clean = Cleaner(safelist).clean(Jsoup.parseBodyFragment(source))
        val mentionedIds = mutableListOf<Long>()
        clean.select("span[data-type], span[data-id]").forEach { span ->
            val userId = span.attr("data-id").toLongOrNull()
            val name = userId?.takeIf { span.attr("data-type") == MENTION_TYPE }?.let(mentionName)
            if (userId == null || name == null) {
                span.removeAttr("data-type")
                span.removeAttr("data-id")
                return@forEach
            }
            span.removeAttr("style")
            span.text("@$name")
            if (userId !in mentionedIds) {
                mentionedIds += userId
            }
        }
        clean.select("span[style]").forEach { span -> cleanStyle(span) }
        clean.select("span:not([style]):not([data-type])").unwrap()
        clean.select("a:not([href])").unwrap() // E.g. a link with a forbidden protocol.
        clean.select("a[href]").forEach { link ->
            link.attr("target", "_blank")
            link.attr("rel", "noopener noreferrer")
        }
        clean.outputSettings().prettyPrint(false)
        val text = clean.body().text().trim()
        val hasContent = text.isNotEmpty()
        return Result(if (hasContent) clean.body().html() else "", text, mentionedIds)
    }

    private fun cleanStyle(span: Element) {
        val color = span.attr("style").split(';').map { it.trim() }.firstOrNull { COLOR_REGEX.matches(it) }
        if (color == null) {
            span.removeAttr("style")
        } else {
            span.attr("style", color)
        }
    }

    const val MENTION_TYPE = "mention"
}
