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

package org.projectforge.web.core.importstorage;

import org.apache.commons.lang.Validate;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.framework.persistence.utils.ImportStorage;
import org.projectforge.framework.persistence.utils.ImportedSheet;
import org.projectforge.framework.utils.ActionLog;
import org.projectforge.web.wicket.AbstractStandardFormPage;

public abstract class AbstractImportPage<F extends AbstractImportForm< ? , ? , ? >> extends AbstractStandardFormPage
{
  private static final long serialVersionUID = -7206460665473795739L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AbstractImportPage.class);

  protected F form;

  protected final ActionLog actionLog = new ActionLog();

  public AbstractImportPage(final PageParameters parameters)
  {
    super(parameters);
  }

  /**
   * Clears imported storages if exists.
   * @return
   */
  protected void clear()
  {
    log.info("clear called");
    form.setStorage(null);
    removeUserPrefEntry(getStorageKey());
  }

  protected ImportedSheet<?> reconcile(final String sheetName)
  {
    if (form.getStorage() == null) {
      log.error("Reconcile called without storage.");
      return null;
    }
    final ImportedSheet<?>sheet = form.getStorage().getNamedSheet(sheetName);
    if (sheet == null) {
      log.error("Reconcile called without finding sheet: '"
          + sheetName + "'.");
    }
    return sheet;
  }

  protected ImportedSheet<?>  commit(final String sheetName)
  {
    if (form.getStorage() == null) {
      log.error("Commit called without storage.");
      return null;
    }
    final ImportedSheet<?>sheet = form.getStorage().getNamedSheet(sheetName);
    if (sheet == null) {
      log.error("Commit called without finding sheet: '"
          + sheetName + "'.");
    }
    return sheet;
  }

  protected void selectAll(final String sheetName)
  {
    final ImportedSheet< ? > sheet = form.getStorage().getNamedSheet(sheetName);
    Validate.notNull(sheet);
    sheet.selectAll(true, "modified".equals(form.importFilter.getListType()));
  }

  protected void select(final String sheetName, final int number)
  {
    final ImportedSheet< ? > sheet = form.getStorage().getNamedSheet(sheetName);
    Validate.notNull(sheet);
    sheet.select(true, "modified".equals(form.importFilter.getListType()), number);
  }

  protected void deselectAll(final String sheetName)
  {
    final ImportedSheet< ? > sheet = form.getStorage().getNamedSheet(sheetName);
    Validate.notNull(sheet);
    sheet.selectAll(false, false);
  }

  protected void showErrorSummary(final String sheetName)
  {
    final ImportedSheet< ? > sheet = form.getStorage().getNamedSheet(sheetName);
    Validate.notNull(sheet);
    form.setErrorProperties(sheet.getErrorProperties());
  }

  public String getStorageKey()
  {
    return this.getClass() + ".importStorage";
  }

  protected void setStorage(final ImportStorage< ? > storage)
  {
    form.storagePanel.storage = storage;
    putUserPrefEntry(getStorageKey(), form.getStorage(), false);
  }

  protected ImportStorage< ? > getStorage()
  {
    return form.getStorage();
  }
}
