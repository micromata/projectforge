/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.excel.ExcelColumnDef;
import de.micromata.merlin.excel.ExcelRow;
import de.micromata.merlin.excel.ExcelSheet;
import de.micromata.merlin.excel.ExcelWorkbook;
import de.micromata.merlin.excel.importer.ImportLogger;
import de.micromata.merlin.excel.importer.ImportStorage;
import de.micromata.merlin.excel.importer.ImportedSheet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.common.SupplierWithException;
import org.projectforge.business.excel.ExcelImportException;
import org.projectforge.common.i18n.UserException;
import org.projectforge.web.wicket.AbstractStandardFormPage;
import org.projectforge.web.wicket.DownloadUtils;

import java.nio.charset.StandardCharsets;

public abstract class AbstractImportPage<F extends AbstractImportForm<?, ?, ?>> extends AbstractStandardFormPage {
  private static final long serialVersionUID = -7206460665473795739L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractImportPage.class);

  protected F form;

  public AbstractImportPage(final PageParameters parameters) {
    super(parameters);
  }

  /**
   * Clears imported storages if exists.
   *
   * @return
   */
  protected void clear() {
    log.info("clear called");
    form.setStorage(null);
    removeUserPrefEntry(getStorageKey());
  }

  protected ImportedSheet<?> reconcile(final String sheetName) {
    if (form.getStorage() == null) {
      log.error("Reconcile called without storage.");
      return null;
    }
    final ImportedSheet<?> sheet = form.getStorage().getNamedSheet(sheetName);
    if (sheet == null) {
      log.error("Reconcile called without finding sheet: '"
              + sheetName + "'.");
    }
    return sheet;
  }

  protected ImportedSheet<?> commit(final String sheetName) {
    if (form.getStorage() == null) {
      log.error("Commit called without storage.");
      return null;
    }
    final ImportedSheet<?> sheet = form.getStorage().getNamedSheet(sheetName);
    if (sheet == null) {
      log.error("Commit called without finding sheet: '"
              + sheetName + "'.");
    }
    return sheet;
  }

  protected void selectAll(final String sheetName) {
    final ImportedSheet<?> sheet = form.getStorage().getNamedSheet(sheetName);
    Validate.notNull(sheet);
    sheet.selectAll(true, "modified".equals(form.importFilter.getListType()));
  }

  protected void select(final String sheetName, final int number) {
    final ImportedSheet<?> sheet = form.getStorage().getNamedSheet(sheetName);
    Validate.notNull(sheet);
    sheet.select(true, "modified".equals(form.importFilter.getListType()), number);
  }

  protected void deselectAll(final String sheetName) {
    final ImportedSheet<?> sheet = form.getStorage().getNamedSheet(sheetName);
    Validate.notNull(sheet);
    sheet.selectAll(false, false);
  }

  protected void showErrorSummary(final String sheetName) {
    final ImportedSheet<?> sheet = form.getStorage().getNamedSheet(sheetName);
    Validate.notNull(sheet);
    form.setErrorProperties(sheet.getErrorProperties());
  }

  protected void downloadErrorLog(ImportedSheet sheet) {
    StringBuilder sb = new StringBuilder();
    ImportLogger logger = getStorage().getLogger();
    try (ExcelWorkbook workbook = logger.getExcelWorkbook()) {
      if (logger.getHasEvents()) {
        String filename = workbook != null ? workbook.getFilename() : "file.xls";
        appendSectionTitle(sb, getLocalizedMessage("common.import.logger.error.workbookSection", filename));
        sb.append(logger.getErrorEventsAsString());
        sb.append("\n\n");
      }
      logger = sheet.getLogger();
      if (logger.getHasEvents()) {
        appendSectionTitle(sb, getLocalizedMessage("common.import.logger.error.sheetSection", sheet.getOrigName()));
        sb.append(logger.getErrorEventsAsString());
      }
      appendSheetInformation(sheet, sb);
      DownloadUtils.setUTF8CharacterEncoding(getResponse());
      DownloadUtils.setDownloadTarget(sb.toString().getBytes(StandardCharsets.UTF_8), "Fehlerprotokolldatei-Excelimport.txt");
    }
  }

  protected void downloadInfoLog(ImportedSheet sheet) {
    StringBuilder sb = new StringBuilder();
    ImportLogger logger = getStorage().getLogger();
    try (ExcelWorkbook workbook = logger.getExcelWorkbook()) {
      if (logger.getHasEvents()) {
        String filename = workbook != null ? workbook.getFilename() : "file.xls";
        appendSectionTitle(sb, getLocalizedMessage("common.import.logger.info.workbookSection", filename));
        sb.append(logger.getEventsAsString());
        sb.append("\n\n");
      }
      logger = sheet.getLogger();
      if (logger.getHasEvents()) {
        appendSectionTitle(sb, getLocalizedMessage("common.import.logger.info.sheetSection", sheet.getOrigName()));
        sb.append(logger.getEventsAsString());
      }
      appendSheetInformation(sheet, sb);
      DownloadUtils.setUTF8CharacterEncoding(getResponse());
      DownloadUtils.setDownloadTarget(sb.toString().getBytes(StandardCharsets.UTF_8), "Protokolldatei-Excelimport.txt");
    }
  }

  private void appendSheetInformation(ImportedSheet sheet, StringBuilder sb) {
    sb.append("\n\n");
    appendSectionTitle(sb, getLocalizedMessage("common.import.excel.info"));
    ImportLogger logger = sheet.getLogger();
    ExcelSheet excelSheet = logger.getExcelSheet();
    if (excelSheet != null) {
      ExcelRow headRow = excelSheet.getHeadRow();
      if (headRow != null) {
        sb.append("Head row found in row #" ).append(headRow.getRowNum() + 1).append("\n");
      } else {
        sb.append("No head row found.\n");
      }
      for (ExcelColumnDef colDef: excelSheet.getColumnDefinitions()) {
        sb.append("Column: ").append(colDef).append("\n");
      }
    } else {
      sb.append("No information about required Excel sheet format available.\n");
    }
  }

  private void appendSectionTitle(StringBuilder sb, String title) {
    sb.append(title).append("\n");
    sb.append(StringUtils.repeat('-', title.length())).append("\n");
  }

  protected void downloadValidatedExcel(final String sheetName) {
    try (final ExcelWorkbook workbook = form.getStorage().getWorkbook()) {
      workbook.setActiveSheet(sheetName);
      DownloadUtils.setUTF8CharacterEncoding(getResponse());
      DownloadUtils.setDownloadTarget(workbook.getAsByteArrayOutputStream().toByteArray(), workbook.getFilename());
    }
  }

  public String getStorageKey() {
    return this.getClass() + ".importStorage";
  }

  protected void setStorage(final ImportStorage<?> storage) {
    form.storagePanel.storage = storage;
    putUserPrefEntry(getStorageKey(), form.getStorage(), false);
  }

  protected ImportStorage<?> getStorage() {
    return form.getStorage();
  }

  protected String translateParams(ExcelImportException ex) {
    return getString("common.import.excel.error1") + " " + ex.getRow() + " " +
            getString("common.import.excel.error2") + " \"" + ex.getColumnname() + "\"";
  }

  protected <T, E extends Exception> T doImportWithExcelExceptionHandling(final SupplierWithException<T, E> importFunction) {
    try {
      return importFunction.get();
    } catch (final Exception ex) {
      if (ex instanceof ExcelImportException) {
        error(translateParams((ExcelImportException) ex));
      } else if (ex instanceof UserException) {
        error(translateParams((UserException) ex));
      } else {
        error("An error occurred (see log files for details): " + ex.getMessage());
      }
      log.error(ex.getMessage(), ex);
      clear();
    }
    return null;
  }
}
