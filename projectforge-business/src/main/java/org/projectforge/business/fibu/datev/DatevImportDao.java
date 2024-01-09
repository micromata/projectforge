/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.excel.ExcelWorkbook;
import de.micromata.merlin.excel.importer.*;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KontoDao;
import org.projectforge.business.fibu.kost.BuchungssatzDO;
import org.projectforge.business.fibu.kost.BuchungssatzDao;
import org.projectforge.business.fibu.kost.Kost1Dao;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.common.i18n.UserException;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class DatevImportDao {
  public static final UserRightId USER_RIGHT_ID = UserRightId.FIBU_DATEV_IMPORT;
  static final String[] KONTO_DIFF_PROPERTIES = {"nummer", "bezeichnung"};
  static final String[] BUCHUNGSSATZ_DIFF_PROPERTIES = {"satznr", "betrag", "sh", "konto", "kost2", "menge", "beleg",
          "datum",
          "gegenKonto", "text", "kost1", "comment"};
  /**
   * Size of bulk inserts. If this value is too large, exceptions are expected and as more small the value is so as more
   * slowly is the insert process.
   */
  private static final int BUCHUNGSSATZ_INSERT_BLOCK_SIZE = 50;
  /**
   * Size of bulk inserts. If this value is too large, exceptions are expected and as more small the value is so as more
   * slowly is the insert process.
   */
  private static final int KONTO_INSERT_BLOCK_SIZE = 50;
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatevImportDao.class);
  @Autowired
  private PfEmgrFactory emgrFactory;
  @Autowired
  private AccessChecker accessChecker;
  @Autowired
  private KontoDao kontoDao;
  @Autowired
  private Kost1Dao kost1Dao;
  @Autowired
  private Kost2Dao kost2Dao;
  @Autowired
  private BuchungssatzDao buchungssatzDao;

  /**
   * Has the user the right FIBU_DATEV_IMPORT (value true)?
   *
   * @param accessChecker
   * @see UserRightId#FIBU_DATEV_IMPORT
   */
  public static boolean hasRight(final AccessChecker accessChecker) {
    return hasRight(accessChecker, false);
  }

  /**
   * Has the user the right FIBU_DATEV_IMPORT (value true)?
   *
   * @param accessChecker
   * @throws AccessException
   * @see UserRightId#FIBU_DATEV_IMPORT
   */
  public static boolean checkLoggeinUserRight(final AccessChecker accessChecker) {
    return hasRight(accessChecker, true);
  }

  private static boolean hasRight(final AccessChecker accessChecker, final boolean throwException) {
    return accessChecker.hasLoggedInUserRight(USER_RIGHT_ID, throwException, UserRightValue.TRUE);
  }

  /**
   * Liest den Kontenplan aus dem InputStream (Exceltabelle) und schreibt die gelesenen Werte des Kontenplans in
   * ImportStorge. Der User muss der FINANCE_GROUP angehören, um diese Funktionalität ausführen zu können.
   *
   * @param inputStream
   * @param filename
   * @return ImportStorage mit den gelesenen Daten.
   * @throws Exception
   */
  public ImportStorage<KontoDO> importKontenplan(final InputStream inputStream, final String filename)
          throws Exception {
    checkLoggeinUserRight(accessChecker);
    log.info("importKontenplan called");
     try (ExcelWorkbook workbook = new ExcelWorkbook(inputStream, filename, ThreadLocalUserContext.getLocale())) {
      final ImportStorage<KontoDO> storage = new ImportStorage<>(Type.KONTENPLAN, workbook, ImportLogger.Level.INFO, "'" + filename + "':", log);
      final KontenplanExcelImporter imp = new KontenplanExcelImporter();
      imp.doImport(storage, workbook);
      return storage;
    }
  }

  /**
   * Liest die Buchungsdaten aus dem InputStream (Exceltabelle) und schreibt die gelesenen Werte in ImportStorge. Der
   * User muss der FINANCE_GROUP angehören, um diese Funktionalität ausführen zu können.
   *
   * @param is
   * @param filename
   * @return ImportStorage mit den gelesenen Daten.
   * @throws Exception
   */
  public ImportStorage<BuchungssatzDO> importBuchungsdaten(final InputStream is, final String filename)
          throws Exception {
    checkLoggeinUserRight(accessChecker);
    log.info("importBuchungsdaten called.");
    try (ExcelWorkbook workbook = new ExcelWorkbook(is, filename, ThreadLocalUserContext.getLocale())) {
      final ImportStorage<BuchungssatzDO> storage = new ImportStorage<>(Type.BUCHUNGSSAETZE, workbook, ImportLogger.Level.INFO, "'" + filename + "':", log);
      final BuchungssatzExcelImporter imp = new BuchungssatzExcelImporter(storage, kontoDao, kost1Dao, kost2Dao);
      imp.doImport(workbook);
      return storage;
    }
  }

  /**
   * Der ImportStorage wird verprobt, dass heißt ein Schreiben der importierten Werte in die Datenbank wird getestet.
   * Ergebnis sind mögliche Fehler und Statistiken, welche Werte neu geschrieben und welche geändert werden. Der User
   * muss der FINANCE_GROUP angehören, um diese Funktionalität ausführen zu können.
   *
   * @param storage
   * @param sheetName of sheet to reconcile.
   */
  @SuppressWarnings("unchecked")
  public void reconcile(final ImportStorage<?> storage, final String sheetName) {
    checkLoggeinUserRight(accessChecker);
    Validate.notNull(storage.getSheets());
    final ImportedSheet<?> sheet = storage.getNamedSheet(sheetName);
    Validate.notNull(sheet);
    if (storage.getId() == Type.KONTENPLAN) {
      reconcileKontenplan((ImportedSheet<KontoDO>) sheet);
    } else {
      reconcileBuchungsdaten((ImportedSheet<BuchungssatzDO>) sheet);
    }
    sheet.setNumberOfCommittedElements(-1);
  }

  @SuppressWarnings("unchecked")
  public void commit(final ImportStorage<?> storage, final String sheetName) {
    checkLoggeinUserRight(accessChecker);
    Validate.notNull(storage.getSheets());
    final ImportedSheet<?> sheet = storage.getNamedSheet(sheetName);
    Validate.notNull(sheet);
    if (sheet.getStatus() != ImportStatus.RECONCILED) {
      throw new UserException("common.import.action.commit.error.notReconciled");
    }
    int no = 0;
    if (storage.getId() == Type.KONTENPLAN) {
      no = commitKontenplan((ImportedSheet<KontoDO>) sheet);
    } else {
      no = commitBuchungsdaten((ImportedSheet<BuchungssatzDO>) sheet);
    }
    sheet.setNumberOfCommittedElements(no);
    sheet.setStatus(ImportStatus.IMPORTED);
  }

  private void reconcileKontenplan(final ImportedSheet<KontoDO> sheet) {
    log.info("Reconcile Kontenplan called");
    if (sheet.getElements() != null) {
      for (final ImportedElement<KontoDO> el : sheet.getElements()) {
        final KontoDO konto = el.getValue();
        final KontoDO dbKonto = kontoDao.getKonto(konto.getNummer());
        if (dbKonto != null) {
          el.setOldValue(dbKonto);
        }
      }
    }
    sheet.setStatus(ImportStatus.RECONCILED);
    sheet.calculateStatistics();
  }

  private void reconcileBuchungsdaten(final ImportedSheet<BuchungssatzDO> sheet) {
    log.info("Reconcile Buchungsdaten called");
    if (sheet.getElements() != null) {
      for (final ImportedElement<BuchungssatzDO> el : sheet.getElements()) {
        final BuchungssatzDO satz = el.getValue();
        if (satz.getYear() != null && satz.getMonth() != null && satz.getSatznr() != null) {
          final BuchungssatzDO dbSatz = buchungssatzDao.getBuchungssatz(satz.getYear(), satz.getMonth(), satz.getSatznr());
          if (dbSatz != null) {
            el.setOldValue(dbSatz);
          }
        }
      }
    }
    sheet.setStatus(ImportStatus.RECONCILED);
    sheet.calculateStatistics();
  }

  private int commitKontenplan(final ImportedSheet<KontoDO> sheet) {
    log.info("Commit Kontenplan called");
    final Collection<KontoDO> col = new ArrayList<>();
    if (sheet.getElements() != null) {
      for (final ImportedElement<KontoDO> el : sheet.getElements()) {
        if (!el.getSelected()) {
          continue;
        }
        final KontoDO konto = el.getValue();
        if (el.getOldValue() != null) {
          konto.setId(el.getOldValue().getId());
        }
        col.add(konto);
      }
      kontoDao.internalSaveOrUpdate(col, KONTO_INSERT_BLOCK_SIZE);
    }
    return col.size();
  }

  private int commitBuchungsdaten(final ImportedSheet<BuchungssatzDO> sheet) {
    log.info("Commit Buchungsdaten called");
    final Collection<BuchungssatzDO> col = new ArrayList<>();
    if (sheet.getElements() != null) {
      for (final ImportedElement<BuchungssatzDO> el : sheet.getElements()) {
        if (!el.getSelected()) {
          continue;
        }
        final BuchungssatzDO satz = el.getValue();
        if (el.getOldValue() != null) {
          satz.setId(el.getOldValue().getId());
        }
        col.add(satz);
      }
      buchungssatzDao.internalSaveOrUpdate(col, BUCHUNGSSATZ_INSERT_BLOCK_SIZE);
    }
    return col.size();
  }

  public enum Type {
    KONTENPLAN, BUCHUNGSSAETZE
  }

}
