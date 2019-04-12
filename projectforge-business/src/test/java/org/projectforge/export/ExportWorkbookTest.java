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

package org.projectforge.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExportConfig;
import org.projectforge.business.excel.ExportSheet;
import org.projectforge.business.excel.ExportWorkbook;
import org.projectforge.framework.configuration.ConfigXmlTest;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.test.WorkFileHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Locale;

public class ExportWorkbookTest {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExportWorkbookTest.class);

  @BeforeEach
  public void setUp() {
    ConfigXmlTest.createTestConfiguration();
  }

  @Test
  public void exportGermanExcel() throws IOException {
    writeExcel("TestExcel_de.xls", Locale.GERMAN, "DD.MM.YYYY");
  }

  @Test
  public void exportExcel() throws IOException {
    writeExcel("TestExcel_en.xls", Locale.ENGLISH, "DD/MM/YYYY");
  }

  private void writeExcel(final String filename, final Locale locale, final String excelDateFormat) throws IOException {
    final PFUserDO user = new PFUserDO();
    user.setLocale(locale);
    user.setExcelDateFormat(excelDateFormat);
    ExportConfig.setInstance(new ExportConfig() {
      @Override
      protected ContentProvider createNewContentProvider(final ExportWorkbook workbook) {
        return new MyXlsContentProvider(workbook);
      }
    }.setDefaultExportContext(new MyXlsExportContext()));
    final ExportWorkbook workbook = new ExportWorkbook();
    final ExportSheet sheet = workbook.addSheet("Test");
    sheet.getContentProvider().setColWidths(20, 20, 20);
    sheet.addRow().setValues("Type", "Precision", "result");
    sheet.addRow().setValues("Java output", ".", "Tue Sep 28 00:27:10 UTC 2010");
    sheet.addRow().setValues("DateHolder", "DAY", getDateHolder().setPrecision(DatePrecision.DAY));
    sheet.addRow().setValues("DateHolder", "HOUR_OF_DAY", getDateHolder().setPrecision(DatePrecision.HOUR_OF_DAY));
    sheet.addRow().setValues("DateHolder", "MINUTE_15", getDateHolder().setPrecision(DatePrecision.MINUTE_15));
    sheet.addRow().setValues("DateHolder", "MINUTE", getDateHolder().setPrecision(DatePrecision.MINUTE));
    sheet.addRow().setValues("DateHolder", "SECOND", getDateHolder().setPrecision(DatePrecision.SECOND));
    sheet.addRow().setValues("DateHolder", "MILLISECOND", getDateHolder().setPrecision(DatePrecision.MILLISECOND));
    sheet.addRow().setValues("DateHolder", "-", getDateHolder());
    sheet.addRow().setValues("DayHolder", "-", new DayHolder(getDate()));
    sheet.addRow().setValues("java.util.Date", "-", getDate());
    sheet.addRow().setValues("java.sql.Timestamp", "-", new Timestamp(getDate().getTime()));
    sheet.addRow().setValues("int", "-", 1234);
    sheet.addRow().setValues("BigDecimal", "-", new BigDecimal("123123123.123123123123"));
    final File file = WorkFileHelper.getWorkFile(filename);
    log.info("Writing Excel test sheet to work directory: " + file.getAbsolutePath());
    workbook.write(new FileOutputStream(file));
  }

  private DateHolder getDateHolder() {
    return new DateHolder(getDate(), DateHelper.UTC);
  }

  private Date getDate() {
    return new Date(1285633630868L);
  }

}
