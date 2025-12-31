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

package org.projectforge.common.mgc;

import java.io.Serializable;
import java.util.Collection;

/**
 * Base class for matching.
 *
 * @author roger
 * @param <T> the generic type
 */
public interface Matcher<T>extends Serializable
{

  /**
   * Allgemeine Methode um ein Object auf das "Passen" zu überpüfen.
   *
   * @param object Das zu checkende Objekt
   * @return true, falls das Objekt passt
   */
  public boolean match(T object);

  /**
   * similar to match, but return 3 state.
   *
   * @param object the object
   * @return the match result
   */
  public MatchResult apply(T object);

  /**
   * Match any.
   *
   * @param sl the sl
   * @param defaultValue if none are matched, returns defualtValue
   * @return true, if successful
   */
  boolean matchAny(Collection<T> sl, boolean defaultValue);

  /**
   * Match all.
   *
   * @param sl the sl
   * @param defaultValue the default value
   * @return true, if successful
   */
  boolean matchAll(Collection<T> sl, boolean defaultValue);
}
