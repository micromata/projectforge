/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

/**
 * Matches always.
 *
 * @author roger
 * @param <T> the generic type
 */
public class EveryMatcher<T>extends MatcherBase<T>
{

  /**
   * The Constant serialVersionUID.
   */
  private static final long serialVersionUID = -6603198094053117381L;

  /**
   * The singleton.
   */
  private static EveryMatcher<?> SINGLETON = new EveryMatcher<Object>();

  /**
   * Single instanceof.
   *
   * @param <T> the generic type
   * @return the every matcher
   */
  @SuppressWarnings("unchecked")
  public static <T> EveryMatcher<T> everyMatcher()
  {
    return (EveryMatcher<T>) SINGLETON;
  }

  @Override
  public boolean match(T token)
  {
    return true;
  }

  @Override
  public String toString()
  {
    return "*";
  }

}
