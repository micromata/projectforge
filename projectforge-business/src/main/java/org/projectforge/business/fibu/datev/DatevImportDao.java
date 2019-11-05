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

import org.apache.commons.lang3.Validate;
import org.projectforge.business.excel.ExcelImportException;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KontoDao;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.kost.*;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.utils.ImportStatus;
import org.projectforge.framework.persistence.utils.ImportStorage;
import org.projectforge.framework.persistence.utils.ImportedElement;
import org.projectforge.framework.persistence.utils.ImportedSheet;
import org.projectforge.framework.utils.ActionLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

@Repository
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
  private EntityManager em;
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
   * @see AccessChecker#hasRight(UserRightId, UserRightValue, boolean)
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
   * @see AccessChecker#hasRight(UserRightId, UserRightValue, boolean)
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
   * @param is
   * @param filename
   * @return ImportStorage mit den gelesenen Daten.
   * @throws Exception
   */
  public ImportStorage<KontoDO> importKontenplan(final InputStream is, final String filename, final ActionLog actionLog)
          throws Exception {
    checkLoggeinUserRight(accessChecker);
    log.info("importKontenplan called");
    final ImportStorage<KontoDO> storage = new ImportStorage<>(Type.KONTENPLAN);
    storage.setFilename(filename);
    final KontenplanExcelImporter imp = new KontenplanExcelImporter();
    imp.doImport(storage, is, actionLog);
    return storage;
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
  public ImportStorage<BuchungssatzDO> importBuchungsdaten(final InputStream is, final String filename,
                                                           final ActionLog actionLog)
          throws Exception {
    checkLoggeinUserRight(accessChecker);
    log.info("importBuchungsdaten called");
    final ImportStorage<BuchungssatzDO> storage = new ImportStorage<>(Type.BUCHUNGSSAETZE);
    storage.setFilename(filename);
    final BuchungssatzExcelImporter imp = new BuchungssatzExcelImporter(storage, kontoDao, kost1Dao, kost2Dao,
            actionLog);
    try {
      imp.doImport(is);
    } catch (final ExcelImportException ex) {
      throw new UserException("common.import.excel.error", ex.getMessage(), ex.getRow(), ex.getColumnname());
    }
    return storage;
  }

  /**
   * Der ImportStorage wird verprobt, dass heißt ein Schreiben der importierten Werte in die Datenbank wird getestet.
   * Ergebnis sind mögliche Fehler und Statistiken, welche Werte neu geschrieben und welche geändert werden. Der User
   * muss der FINANCE_GROUP angehören, um diese Funktionalität ausführen zu können.
   *
   * @param storage
   * @param name    of sheet to reconcile.
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
    int no = -1;
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
    for (final ImportedElement<KontoDO> el : sheet.getElements()) {
      final KontoDO konto = el.getValue();
      final KontoDO dbKonto = kontoDao.getKonto(konto.getNummer());
      if (dbKonto != null) {
        el.setOldValue(dbKonto);
      }
    }
    sheet.setStatus(ImportStatus.RECONCILED);
    sheet.calculateStatistics();
  }

  private void reconcileBuchungsdaten(final ImportedSheet<BuchungssatzDO> sheet) {
    log.info("Reconcile Buchungsdaten called");
    for (final ImportedElement<BuchungssatzDO> el : sheet.getElements()) {
      final BuchungssatzDO satz = el.getValue();
      if (el.isFaulty()) {
        String kost = (String) el.getErrorProperty("kost1");
        if (kost != null) {
          final int[] vals = KostFormatter.splitKost(kost);
          final Kost1DO kost1 = kost1Dao.getKost1(vals[0], vals[1], vals[2], vals[3]);
          if (kost1 != null) {
            satz.setKost1(kost1);
            el.removeErrorProperty("kost1");
          }
        }
        kost = (String) el.getErrorProperty("kost2");
        if (kost != null) {
          final int[] vals = KostFormatter.splitKost(kost);
          final Kost2DO kost2 = kost2Dao.getKost2(vals[0], vals[1], vals[2], vals[3]);
          if (kost2 != null) {
            satz.setKost2(kost2);
            el.removeErrorProperty("kost2");
          }
        }
      }
      final BuchungssatzDO dbSatz = buchungssatzDao.getBuchungssatz(satz.getYear(), satz.getMonth(), satz.getSatznr());
      if (dbSatz != null) {
        el.setOldValue(dbSatz);
      }
    }
    sheet.setStatus(ImportStatus.RECONCILED);
    sheet.calculateStatistics();
  }

  private int commitKontenplan(final ImportedSheet<KontoDO> sheet) {
    log.info("Commit Kontenplan called");
    final Collection<KontoDO> col = new ArrayList<>();
    for (final ImportedElement<KontoDO> el : sheet.getElements()) {
      final KontoDO konto = el.getValue();
      final KontoDO dbKonto = kontoDao.getKonto(konto.getNummer());
      if (dbKonto != null) {
        konto.setId(dbKonto.getId());
        if (el.isSelected()) {
          col.add(konto);
        }
      } else if (el.isSelected()) {
        col.add(konto);
      }
    }
    kontoDao.internalSaveOrUpdate(kontoDao, col, KONTO_INSERT_BLOCK_SIZE);
    return col.size();
  }

  private Object get(final Class<?> clazz, final Integer id) {
    if (id == null) {
      return null;
    }
    return em.find(clazz, id, LockModeType.READ);
  }

  private int commitBuchungsdaten(final ImportedSheet<BuchungssatzDO> sheet) {
    log.info("Commit Buchungsdaten called");
    final Collection<BuchungssatzDO> col = new ArrayList<>();
    for (final ImportedElement<BuchungssatzDO> el : sheet.getElements()) {
      final BuchungssatzDO satz = el.getValue();
      final BuchungssatzDO dbSatz = buchungssatzDao.getBuchungssatz(satz.getYear(), satz.getMonth(), satz.getSatznr());
      boolean addSatz = false;
      if (dbSatz != null) {
        satz.setId(dbSatz.getId());
        if (el.isSelected()) {
          addSatz = true;
        }
      } else if (el.isSelected()) {
        addSatz = true;
      }
      if (addSatz) {
        final BuchungssatzDO newSatz = new BuchungssatzDO();
        newSatz.copyValuesFrom(satz, "konto", "gegenKonto", "kost1", "kost2");
        newSatz.setKonto((KontoDO) get(KontoDO.class, satz.getKontoId()));
        newSatz.setGegenKonto((KontoDO) get(KontoDO.class, satz.getGegenKontoId()));
        newSatz.setKost1((Kost1DO) get(Kost1DO.class, satz.getKost1Id()));
        newSatz.setKost2((Kost2DO) get(Kost2DO.class, satz.getKost2Id()));
        col.add(newSatz);
      }
    }
    buchungssatzDao.internalSaveOrUpdate(buchungssatzDao, col, BUCHUNGSSATZ_INSERT_BLOCK_SIZE);
    return col.size();
  }

  public enum Type {
    KONTENPLAN, BUCHUNGSSAETZE
  }

}
