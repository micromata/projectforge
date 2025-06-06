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

package org.projectforge.export;

import org.projectforge.business.excel.I18nExportColumn;

public class AttrColumnDescription
{
  private final String groupName;

  private final String propertyName;

  private final String i18nKey;

  private final int colWidth;

  public AttrColumnDescription(final String groupName, final String propertyName, final String i18nKey)
  {
    this.groupName = groupName;
    this.propertyName = propertyName;
    this.i18nKey = i18nKey;
    this.colWidth = 20; // default
  }

  public AttrColumnDescription(final String groupName, final String propertyName, final String i18nKey, final int colWidth)
  {
    this.groupName = groupName;
    this.propertyName = propertyName;
    this.i18nKey = i18nKey;
    this.colWidth = colWidth;
  }

  public String getGroupName()
  {
    return groupName;
  }

  public String getPropertyName()
  {
    return propertyName;
  }

  public String getI18nKey()
  {
    return i18nKey;
  }

  /**
   * This is used as an ID for the excel export column mapping and
   * for the excel import mapping from ExcelImport over EmployeeBillingExcelRow to an AttrRow.
   *
   * @return The string concatenation of the groupName and the propertyName.
   */
  public String getCombinedName()
  {
    return groupName + propertyName;
  }

  public I18nExportColumn toI18nExportColumn()
  {
    return new I18nExportColumn(getCombinedName(), i18nKey, colWidth);
  }

}
