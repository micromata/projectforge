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

public class ExportColumn
{
  private final String name;

  private final String title;

  private int width;

  public ExportColumn(final String name, final String title)
  {
    this.name = name;
    this.title = title;
  }

  public ExportColumn(final String name, final String title, final int width)
  {
    this.name = name;
    this.title = title;
    this.width = width;
  }

  public ExportColumn(final Enum< ? > name, final String title, final int width)
  {
    this.name = name.name();
    this.title = title;
    this.width = width;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the title
   */
  public String getTitle()
  {
    return title;
  }

  /**
   * @return the width
   */
  public int getWidth()
  {
    return width;
  }

  /**
   * @param width the width to set
   * @return this for chaining.
   */
  public ExportColumn setWidth(final int width)
  {
    this.width = width;
    return this;
  }
}
