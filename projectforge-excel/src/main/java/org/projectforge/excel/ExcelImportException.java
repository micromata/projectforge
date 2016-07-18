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

/**
 * Wird beim Umwandeln der aus der Exceltabelle gelesenen Werte in die Ziel Instanzen geworfen.
 * @author achim
 * 
 */
public class ExcelImportException extends RuntimeException
{

  /**
   * 
   */
  private static final long serialVersionUID = -2082438591559192267L;

  /**
   * Zeile des Fehlers im Sheet
   */
  private Integer row;

  /**
   * Spaltenname
   */
  private String columnname;

  /**
   * setzt alle Member
   * @param msg
   * @param row
   * @param columnname
   */
  public ExcelImportException(final String msg, final Integer row, final String columnname)
  {
    super(msg);
    this.columnname = columnname;
    this.row = row;
  }

  /**
   * liefert den Spaltennamen des Fehlers
   * @return Spaltennamen des Fehlers
   */
  public String getColumnname()
  {
    return columnname;
  }

  /**
   * liefert die Zeilennummer des Fehlers
   * @return Zeilennummer des Fehlers
   */
  public Integer getRow()
  {
    return row;
  }

}
