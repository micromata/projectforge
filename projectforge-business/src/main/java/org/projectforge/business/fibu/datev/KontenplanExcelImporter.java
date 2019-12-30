/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu.datev;

import de.micromata.merlin.excel.ExcelColumnNumberValidator;
import de.micromata.merlin.excel.ExcelSheet;
import de.micromata.merlin.excel.ExcelWorkbook;
import de.micromata.merlin.excel.importer.ImportStorage;
import de.micromata.merlin.excel.importer.ImportedSheet;
import org.apache.poi.ss.usermodel.Row;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.utils.MyImportedElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Iterator;

public class KontenplanExcelImporter {
  public static final String NAME_OF_EXCEL_SHEET = "Kontenplan";

  private static final Logger log = LoggerFactory.getLogger(KontenplanExcelImporter.class);

  public void doImport(final ImportStorage<KontoDO> storage, final InputStream is) {
    final ExcelWorkbook workbook = new ExcelWorkbook(is, storage.getFilename());
    final ExcelSheet sheet = workbook.getSheet(NAME_OF_EXCEL_SHEET);
    if (sheet == null) {
      String msg = "Konten können nicht importiert werden: Blatt '" + NAME_OF_EXCEL_SHEET + "' nicht gefunden.";
      storage.getLogger().error(msg);
      throw new UserException(msg);
    }
    importKontenplan(storage, sheet);
  }

  private void importKontenplan(final ImportStorage<KontoDO> storage, final ExcelSheet sheet) {
    sheet.setAutotrimCellValues(true);
    storage.getLogger().info("Reading sheet '" + NAME_OF_EXCEL_SHEET + "'.");
    sheet.registerColumn("Konto", "Konto von").addColumnListener(new ExcelColumnNumberValidator(1.0).setRequired());
    sheet.registerColumn("Bezeichnung", "Beschriftung").addColumnListener(new ExcelColumnNumberValidator(1.0).setRequired());

    sheet.analyze(true);
    if (sheet.getHeadRow() == null) {
      String msg = "Ignoring sheet '" + NAME_OF_EXCEL_SHEET + "' for importing Buchungssätze, no valid head row found.";
      log.info(msg);
      storage.getLogger().info(msg);
      return;
    }

    final ImportedSheet<KontoDO> importedSheet = new ImportedSheet<>();
    storage.addSheet(importedSheet);
    importedSheet.setName(NAME_OF_EXCEL_SHEET);

    Iterator<Row> it = sheet.getDataRowIterator();
    int year = 0;
    while (it.hasNext()) {
      Row row = it.next();
      final MyImportedElement<KontoDO> element = new MyImportedElement<>(storage.nextVal(), KontoDO.class,
              DatevImportDao.KONTO_DIFF_PROPERTIES);
      final KontoDO konto = new KontoDO();
      element.setValue(konto);
      konto.setNummer(sheet.getCellInt(row, "Konto"));
      konto.setBezeichnung(sheet.getCellString(row, "Bezeichnung"));
      importedSheet.addElement(element);
      log.debug(konto.toString());
    }
  }
}
