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

package org.projectforge.common;


/**
 * Date formats.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum DateFormatType
{
  /**
   * "dd.MM.", "MM/dd", "dd/MM", ...
   */
  DATE_WITHOUT_YEAR,
  /**
   * yyyy or yy depends on defaultDateFormat. "dd.MM.yy(yy)", "MM/dd/yy(yy)", "dd/MM/yy(yy)", ...
   */
  DATE,
  /**
   * E, DATE
   */
  DATE_WITH_DAY_NAME,
  /**
   * "dd.MM.yy", "MM/dd/yy", "dd/MM/yy", ... For Excel: "DD.MM.YY", "MM/DD/YY", "DD/MM/YY", ...
   */
  DATE_SHORT,
  /**
   * DATE_SHORT + HH:mm:ss
   */
  DATE_TIME_SHORT_SECONDS,
  /**
   * DATE_SHORT + HH:mm
   */
  DATE_TIME_SHORT_MINUTES,
  /**
   * DATE + "HH:mm:ss.SSS"
   */
  DATE_TIME_MILLIS,
  /**
   * DATE + "HH:mm:ss"
   */
  DATE_TIME_SECONDS,
  /**
   * DATE + "HH:mm"
   */
  DATE_TIME_MINUTES,
  /**
   * "HH:mm:ss"
   */
  TIME_OF_DAY_SECONDS,
  /**
   * "HH:mm"
   */
  TIME_OF_DAY_MINUTES,
  /**
   * yyyy-MM-dd
   */
  ISO_DATE,
  /**
   * yyyy-MM-dd HH:mm:ss.SSS
   */
  ISO_TIMESTAMP_MILLIS,
  /**
   * yyyy-MM-dd HH:mm:ss
   */
  ISO_TIMESTAMP_SECONDS,
  /**
   * yyyy-MM-dd HH:mm:ss
   */
  ISO_TIMESTAMP_MINUTES,
  /**
   * EE
   */
  DAY_OF_WEEK_SHORT;
}
