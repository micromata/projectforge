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

package org.projectforge.framework.persistence.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;

/**
 * Stores the imported data for displaying and committing.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ImportStorage<T> implements Serializable
{
  private static final long serialVersionUID = 3744632935997632321L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImportStorage.class);

  private List<ImportedSheet<T>> sheets;

  private String filename;

  private Object id;

  private int sequence = 0;

  public ImportStorage()
  {
  }

  public ImportStorage(final Object id)
  {
    this.id = id;
  }

  /**
   * Sheets of the import (e. g. mapping of MS Excel sheets).
   * @return
   */
  public List<ImportedSheet<T>> getSheets()
  {
    return sheets;
  }

  public void addSheet(final ImportedSheet<T> sheet)
  {
    Validate.notNull(sheet);
    if (sheets == null) {
      sheets = new ArrayList<ImportedSheet<T>>();
    }
    sheets.add(sheet);
  }

  public ImportedSheet<T> getNamedSheet(final String name)
  {
    if (CollectionUtils.isEmpty(sheets) == true) {
      return null;
    }
    for (final ImportedSheet<T> sheet : sheets) {
      if (name.equals(sheet.getName()) == true) {
        return sheet;
      }
    }
    return null;
  }

  public void setSheetOpen(final String name, final boolean open)
  {
    final ImportedSheet<T> sheet = getNamedSheet(name);
    if (sheet != null) {
      sheet.setOpen(open);
    } else {
      log.warn("Sheet with name '" + name + "' not found. Can't open/close this sheet in gui.");
    }
  }

  /**
   * File name, if data was imported from a file.
   * @return
   */
  public String getFilename()
  {
    return filename;
  }

  public void setFilename(final String filename)
  {
    this.filename = filename;
  }

  /*
   * @return the name given to constructor or null if default constructor was used. Use-ful for managing multiple storages.
   */
  public Object getId()
  {
    return id;
  }

  /**
   * Each entry in the sheets (ImportedElements) of the storage should have an unique identifier. Use-age: new
   * ImportedElement<Xxx>(storage.nextVal(), Xxx.class, ...); Returns the next integer.
   */
  public synchronized int nextVal()
  {
    return sequence++;
  }

  /**
   * @return the last int given by nextVal without incrementing the underlaying sequencer.
   */
  public int getLastVal()
  {
    return sequence;
  }
}
