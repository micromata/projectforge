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

package org.projectforge.business.fibu;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.i18n.MessageParam;
import org.projectforge.framework.i18n.MessageParamType;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.xstream.XmlObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.Tuple;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Repository
public class RechnungDao extends BaseDao<RechnungDO> {
  public static final UserRightId USER_RIGHT_ID = UserRightId.FIBU_AUSGANGSRECHNUNGEN;

  public final static int START_NUMBER = 1000;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RechnungDao.class);

  private static final Class<?>[] ADDITIONAL_SEARCH_DOS = new Class[]{RechnungsPositionDO.class};

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"kunde.name", "projekt.name",
          "projekt.kunde.name", "positionen.auftragsPosition.auftrag.nummer"};

  @Autowired
  private KundeDao kundeDao;

  @Autowired
  private ProjektDao projektDao;

  @Autowired
  private RechnungCache rechnungCache;

  public RechnungDao() {
    super(RechnungDO.class);
    userRightId = USER_RIGHT_ID;
  }

  public static BigDecimal getNettoSumme(final Collection<RechnungsPositionVO> col) {
    BigDecimal nettoSumme = BigDecimal.ZERO;
    if (col != null && col.size() > 0) {
      for (final RechnungsPositionVO pos : col) {
        nettoSumme = nettoSumme.add(pos.getNettoSumme());
      }
    }
    return nettoSumme;
  }

  static void writeUiStatusToXml(final AbstractRechnungDO rechnung) {
    final String uiStatusAsXml = XmlObjectWriter.writeAsXml(rechnung.getUiStatus());
    rechnung.setUiStatusAsXml(uiStatusAsXml);
  }

  /**
   * @return the rechnungCache
   */
  public RechnungCache getRechnungCache() {
    return rechnungCache;
  }

  /**
   * List of all years with invoices: select min(datum), max(datum) from t_fibu_rechnung.
   */
  public int[] getYears() {
    final Tuple minMaxDate = SQLHelper.ensureUniqueResult(em.createNamedQuery(RechnungDO.SELECT_MIN_MAX_DATE, Tuple.class));
    return SQLHelper.getYears((java.sql.Date) minMaxDate.get(0), (java.sql.Date) minMaxDate.get(1));
  }

  public RechnungsStatistik buildStatistik(final List<RechnungDO> list) {
    final RechnungsStatistik stats = new RechnungsStatistik();
    if (list == null) {
      return stats;
    }
    for (final RechnungDO rechnung : list) {
      stats.add(rechnung);
    }
    return stats;
  }

  /**
   * @param rechnung
   * @param days
   * @see DateHelper#getCalendar()
   */
  public Date calculateFaelligkeit(final RechnungDO rechnung, final int days) {
    if (rechnung.getDatum() == null) {
      return null;
    }
    final Calendar cal = DateHelper.getCalendar();
    cal.setTime(rechnung.getDatum());
    cal.add(Calendar.DAY_OF_YEAR, days);
    return cal.getTime();
  }

  /**
   * @param rechnung
   * @param kundeId  If null, then kunde will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setKunde(final RechnungDO rechnung, final Integer kundeId) {
    final KundeDO kunde = kundeDao.getOrLoad(kundeId);
    rechnung.setKunde(kunde);
  }

  /**
   * @param rechnung
   * @param projektId If null, then projekt will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setProjekt(final RechnungDO rechnung, final Integer projektId) {
    final ProjektDO projekt = projektDao.getOrLoad(projektId);
    rechnung.setProjekt(projekt);
  }

  /**
   * Sets the scales of percentage and currency amounts. <br/>
   * Gutschriftsanzeigen dürfen keine Rechnungsnummer haben. Wenn eine Rechnungsnummer für neue Rechnungen gegeben
   * wurde, so muss sie fortlaufend sein. Berechnet das Zahlungsziel in Tagen, wenn nicht gesetzt, damit es indiziert
   * wird.
   */
  @Override
  protected void onSaveOrModify(final RechnungDO rechnung) {
    if (RechnungTyp.RECHNUNG.equals(rechnung.getTyp()) && rechnung.getId() != null) {
      RechnungDO originValue = internalGetById(rechnung.getId());
      if (RechnungStatus.GEPLANT.equals(originValue.getStatus()) && !RechnungStatus.GEPLANT.equals(rechnung.getStatus())) {
        rechnung.setNummer(getNextNumber(rechnung));

        final DayHolder day = new DayHolder();
        rechnung.setDatum(day.getSQLDate());

        Integer zahlungsZielInTagen = rechnung.getZahlungsZielInTagen();
        if (zahlungsZielInTagen != null) {
          day.add(Calendar.DAY_OF_MONTH, zahlungsZielInTagen);
        }
        rechnung.setFaelligkeit(day.getSQLDate());
      }
    }

    AuftragAndRechnungDaoHelper.onSaveOrModify(rechnung);

    validate(rechnung);

    if (rechnung.getTyp() == RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN) {
      if (rechnung.getNummer() != null) {
        throw new UserException("fibu.rechnung.error.gutschriftsanzeigeDarfKeineRechnungsnummerHaben");
      }
    } else {
      if (!RechnungStatus.GEPLANT.equals(rechnung.getStatus()) && rechnung.getNummer() == null) {
        throw new UserException("validation.required.valueNotPresent",
                new MessageParam("fibu.rechnung.nummer", MessageParamType.I18N_KEY));
      }
      if (!RechnungStatus.GEPLANT.equals(rechnung.getStatus())) {
        if (rechnung.getId() == null) {
          // Neue Rechnung
          final Integer next = getNextNumber(rechnung);
          if (next.intValue() != rechnung.getNummer().intValue()) {
            throw new UserException("fibu.rechnung.error.rechnungsNummerIstNichtFortlaufend");
          }
        } else {
          final RechnungDO other = SQLHelper.ensureUniqueResult(em.createNamedQuery(RechnungDO.FIND_OTHER_BY_NUMMER, RechnungDO.class)
                  .setParameter("nummer", rechnung.getNummer())
                  .setParameter("id", rechnung.getId()));
          if (other != null) {
            throw new UserException("fibu.rechnung.error.rechnungsNummerBereitsVergeben");
          }
        }
      }
    }
    if (rechnung.getZahlBetrag() != null) {
      rechnung.setZahlBetrag(rechnung.getZahlBetrag().setScale(2, RoundingMode.HALF_UP));
    }
    rechnung.recalculate();
    if (CollectionUtils.isEmpty(rechnung.getPositionen())) {
      throw new UserException("fibu.rechnung.error.rechnungHatKeinePositionen");
    }
    final int size = rechnung.getPositionen().size();
    for (int i = size - 1; i > 0; i--) {
      // Don't remove first position, remove only the last empty positions.
      final RechnungsPositionDO position = rechnung.getPositionen().get(i);
      if (position.getId() == null && position.isEmpty()) {
        rechnung.getPositionen().remove(i);
      } else {
        break;
      }
    }
    writeUiStatusToXml(rechnung);
  }

  private void validate(final RechnungDO rechnung) {
    final RechnungStatus status = rechnung.getStatus();
    final BigDecimal zahlBetrag = rechnung.getZahlBetrag();
    final boolean zahlBetragExists = (zahlBetrag != null && zahlBetrag.compareTo(BigDecimal.ZERO) != 0);
    if (status == RechnungStatus.BEZAHLT && !zahlBetragExists) {
      throw new UserException("fibu.rechnung.error.statusBezahltErfordertZahlBetrag");
    }

    final Integer projektId = rechnung.getProjektId();
    final Integer kundeId = rechnung.getKundeId();
    final String kundeText = rechnung.getKundeText();
    if (projektId == null && kundeId == null && StringUtils.isEmpty(kundeText)) {
      throw new UserException("fibu.rechnung.error.kundeTextOderProjektRequired");
    }
  }

  @Override
  protected void afterSaveOrModify(final RechnungDO obj) {
    getRechnungCache().setExpired(); // Expire the cache because assignments to order position may be changed.
  }

  @Override
  protected void prepareHibernateSearch(final RechnungDO obj, final OperationType operationType) {
    projektDao.initializeProjektManagerGroup(obj.getProjekt());
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * Fetches the cost assignments.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#getById(java.io.Serializable)
   */
  @Override
  public RechnungDO getById(final Serializable id) throws AccessException {
    final RechnungDO rechnung = super.getById(id);
    for (final RechnungsPositionDO pos : rechnung.getPositionen()) {
      final List<KostZuweisungDO> list = pos.getKostZuweisungen();
      if (list != null && list.size() > 0) {
        // Kostzuweisung is initialized
      }
    }
    return rechnung;
  }

  @Override
  public List<RechnungDO> getList(final BaseSearchFilter filter) {
    final RechnungListFilter myFilter;
    if (filter instanceof RechnungListFilter) {
      myFilter = (RechnungListFilter) filter;
    } else {
      myFilter = new RechnungListFilter(filter);
    }

    final QueryFilter queryFilter = AuftragAndRechnungDaoHelper.createQueryFilterWithDateRestriction(myFilter);
    queryFilter.addOrder(SortProperty.desc("datum"));
    queryFilter.addOrder(SortProperty.desc("nummer"));
    if (myFilter.isShowKostZuweisungStatus()) {
      //queryFilter.setFetchMode("positionen.kostZuweisungen", FetchMode.JOIN);
    }

    AuftragAndRechnungDaoHelper.createCriterionForPeriodOfPerformance(myFilter).ifPresent(queryFilter::add);

    final List<RechnungDO> list = getList(queryFilter);
    if (myFilter.isShowAll() || myFilter.isDeleted()) {
      return list;
    }

    final List<RechnungDO> result = new ArrayList<>();
    for (final RechnungDO rechnung : list) {
      if (myFilter.isShowUnbezahlt()) {
        if (!rechnung.isBezahlt()) {
          result.add(rechnung);
        }
      } else if (myFilter.isShowBezahlt()) {
        if (rechnung.isBezahlt()) {
          result.add(rechnung);
        }
      } else if (myFilter.isShowUeberFaellig()) {
        if (rechnung.isUeberfaellig()) {
          result.add(rechnung);
        }
      } else {
        log.error("Unknown filter setting: " + myFilter.listType);
        break;
      }
    }
    return result;
  }

  @Override
  public List<RechnungDO> sort(final List<RechnungDO> list) {
    Collections.sort(list);
    return list;
  }

  /**
   * Gets the highest Rechnungsnummer.
   */
  public Integer getNextNumber() {
    return getNextNumber(null);
  }

  /**
   * Gets the highest Rechnungsnummer.
   *
   * @param rechnung wird benötigt, damit geschaut werden kann, ob diese Rechnung ggf. schon existiert. Wenn sie schon
   *                 eine Nummer hatte, so kann verhindert werden, dass sie eine nächst höhere Nummer bekommt. Eine solche
   *                 Rechnung bekommt die alte Nummer wieder zugeordnet.
   */
  @SuppressWarnings("unchecked")
  public Integer getNextNumber(final RechnungDO rechnung) {
    if (rechnung != null && rechnung.getId() != null) {
      final RechnungDO orig = internalGetById(rechnung.getId());
      if (orig.getNummer() != null) {
        rechnung.setNummer(orig.getNummer());
        return orig.getNummer();
      }
    }
    final List<Integer> list = em.createQuery("select max(t.nummer) from RechnungDO t").getResultList();
    Validate.notNull(list);
    if (list.size() == 0 || list.get(0) == null) {
      log.info("First entry of RechnungDO");
      return START_NUMBER;
    }
    Integer number = list.get(0);
    return ++number;
  }

  /**
   * Gets history entries of super and adds all history entries of the RechnungsPositionDO children.
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final RechnungDO obj) {
    final List<DisplayHistoryEntry> list = super.getDisplayHistoryEntries(obj);
    if (!hasLoggedInUserHistoryAccess(obj, false)) {
      return list;
    }
    if (CollectionUtils.isNotEmpty(obj.getPositionen())) {
      for (final RechnungsPositionDO position : obj.getPositionen()) {
        final List<DisplayHistoryEntry> entries = internalGetDisplayHistoryEntries(position);
        for (final DisplayHistoryEntry entry : entries) {
          final String propertyName = entry.getPropertyName();
          if (propertyName != null) {
            entry.setPropertyName("#" + position.getNumber() + ":" + entry.getPropertyName()); // Prepend number of positon.
          } else {
            entry.setPropertyName("#" + position.getNumber());
          }
        }
        list.addAll(entries);
        if (CollectionUtils.isNotEmpty(position.getKostZuweisungen())) {
          for (final KostZuweisungDO zuweisung : position.getKostZuweisungen()) {
            final List<DisplayHistoryEntry> kostEntries = internalGetDisplayHistoryEntries(zuweisung);
            for (final DisplayHistoryEntry entry : kostEntries) {
              final String propertyName = entry.getPropertyName();
              if (propertyName != null) {
                entry.setPropertyName(
                        "#" + position.getNumber() + ".kost#" + zuweisung.getIndex() + ":" + entry.getPropertyName()); // Prepend
                // number of positon and index of zuweisung.
              } else {
                entry.setPropertyName("#" + position.getNumber() + ".kost#" + zuweisung.getIndex());
              }
            }
            list.addAll(kostEntries);
          }
        }
      }
    }
    list.sort(new Comparator<DisplayHistoryEntry>() {
      @Override
      public int compare(final DisplayHistoryEntry o1, final DisplayHistoryEntry o2) {
        return (o2.getTimestamp().compareTo(o1.getTimestamp()));
      }
    });
    return list;
  }

  @Override
  protected Class<?>[] getAdditionalHistorySearchDOs() {
    return ADDITIONAL_SEARCH_DOS;
  }

  /**
   * Returns also true, if idSet contains the id of any order position.
   */
  @Override
  public boolean contains(final Set<Integer> idSet, final RechnungDO entry) {
    if (super.contains(idSet, entry)) {
      return true;
    }
    for (final RechnungsPositionDO pos : entry.getPositionen()) {
      if (idSet.contains(pos.getId())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public RechnungDO newInstance() {
    return new RechnungDO();
  }
}
