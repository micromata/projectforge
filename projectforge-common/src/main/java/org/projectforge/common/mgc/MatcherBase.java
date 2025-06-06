/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import java.util.Collection;

/**
 * Base class of Matcher.
 *
 * @author roger
 * @param <T> the generic type
 */
public abstract class MatcherBase<T>implements Matcher<T>
{

  /**
   * The Constant serialVersionUID.
   */
  private static final long serialVersionUID = 7157263544470217750L;

  @Override
  public MatchResult apply(T token)
  {
    return match(token) ? MatchResult.MatchPositive : MatchResult.NoMatch;
  }

  @Override
  public boolean matchAll(Collection<T> sl, boolean defaultValue)
  {
    boolean matches = defaultValue;
    for (T token : sl) {
      MatchResult mr = apply(token);
      if (mr == MatchResult.NoMatch) {
        return false;
      }
      if (mr == MatchResult.MatchPositive) {
        matches = true;
      }
    }
    return matches;
  }

  @Override
  public boolean matchAny(Collection<T> sl, boolean defaultValue)
  {
    boolean matches = defaultValue;
    for (T token : sl) {
      MatchResult mr = apply(token);
      if (mr == MatchResult.MatchPositive) {
        return true;
      }
    }
    return matches;
  }
}
