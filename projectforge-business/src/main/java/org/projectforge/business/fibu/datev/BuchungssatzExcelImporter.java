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

import de.micromata.merlin.excel.*;
import org.apache.poi.ss.usermodel.Row;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KontoDao;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.kost.*;
import org.projectforge.framework.persistence.utils.ImportStorage;
import org.projectforge.framework.persistence.utils.ImportedElement;
import org.projectforge.framework.persistence.utils.ImportedSheet;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.utils.ActionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;

public class BuchungssatzExcelImporter {
  private static final Logger log = LoggerFactory.getLogger(BuchungssatzExcelImporter.class);

  /**
   * Die Spalte SH ist zweimal vertreten und muss einmal umbenannt werden. Das Vorkommen der zweiten Spalte SH wird bis zu maximal
   * MAX_COLUMNS gesucht.
   */
  public static final short MAX_COLUMNS = 20;

  private final KontoDao kontoDao;

  private final Kost1Dao kost1Dao;

  private final Kost2Dao kost2Dao;

  private final ImportStorage<BuchungssatzDO> storage;

  private final ActionLog actionLog;

  private ExcelColumnDateValidator dateValidator = new ExcelColumnDateValidator("dd.MM.yyyy", "dd.MM.yy", "yyyy-MM-dd");


  public BuchungssatzExcelImporter(final ImportStorage<BuchungssatzDO> storage, final KontoDao kontoDao, final Kost1Dao kost1Dao,
                                   final Kost2Dao kost2Dao, final ActionLog actionLog) {
    this.storage = storage;
    this.kontoDao = kontoDao;
    this.kost1Dao = kost1Dao;
    this.kost2Dao = kost2Dao;
    this.actionLog = actionLog;
  }

  public void doImport(final InputStream is) {
    final ExcelWorkbook workbook = new ExcelWorkbook(is, storage.getFilename());
    for (short idx = 0; idx < workbook.getNumberOfSheets(); idx++) {
      final ImportedSheet<BuchungssatzDO> importedSheet = importBuchungssaetze(workbook, idx);
      if (importedSheet != null) {
        storage.addSheet(importedSheet);
      }
    }
  }

  private ImportedSheet<BuchungssatzDO> importBuchungssaetze(final ExcelWorkbook workbook, final int idx) {
    ExcelSheet sheet = workbook.getSheet(idx);
    sheet.setAutotrimCellValues(true);
    final String name = sheet.getSheetName();
    Integer month = null;
    try {
      month = Integer.parseInt(name); // month beginnt bei 01 - Januar.
    } catch (final NumberFormatException ex) {
      // ignore
    }
    if (month == null) {
      log.info("Ignoring sheet '" + name + "' for importing Buchungssätze.");
      return null;
    }
    ImportedSheet<BuchungssatzDO> importedSheet = null;
    actionLog.logInfo("Importing sheet '" + name + "'.");
    sheet.registerColumn("SatzNr.", "Satz-Nr.").addColumnListener(new ExcelColumnNumberValidator(1.0).setRequired().setUnique());
    sheet.registerColumn("Betrag").addColumnListener(new ExcelColumnNumberValidator().setRequired());
    sheet.registerColumn("SH", "S/H").addColumnListener(new ExcelColumnOptionsValidator("S", "H").setRequired());
    sheet.registerColumn("Konto").addColumnListener(new ExcelColumnNumberValidator().setRequired());
    sheet.registerColumn("Kostenstelle/-träger", "Kost2", "Kst.").addColumnListener(new ExcelColumnValidator().setRequired());
    sheet.registerColumn("Menge");
    //sheet.registerColumn("SH", "S/H"); // Second column not needed.
    sheet.registerColumn("Beleg");
    sheet.registerColumn("Datum").addColumnListener(dateValidator);
    sheet.registerColumn("Gegenkonto").addColumnListener(new ExcelColumnValidator().setRequired());
    sheet.registerColumn("Text");
    sheet.registerColumn("Alt.-Kst.", "Kost1").addColumnListener(new ExcelColumnValidator().setRequired());
    //sheet.registerColumn("Beleg 2");
    //sheet.registerColumn("KR-BSNr.");
    //sheet.registerColumn("ZI");
    sheet.registerColumn("Kommentar", "Bemerkung");
    sheet.analyze(true);
    if (sheet.getHeadRow() == null) {
      log.info("Ignoring sheet '" + name + "' for importing Buchungssätze, no valid head row found.");
      return null;
    }
    importedSheet = importBuchungssaetze(sheet, month);
    return importedSheet;
  }

  /**
   * @param month 1-January, ..., 12-December
   * @throws Exception
   */
  private ImportedSheet<BuchungssatzDO> importBuchungssaetze(final ExcelSheet sheet, final Integer month) {
    final ImportedSheet<BuchungssatzDO> importedSheet = new ImportedSheet<>();
    Iterator<Row> it = sheet.getDataRowIterator();
    int year = 0;
    while (it.hasNext()) {
      Row row = it.next();
      final ImportedElement<BuchungssatzDO> element = new ImportedElement<>(storage.nextVal(), BuchungssatzDO.class,
              DatevImportDao.BUCHUNGSSATZ_DIFF_PROPERTIES);
      final BuchungssatzDO satz = new BuchungssatzDO();
      element.setValue(satz);
      satz.setSatznr(sheet.getCellInt(row, "SatzNr."));
      PFDay day = PFDay.from(dateValidator.convert(sheet.getCell(row, "Datum")));
      if (day == null)
        continue; // Empty row? date not given.
      satz.setDatum(day.getSqlDate());
      if (year == 0) {
        year = day.getYear();
      } else if (year != day.getYear()) {
        final String msg =
                "Not supported: Buchungssätze innerhalb eines Excel-Sheets liegen in verschiedenen Jahren: Im Blatt '" + sheet.getSheetName() + "', in Zeile " + (row.getRowNum() + 1);
        actionLog.logError(msg);
        element.putErrorProperty("datum", "Buchungssatz liegt außerhalb des Buchungsmonats.");
      }
      if (day.getMonthValue() > month) {
        final String msg = "Buchungssätze können nicht in die Zukunft für den aktuellen Monat '"
                + KostFormatter.formatBuchungsmonat(year, day.getMonthValue())
                + " gebucht werden! "
                + satz;
        actionLog.logError(msg);
        element.putErrorProperty("datum", "Buchungssätze können nicht in die Zukunft für den aktuellen Monat erfasst werden.");
      } else if (day.getMonthValue() < month) {
        final String msg = "Buchungssatz liegt vor Monat '" + KostFormatter.formatBuchungsmonat(year, month) + "' (OK): " + satz;
        actionLog.logInfo(msg);
      }
      satz.setYear(year);
      satz.setMonth(month);
      satz.setBetrag(new BigDecimal(sheet.getCellDouble(row, "Betrag")).setScale(2, RoundingMode.HALF_UP));
      satz.setMenge(sheet.getCellString(row, "Menge"));
      ExcelColumnDef commentColDef = sheet.getColumnDef("Kommentar");
      if (commentColDef.found()) {
        satz.setComment(sheet.getCellString(row, commentColDef.getColumnHeadname()));
      }
      satz.setSH(sheet.getCellString(row, "SH"));
      satz.setBeleg(sheet.getCellString(row, "Beleg"));
      satz.setText(sheet.getCellString(row, "Text"));
      Integer kontoInt = sheet.getCellInt(row, "Konto");
      KontoDO konto = kontoDao.getKonto(kontoInt);
      if (konto != null) {
        satz.setKonto(konto);
      } else {
        element.putErrorProperty("konto", kontoInt);
      }
      kontoInt = sheet.getCellInt(row, "Gegenkonto");
      konto = kontoDao.getKonto(kontoInt);
      if (konto != null) {
        satz.setGegenKonto(konto);
      } else {
        element.putErrorProperty("gegenkonto", kontoInt);
      }
      String kostString = sheet.getCellString(row, "Kost1");
      final Kost1DO kost1 = kost1Dao.getKost1(kostString);
      if (kost1 != null) {
        satz.setKost1(kost1);
      } else {
        element.putErrorProperty("kost1", kostString);
      }
      kostString = sheet.getCellString(row, "Kost2");
      final Kost2DO kost2 = kost2Dao.getKost2(kostString);
      if (kost2 != null) {
        satz.setKost2(kost2);
      } else {
        element.putErrorProperty("kost2", kostString);
      }
      satz.calculate(true);
      importedSheet.addElement(element);
      log.debug(satz.toString());
    }
    importedSheet.setName(KostFormatter.formatBuchungsmonat(year, month));
    importedSheet.setProperty("year", year);
    importedSheet.setProperty("month", month);
    return importedSheet;
  }
}
