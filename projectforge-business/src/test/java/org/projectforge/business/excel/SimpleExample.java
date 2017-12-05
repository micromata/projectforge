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

package org.projectforge.business.excel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

public class SimpleExample
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SimpleExample.class);

  public static void main(String... args) throws IOException
  {
    final ExportWorkbook workbook = new ExportWorkbook();
    ExportSheet sheet = workbook.addSheet("Data types");
    sheet.getContentProvider().setColWidths(20, 20);
    sheet.addRow().setValues("Type", "result");
    sheet.addRow().setValues("String", "This is a text.");
    sheet.addRow().setValues("int", 1234);
    sheet.addRow().setValues("BigDecimal", new BigDecimal("1042.3873"));
    Date date = new Date();
    sheet.addRow().setValues("Date", date);
    sheet.addRow().setValues("SQL-Date", new java.sql.Date(date.getTime()));
    sheet.addRow().setValues("Timestamp", new Timestamp(date.getTime()));
    sheet = workbook.addSheet("Own data types");
    sheet.getContentProvider().setColWidths(20, 20).putFormat(Currency.class, "#,##0.00;[Red]-#,##0.00");
    sheet.addRow().setValues("Type", "result");
    sheet.addRow().setValues("Currency", new Currency("1023.873").getValue());
    sheet.addRow().setValues("Currency", new Currency("-10").getValue());
    final File file = new File("target/test-excel.xls");
    log.info("Writing Excel test sheet to work directory: " + file.getAbsolutePath());
    workbook.write(new FileOutputStream(file));
  }
}
