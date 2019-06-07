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

/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.projectforge.business.teamcal.filter;

public enum ViewType
{
  MONTH("month"), BASIC_WEEK("basicWeek"), BASIC_DAY("basicDay"), AGENDA_WEEK("agendaWeek"), AGENDA_DAY("agendaDay");

  private final String code;

  private ViewType(final String code)
  {
    this.code = code;
  }

  public static ViewType forCode(final String code)
  {
    for (final ViewType type : values()) {
      if (type.code.equals(code))
        return type;
    }
    throw new IllegalStateException("Invalid view type code: " + code);
  }

  /**
   * @author Micromata
   * @return the code
   */
  public String getCode()
  {
    return code;
  }
}
