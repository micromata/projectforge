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

import org.apache.poi.hssf.usermodel.HSSFRow;

/**
 * Interface for dynamically creating objects for a given row in the excel sheet.
 * @param <T> baseclass for the rows
 * 
 * @author Wolfgang Jung (w.jung@micromata.de)
 */
public interface ClassFactory<T>
{
  /**
   * create a new instance of the objectholder for the given row.
   * 
   * @param row the row to convert.
   * @return a new Object for holding the values.
   * @throws InstantiationException @see Class#newInstance()
   * @throws IllegalAccessException @see Class#newInstance()
   */
  public T newInstance(HSSFRow row) throws InstantiationException, IllegalAccessException;
}
