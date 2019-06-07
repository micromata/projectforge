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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.projectforge.business.excel.ExcelImport;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KontoDao;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.kost.BuchungssatzDO;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost1Dao;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.utils.ImportStorage;
import org.projectforge.framework.persistence.utils.ImportedElement;
import org.projectforge.framework.persistence.utils.ImportedSheet;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.utils.ActionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuchungssatzExcelImporter
{
  private static final Logger log = LoggerFactory.getLogger(BuchungssatzExcelImporter.class);

  /**
   * In dieser Zeile stehen die Überschriften der Spalten für die Buchungssätze.
   */
  public static final int ROW_COLUMNNAMES = 0;

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

  public BuchungssatzExcelImporter(final ImportStorage<BuchungssatzDO> storage, final KontoDao kontoDao, final Kost1Dao kost1Dao,
      final Kost2Dao kost2Dao, final ActionLog actionLog)
  {
    this.storage = storage;
    this.kontoDao = kontoDao;
    this.kost1Dao = kost1Dao;
    this.kost2Dao = kost2Dao;
    this.actionLog = actionLog;
  }

  public void doImport(final InputStream is) throws Exception
  {
    final ExcelImport<BuchungssatzImportRow> imp = new ExcelImport<BuchungssatzImportRow>(is);
    for (short idx = 0; idx < imp.getWorkbook().getNumberOfSheets(); idx++) {
      final ImportedSheet<BuchungssatzDO> sheet = importBuchungssaetze(imp, idx);
      if (sheet != null) {
        storage.addSheet(sheet);
      }
    }
  }

  private ImportedSheet<BuchungssatzDO> importBuchungssaetze(final ExcelImport<BuchungssatzImportRow> imp, final int idx) throws Exception
  {
    ImportedSheet<BuchungssatzDO> importedSheet = null;
    imp.setActiveSheet(idx);
    final String name = imp.getWorkbook().getSheetName(idx);
    int m = -1;
    try {
      m = new Integer(name) - 1; // Achtung: month beginnt bei 01 - Januar, während Java mit 0 - Januar rechnet (also - 1).
    } catch (final NumberFormatException ex) {
      // ignore
    }
    if (m >= 0 && m <= 11) {
      actionLog.logInfo("Importing sheet '" + name + "'.");
      final HSSFSheet sheet = imp.getWorkbook().getSheetAt(idx);
      importedSheet = importBuchungssaetze(imp, sheet, m);
    } else {
      log.info("Ignoring sheet '" + name + "' for importing Buchungssätze.");
    }
    return importedSheet;
  }

  private ImportedSheet<BuchungssatzDO> importBuchungssaetze(final ExcelImport<BuchungssatzImportRow> imp, final HSSFSheet sheet,
      final int month) throws Exception
  {
    final ImportedSheet<BuchungssatzDO> importedSheet = new ImportedSheet<BuchungssatzDO>();
    imp.setNameRowIndex(ROW_COLUMNNAMES);
    imp.setStartingRowIndex(ROW_COLUMNNAMES + 1);
    imp.setRowClass(BuchungssatzImportRow.class);

    final Map<String, String> map = new HashMap<String, String>();
    map.put("SatzNr.", "satzNr");
    map.put("Satz-Nr.", "satzNr");
    map.put("Betrag", "betrag");
    map.put("SH", "sh"); // Nicht eindeutig!
    map.put("Konto", "konto");
    map.put("Kostenstelle/-träger", "kost2");
    map.put("Kost2", "kost2");
    map.put("Menge", "menge");
    map.put("SH2", "sh2");
    map.put("Beleg", "beleg");
    map.put("Datum", "datum");
    map.put("Gegenkonto", "gegenkonto");
    map.put("Text", "text");
    map.put("Alt.-Kst.", "kost1");
    map.put("Kost1", "kost1");
    map.put("Beleg 2", "beleg2");
    map.put("KR-BSNr.", "kr_bsnr");
    map.put("ZI", "zi");
    map.put("Kommentar", "comment");
    map.put("Bemerkung", "comment");
    imp.setColumnMapping(map);

    BuchungssatzImportRow[] rows = new BuchungssatzImportRow[0];
    rename2ndSH(sheet);
    rows = imp.convertToRows(BuchungssatzImportRow.class);
    if (rows == null || rows.length == 0) {
      return null;
    }
    int year = 0;
    for (int i = 0; i < rows.length; i++) {
      ImportedElement<BuchungssatzDO> element;
      try {
        element = convertBuchungssatz(rows[i]);
      } catch (final RuntimeException ex) {
        throw new RuntimeException("Im Blatt '" + sheet.getSheetName() + "', in Zeile " + (i + 2) + ": " + ex.getMessage(), ex);
      }
      if (element == null) {
        // Empty row:
        continue;
      }
      final BuchungssatzDO satz = element.getValue();
      final DateHolder date = new DateHolder(satz.getDatum(), DatePrecision.DAY, Locale.GERMAN);
      if (year == 0) {
        year = date.getYear();
      } else if (year != date.getYear()) {
        final String msg =
            "Not supported: Buchungssätze innerhalb eines Excel-Sheets liegen in verschiedenen Jahren: Im Blatt '" + sheet.getSheetName() + "', in Zeile " + (i
                + 2);
        actionLog.logError(msg);
        throw new UserException(msg);
      }
      if (date.getMonth() > month) {
        final String msg = "Buchungssätze können nicht in die Zukunft für den aktuellen Monat '"
            + KostFormatter.formatBuchungsmonat(year, date.getMonth())
            + " gebucht werden! "
            + satz;
        actionLog.logError(msg);
        throw new RuntimeException(msg);
      } else if (date.getMonth() < month) {
        final String msg = "Buchungssatz liegt vor Monat '" + KostFormatter.formatBuchungsmonat(year, month) + "' (OK): " + satz;
        actionLog.logInfo(msg);
      }
      satz.setYear(year);
      satz.setMonth(month);
      importedSheet.addElement(element);
      log.debug(satz.toString());
    }
    importedSheet.setName(KostFormatter.formatBuchungsmonat(year, month));
    importedSheet.setProperty("year", year);
    importedSheet.setProperty("month", month);
    return importedSheet;
  }

  /**
   * Dummerweise ist im DATEV-Export die Spalte SH zweimal vertreten. Da wir SH aber für Haben/Soll auswerten müssen, müssen die Spalten
   * unterschiedlich heißen. Die zweite Spalte wird hier in SH2 umbenannt, sofern vorhanden.
   *
   * @param sheet
   */
  private void rename2ndSH(final HSSFSheet sheet)
  {
    try {
      final HSSFRow row = sheet.getRow(ROW_COLUMNNAMES);
      if (row == null) {
        return;
      }
      short numberOfSH = 0;
      for (int col = 0; col < MAX_COLUMNS; col++) {
        final HSSFCell cell = row.getCell(col);
        if (cell == null) {
          break;
        }
        final String name = cell.getStringCellValue();
        log.debug("Processing column '" + name + "'");
        if ("SH".equals(cell.getStringCellValue()) == true) {
          numberOfSH++;
          if (numberOfSH == 2) {
            log.debug("Renaming 2nd column 'SH' to 'SH2' (column no. " + col + ").");
            cell.setCellValue("SH2");
          }
        }
      }
    } catch (final Exception ex) {
      log.error(ex.getMessage(), ex);
      throw new UserException(ThreadLocalUserContext.getLocalizedString("finance.datev.import.error.titleRowMissed"));
    }
  }

  private ImportedElement<BuchungssatzDO> convertBuchungssatz(final BuchungssatzImportRow row) throws Exception
  {
    if (row.isEmpty() == true) {
      return null;
    }
    final ImportedElement<BuchungssatzDO> element = new ImportedElement<BuchungssatzDO>(storage.nextVal(), BuchungssatzDO.class,
        DatevImportDao.BUCHUNGSSATZ_DIFF_PROPERTIES);
    final BuchungssatzDO satz = new BuchungssatzDO();
    element.setValue(satz);
    satz.setBeleg(row.beleg);
    satz.setBetrag(row.betrag);
    satz.setSH(row.sh);
    satz.setDatum(row.datum);
    satz.setSatznr(row.satzNr);
    satz.setText(StringUtils.replace(row.text, "^", ""));
    satz.setMenge(row.menge);
    satz.setComment(row.comment);
    KontoDO konto = kontoDao.getKonto(row.konto);
    if (konto != null) {
      satz.setKonto(konto);
    } else {
      element.putErrorProperty("konto", row.konto);
    }
    konto = kontoDao.getKonto(row.gegenkonto);
    if (konto != null) {
      satz.setGegenKonto(konto);
    } else {
      element.putErrorProperty("gegenkonto", row.gegenkonto);
    }
    int[] values = KostFormatter.splitKost(row.getKost1());
    final Kost1DO kost1 = kost1Dao.getKost1(values[0], values[1], values[2], values[3]);
    if (kost1 != null) {
      satz.setKost1(kost1);
    } else {
      element.putErrorProperty("kost1", KostFormatter.formatKost(row.kost1));
    }
    values = KostFormatter.splitKost(row.getKost2());
    final Kost2DO kost2 = kost2Dao.getKost2(values[0], values[1], values[2], values[3]);
    if (kost2 != null) {
      satz.setKost2(kost2);
    } else {
      element.putErrorProperty("kost2", KostFormatter.formatKost(row.kost2));
    }
    satz.calculate();
    return element;
  }
}
