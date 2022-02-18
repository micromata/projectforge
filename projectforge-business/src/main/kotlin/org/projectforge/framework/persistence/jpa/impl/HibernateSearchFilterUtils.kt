/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.jpa.impl

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory

/**
 * Some methods pulled out of BaseDao.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
object HibernateSearchFilterUtils {
  private val LOG = LoggerFactory.getLogger(HibernateSearchFilterUtils::class.java)
  private val HISTORY_SEARCH_FIELDS = arrayOf("NEW_VALUE", "OLD_VALUE")
  private val luceneReservedWords = arrayOf("AND", "OR", "NOT")

  /**
   * Additional allowed characters (not at first position) for search string modification with wildcards. Do not forget
   * to update I18nResources.properties and the user documentation after any changes. <br></br>
   * ALLOWED_CHARS = @._-+*
   */
  private const val ALLOWED_CHARS = "@._-+*"

  /**
   * Additional allowed characters (at first position) for search string modification with wildcards. Do not forget to
   * update I18nResources.properties and the user documentation after any changes. <br></br>
   * ALLOWED_BEGINNING_CHARS =
   */
  private const val ALLOWED_BEGINNING_CHARS = "@._*"

  /**
   * If the search string containts any of this escape chars, no string modification will be done.
   */
  private const val ESCAPE_CHARS = "+-"
  /**
   * If the search string starts with "'" then the searchString will be returned without leading "'". If the search
   * string consists only of alphanumeric characters and allowed chars and spaces the wild card character '*' will be
   * appended for enable ...* search. Otherwise the searchString itself will be returned.
   *
   * @param searchString
   * @param andSearch    If true then all terms must match (AND search), otherwise OR will used (default)
   * @return The modified search string or the original one if no modification was done.
   * @see .ALLOWED_CHARS
   *
   * @see .ALLOWED_BEGINNING_CHARS
   *
   * @see .ESCAPE_CHARS
   */
  /**
   * @see .modifySearchString
   */
  @JvmOverloads
  @JvmStatic
  fun modifySearchString(searchString: String?, andSearch: Boolean = false): String {
    return modifySearchString(searchString, "*", andSearch)
  }

  /**
   * If the search string starts with "'" then the searchString will be returned without leading "'". If the search
   * string consists only of alphanumeric characters and allowed chars and spaces the wild card character '*' will be
   * appended for enable ...* search. Otherwise the searchString itself will be returned.
   *
   * @param searchString
   * @param wildcardChar The used wildcard character (normally '*' or '%')
   * @param andSearch    If true then all terms must match (AND search), otherwise OR will used (default)
   * @return The modified search string or the original one if no modification was done.
   * @see .ALLOWED_CHARS
   *
   * @see .ALLOWED_BEGINNING_CHARS
   *
   * @see .ESCAPE_CHARS
   */
  @JvmStatic
  @JvmOverloads
  fun modifySearchString(
    searchString: String?,
    wildcardChar: String?,
    andSearch: Boolean,
    prependWildcard: Boolean = false
  ): String {
    searchString ?: return ""
    if (searchString.startsWith("'")) {
      return searchString.substring(1)
    }
    if (NumberUtils.isCreatable(searchString)) {
      return searchString // Numerical value
    }
    for (i in 0 until searchString.length) {
      val ch = searchString[i]
      if (!Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch)) {
        val allowed = if (i == 0) ALLOWED_BEGINNING_CHARS else ALLOWED_CHARS
        if (allowed.indexOf(ch) < 0) {
          return searchString
        }
      }
    }
    val tokens = StringUtils.split(searchString, ' ')
    val sb = StringBuilder()
    var first = true
    for (token in tokens) {
      if (first) {
        first = false
      } else {
        sb.append(" ")
      }
      if (luceneReservedWords.none { it == token }) {
        val modified = modifySearchToken(token)
        if (tokens.size > 1 && andSearch && StringUtils.containsNone(modified, ESCAPE_CHARS)) {
          sb.append("+")
        }
        if (prependWildcard && modified.length > 1 && modified[0].isLetterOrDigit()) {
          sb.append(wildcardChar)
        }
        sb.append(modified)
        if (!modified.endsWith(wildcardChar!!) && StringUtils.containsNone(modified, ESCAPE_CHARS)) {
          if (!andSearch || tokens.size > 1) { // Don't append '*' if used by SearchForm and only one token is given. It's will be appended automatically by BaseDao before the
            // search is executed.
            sb.append(wildcardChar)
          }
        }
      } else {
        sb.append(token)
      }
    }
    val str = sb.toString().trim()
    return if (luceneReservedWords.any { str.contains(it) } && !str.startsWith('(') && !str.endsWith(')')) {
      "($str)"
    } else {
      return str
    }
  }

  /**
   * Does nothing (because it seems to be work better in most times). Quotes special Lucene characters: '-' -> "\-"
   *
   * @param searchToken One word / token of the search string (one entry of StringUtils.split(searchString, ' ')).
   * @return
   */
  internal fun modifySearchToken(searchToken: String): String {
    val buf = StringBuilder()
    for (i in 0 until searchToken.length) {
      val ch = searchToken[i]
      /*
       * if (ESCAPE_CHARS.indexOf(ch) >= 0) { buf.append('\\'); }
       */
      buf.append(ch)
    }
    return buf.toString()
  }
}
