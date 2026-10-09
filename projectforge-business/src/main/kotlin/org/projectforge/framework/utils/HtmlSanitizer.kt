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


package org.projectforge.framework.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist
import org.projectforge.business.utils.HtmlHelper

/**
 * Sanitizes the rich text (HTML) of the next app's RichTextEditor before it is sent, e.g. as the body of a mail.
 * Allows the same as `projectforge-next/components/shared/rich-text.tsx` (DOMPurify): paragraphs, line breaks,
 * bold, italic, underline, strike-through, lists, links (http, https, mailto) and text colours.
 */
object HtmlSanitizer {
  private val safelist: Safelist = Safelist()
    .addTags("p", "br", "strong", "b", "em", "i", "u", "s", "ul", "ol", "li", "a", "span")
    .addAttributes("a", "href")
    .addProtocols("a", "href", "http", "https", "mailto")
    .addAttributes("span", "style")

  private val COLOR_REGEX = """^\s*color\s*:\s*(#[0-9a-fA-F]{3,8}|rgba?\([0-9.,\s%]+\)|[a-zA-Z]+)\s*;?\s*$""".toRegex()

  /**
   * The sanitized HTML of the given rich text. A text without any tag (plain text) keeps its line breaks as `<br>`.
   * Links open in a new window; a style other than one text colour is dropped.
   */
  fun sanitize(html: String?): String {
    if (html.isNullOrBlank()) {
      return ""
    }
    val source = if (TAG_REGEX.containsMatchIn(html)) html else HtmlHelper.escapeHtml(html, true)
    val dirty = Jsoup.parseBodyFragment(source)
    val clean: Document = Cleaner(safelist).clean(dirty)
    clean.select("span[style]").forEach { span -> cleanStyle(span) }
    clean.select("a:not([href])").unwrap() // E.g. a link with a forbidden protocol.
    clean.select("a[href]").forEach { link ->
      link.attr("target", "_blank")
      link.attr("rel", "noopener")
    }
    clean.outputSettings().prettyPrint(false)
    return clean.body().html()
  }

  private fun cleanStyle(span: Element) {
    val color = span.attr("style").split(';').map { it.trim() }.firstOrNull { COLOR_REGEX.matches(it) }
    if (color == null) {
      span.removeAttr("style")
    } else {
      span.attr("style", color)
    }
  }

  private val TAG_REGEX = """<[a-zA-Z/][^>]*>""".toRegex()
}
