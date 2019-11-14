/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.jpa.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some methods pulled out of BaseDao.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class HibernateSearchFilterUtils {
  private static final Logger LOG = LoggerFactory.getLogger(HibernateSearchFilterUtils.class);
  private static final String[] HISTORY_SEARCH_FIELDS = {"NEW_VALUE", "OLD_VALUE"};
  private static final String[] luceneReservedWords = {"AND", "OR", "NOT"};

  /**
   * Additional allowed characters (not at first position) for search string modification with wildcards. Do not forget
   * to update I18nResources.properties and the user documentation after any changes. <br/>
   * ALLOWED_CHARS = @._-+*
   */
  private static final String ALLOWED_CHARS = "@._-+*";

  /**
   * Additional allowed characters (at first position) for search string modification with wildcards. Do not forget to
   * update I18nResources.properties and the user documentation after any changes. <br/>
   * ALLOWED_BEGINNING_CHARS =
   */
  private static final String ALLOWED_BEGINNING_CHARS = "@._*";
  /**
   * If the search string containts any of this escape chars, no string modification will be done.
   */
  private static final String ESCAPE_CHARS = "+-";

  /**
   * If the search string starts with "'" then the searchString will be returned without leading "'". If the search
   * string consists only of alphanumeric characters and allowed chars and spaces the wild card character '*' will be
   * appended for enable ...* search. Otherwise the searchString itself will be returned.
   *
   * @param searchString
   * @param andSearch    If true then all terms must match (AND search), otherwise OR will used (default)
   * @return The modified search string or the original one if no modification was done.
   * @see #ALLOWED_CHARS
   * @see #ALLOWED_BEGINNING_CHARS
   * @see #ESCAPE_CHARS
   */
  public static String modifySearchString(final String searchString, final boolean andSearch) {
    return modifySearchString(searchString, "*", andSearch);
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
   * @see #ALLOWED_CHARS
   * @see #ALLOWED_BEGINNING_CHARS
   * @see #ESCAPE_CHARS
   */
  public static String modifySearchString(final String searchString, String wildcardChar, final boolean andSearch) {
    if (searchString == null) {
      return "";
    }
    if (searchString.startsWith("'")) {
      return searchString.substring(1);
    }
    if (NumberUtils.isCreatable(searchString)) {
      return searchString; // Numerical value
    }
    for (int i = 0; i < searchString.length(); i++) {
      final char ch = searchString.charAt(i);
      if (!Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch)) {
        final String allowed = (i == 0) ? ALLOWED_BEGINNING_CHARS : ALLOWED_CHARS;
        if (allowed.indexOf(ch) < 0) {
          return searchString;
        }
      }
    }
    final String[] tokens = StringUtils.split(searchString, ' ');
    final StringBuilder buf = new StringBuilder();
    boolean first = true;
    for (final String token : tokens) {
      if (first) {
        first = false;
      } else {
        buf.append(" ");
      }
      if (!ArrayUtils.contains(luceneReservedWords, token)) {
        final String modified = modifySearchToken(token);
        if (tokens.length > 1 && andSearch && StringUtils.containsNone(modified, ESCAPE_CHARS)) {
          buf.append("+");
        }
        buf.append(modified);
        if (!modified.endsWith(wildcardChar) && StringUtils.containsNone(modified, ESCAPE_CHARS)) {
          if (!andSearch || tokens.length > 1) {
            // Don't append '*' if used by SearchForm and only one token is given. It's will be appended automatically by BaseDao before the
            // search is executed.
            buf.append(wildcardChar);
          }
        }
      } else {
        buf.append(token);
      }
    }
    return buf.toString();
  }

  /**
   * @see #modifySearchString(String, boolean)
   */
  public static String modifySearchString(final String searchString) {
    return modifySearchString(searchString, false);
  }

  /**
   * Does nothing (because it seems to be work better in most times). Quotes special Lucene characters: '-' -> "\-"
   *
   * @param searchToken One word / token of the search string (one entry of StringUtils.split(searchString, ' ')).
   * @return
   */
  protected static String modifySearchToken(final String searchToken) {
    final StringBuilder buf = new StringBuilder();
    for (int i = 0; i < searchToken.length(); i++) {
      final char ch = searchToken.charAt(i);
      /*
       * if (ESCAPE_CHARS.indexOf(ch) >= 0) { buf.append('\\'); }
       */
      buf.append(ch);
    }
    return buf.toString();
  }
}
