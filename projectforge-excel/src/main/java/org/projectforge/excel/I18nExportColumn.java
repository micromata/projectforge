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

package org.projectforge.excel;


public class I18nExportColumn extends ExportColumn
{
  /**
   * @param name
   * @param i18nTitle The i18n key of the title.
   */
  public I18nExportColumn(final String name, final String i18nTitle)
  {
    super(name, i18nTitle != null ? ExportConfig.getInstance().getDefaultExportContext().getLocalizedString(i18nTitle) : "");
  }

  /**
   * @param name
   * @param i18nTitle The i18n key of the title.
   * @param width
   */
  public I18nExportColumn(final String name, final String i18nTitle, final int width)
  {
    super(name, i18nTitle != null ? ExportConfig.getInstance().getDefaultExportContext().getLocalizedString(i18nTitle) : "", width);
  }

  /**
   * @param name
   * @param i18nTitle The i18n key of the title.
   * @param width
   */
  public I18nExportColumn(final Enum< ? > name, final String i18nTitle, final int width)
  {
    super(name, i18nTitle != null ? ExportConfig.getInstance().getDefaultExportContext().getLocalizedString(i18nTitle) : "", width);
  }
}
