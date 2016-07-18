/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.framework.utils;

import java.io.Serializable;

/**
 * Hold integer ranges, such as "20-25" which fits als numbers between 20 and 25.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class IntRanges extends Ranges<Integer> implements Serializable
{
  private static final long serialVersionUID = 6540624564597885182L;

  /**
   * @param rangesString
   */
  public IntRanges(final String rangesString)
  {
    super(rangesString);
  }

  /**
   * @see org.projectforge.framework.utils.Ranges#parseValue(java.lang.String)
   */
  @Override
  protected Integer parseValue(final String value)
  {
    return new Integer(value);
  }
}
