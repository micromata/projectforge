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

package org.projectforge.framework.renderer;

/**
 * For storing a cell content.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class CellHolder
{
  private String content;

  private int colspan = 1;

  private int rowspan = 1;

  public CellHolder()
  {
  }

  /**
   * 
   * @param content If null, content will set to empty string.
   */
  public CellHolder(String content)
  {
    if (content != null) {
      this.content = content;
    } else {
      this.content = "";
    }
  }

  public void setContent(String content)
  {
    this.content = content;
  }

  public String getContent()
  {
    return content;
  }

  public void setColspan(int colspan)
  {
    this.colspan = colspan;
  }

  public int getColspan()
  {
    return colspan;
  }

  public void setRowspan(int rowspan)
  {
    this.rowspan = rowspan;
  }

  public int getRowspan()
  {
    return rowspan;
  }

  public String toString()
  {
    return content;
  }
}
