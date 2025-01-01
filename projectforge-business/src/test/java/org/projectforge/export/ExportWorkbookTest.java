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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExportConfig;
import org.projectforge.business.excel.ExportSheet;
import org.projectforge.business.excel.ExportWorkbook;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.business.test.TestSetup;
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
    TestSetup.init();
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
    sheet.addRow().setValues("DateTime", "DAY", getDateTime().withPrecision(DatePrecision.DAY));
    sheet.addRow().setValues("DateTime", "HOUR_OF_DAY", getDateTime().withPrecision(DatePrecision.HOUR_OF_DAY));
    sheet.addRow().setValues("DateTime", "MINUTE_15", getDateTime().withPrecision(DatePrecision.MINUTE_15));
    sheet.addRow().setValues("DateTime", "MINUTE", getDateTime().withPrecision(DatePrecision.MINUTE));
    sheet.addRow().setValues("DateTime", "SECOND", getDateTime().withPrecision(DatePrecision.SECOND));
    sheet.addRow().setValues("DateTime", "MILLISECOND", getDateTime().withPrecision(DatePrecision.MILLISECOND));
    sheet.addRow().setValues("DateTime", "-", getDateTime());
    sheet.addRow().setValues("DayHolder", "-", PFDateTime.from(getDate()));
    sheet.addRow().setValues("java.util.Date", "-", getDate());
    sheet.addRow().setValues("java.sql.Timestamp", "-", new Timestamp(getDate().getTime()));
    sheet.addRow().setValues("int", "-", 1234);
    sheet.addRow().setValues("BigDecimal", "-", new BigDecimal("123123123.123123123123"));
    final File file = WorkFileHelper.getWorkFile(filename);
    log.info("Writing Excel test sheet to work directory: " + file.getAbsolutePath());
    workbook.write(new FileOutputStream(file));
  }

  private PFDateTime getDateTime() {
    return PFDateTime.from(getDate(), DateHelper.UTC);
  }

  private Date getDate() {
    return new Date(1285633630868L);
  }

}
