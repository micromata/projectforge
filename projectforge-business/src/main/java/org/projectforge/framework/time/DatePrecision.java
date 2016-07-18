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

package org.projectforge.framework.time;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum DatePrecision
{
  MILLISECOND,
  /** Milliseconds will be set to zero (default). */
  SECOND,
  /** Milliseconds and seconds will be set to zero. */
  MINUTE,
  /** Milliseconds and seconds will be set to zero, minutes to 0, 5, 10, 15 etc. */
  MINUTE_5,
  /**
   * Milliseconds and seconds will be set to zero, minutes to 0, 15, 30 or 45.
   */
  MINUTE_15,
  /** Milliseconds, seconds and minutes will be set to zero. */
  HOUR_OF_DAY,
  /** Milliseconds, seconds, minutes and hours will be set to zero. */
  DAY;
}
