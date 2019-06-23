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

package org.projectforge;

import java.util.ResourceBundle;

public class ProjectForgeVersion
{
  public static final String APP_ID = "ProjectForge";

  public static final String VERSION_STRING = ResourceBundle.getBundle("version").getString("version.number");

  public static final String RELEASE_TIMESTAMP = ResourceBundle.getBundle("version").getString("version.buildDate");

  public static final String RELEASE_DATE = RELEASE_TIMESTAMP.split(" ")[0];

  public static final String YEAR = RELEASE_TIMESTAMP.substring(0, 4);

  /**
   * From 2001 until year of release timestamp.
   */
  public static final String COPYRIGHT_YEARS = "2001-" + YEAR;
}
