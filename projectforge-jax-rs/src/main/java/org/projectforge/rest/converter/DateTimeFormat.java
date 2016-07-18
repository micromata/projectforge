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

package org.projectforge.rest.converter;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum DateTimeFormat
{
  /** This is the default date-time format. */
  ISO_DATE_TIME_MILLIS("yyyy-MM-dd HH:mm:ss.SSS"), MILLIS_SINCE_1970(null);

  private final String pattern;

  /**
   * @return the pattern
   */
  public String getPattern()
  {
    return pattern;
  }

  private DateTimeFormat(final String pattern)
  {
    this.pattern = pattern;
  }
}
