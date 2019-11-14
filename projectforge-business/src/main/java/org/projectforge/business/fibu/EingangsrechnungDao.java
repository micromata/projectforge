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
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.math.RoundingMode;
import java.util.*;

@Repository
public class EingangsrechnungDao extends BaseDao<EingangsrechnungDO> {
  public static final UserRightId USER_RIGHT_ID = UserRightId.FIBU_EINGANGSRECHNUNGEN;
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EingangsrechnungDao.class);
  private static final Class<?>[] ADDITIONAL_SEARCH_DOS = new Class[]{EingangsrechnungsPositionDO.class};

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"positionen.text"};

  @Autowired
  private KontoDao kontoDao;

  @Autowired
  private PfEmgrFactory pfEmgrFactory;

  public EingangsrechnungDao() {
    super(EingangsrechnungDO.class);
    userRightId = USER_RIGHT_ID;
  }

  /**
   * List of all years with invoices: select min(datum), max(datum) from t_fibu_rechnung.
   */
  public int[] getYears() {
    final Tuple minMaxDate = SQLHelper.ensureUniqueResult(em.createNamedQuery(EingangsrechnungDO.SELECT_MIN_MAX_DATE, Tuple.class));
    return SQLHelper.getYears((java.sql.Date) minMaxDate.get(0), (java.sql.Date) minMaxDate.get(1));
  }

  public EingangsrechnungsStatistik buildStatistik(final List<EingangsrechnungDO> list) {
    final EingangsrechnungsStatistik stats = new EingangsrechnungsStatistik();
    if (list == null) {
      return stats;
    }
    for (final EingangsrechnungDO rechnung : list) {
      stats.add(rechnung);
    }
    return stats;
  }

  /**
   * @param eingangsrechnung
   * @param kontoId          If null, then konto will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setKonto(final EingangsrechnungDO eingangsrechnung, final Integer kontoId) {
    final KontoDO konto = kontoDao.getOrLoad(kontoId);
    eingangsrechnung.setKonto(konto);
  }

  /**
   * Sets the scales of percentage and currency amounts. <br/>
   * Gutschriftsanzeigen dürfen keine Rechnungsnummer haben. Wenn eine Rechnungsnummer für neue Rechnungen gegeben
   * wurde, so muss sie fortlaufend sein. Berechnet das Zahlungsziel in Tagen, wenn nicht gesetzt, damit es indiziert
   * wird.
   */
  @Override
  protected void onSaveOrModify(final EingangsrechnungDO rechnung) {
    AuftragAndRechnungDaoHelper.onSaveOrModify(rechnung);

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
      final EingangsrechnungsPositionDO position = rechnung.getPositionen().get(i);
      if (position.getId() == null && position.isEmpty()) {
        rechnung.getPositionen().remove(i);
      } else {
        break;
      }
    }
    RechnungDao.writeUiStatusToXml(rechnung);
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  @Override
  public List<EingangsrechnungDO> getList(final BaseSearchFilter filter) {
    final EingangsrechnungListFilter myFilter;
    if (filter instanceof EingangsrechnungListFilter) {
      myFilter = (EingangsrechnungListFilter) filter;
    } else {
      myFilter = new EingangsrechnungListFilter(filter);
    }

    final QueryFilter queryFilter = AuftragAndRechnungDaoHelper.createQueryFilterWithDateRestriction(myFilter);

    if (myFilter.getPaymentTypes() != null && myFilter.getPaymentTypes().size() > 0) {
      queryFilter.add(QueryFilter.isIn("paymentType", myFilter.getPaymentTypes()));
    }

    queryFilter.addOrder(SortProperty.desc("datum"));
    queryFilter.addOrder(SortProperty.desc("kreditor"));

    final List<EingangsrechnungDO> list = getList(queryFilter);
    if (myFilter.isShowAll() || myFilter.isDeleted()) {
      return list;
    }

    final List<EingangsrechnungDO> result = new ArrayList<>();
    for (final EingangsrechnungDO rechnung : list) {
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
        log.debug("Unknown filter setting (probably caused by serialize/de-serialize problems): " + myFilter.listType);
      }
    }
    return result;
  }

  /**
   * Gets history entries of super and adds all history entries of the EingangsrechnungsPositionDO children.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#getDisplayHistoryEntries(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final EingangsrechnungDO obj) {
    final List<DisplayHistoryEntry> list = super.getDisplayHistoryEntries(obj);
    if (!hasLoggedInUserHistoryAccess(obj, false)) {
      return list;
    }
    if (CollectionUtils.isNotEmpty(obj.getPositionen())) {
      for (final EingangsrechnungsPositionDO position : obj.getPositionen()) {
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
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#contains(Set, ExtendedBaseDO)
   */
  @Override
  public boolean contains(final Set<Integer> idSet, final EingangsrechnungDO entry) {
    if (super.contains(idSet, entry)) {
      return true;
    }
    for (final EingangsrechnungsPositionDO pos : entry.getPositionen()) {
      if (idSet.contains(pos.getId())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public List<EingangsrechnungDO> sort(final List<EingangsrechnungDO> list) {
    Collections.sort(list);
    return list;
  }

  @Override
  public EingangsrechnungDO newInstance() {
    return new EingangsrechnungDO();
  }

  public EingangsrechnungDO findNewestByKreditor(final String kreditor) {
    return pfEmgrFactory.runRoTrans(emgr -> {
      final String sql = "SELECT er FROM EingangsrechnungDO er WHERE er.kreditor = :kreditor AND er.deleted = false ORDER BY er.created DESC";
      final TypedQuery<EingangsrechnungDO> query = emgr.createQueryDetached(EingangsrechnungDO.class, sql, "kreditor", kreditor);
      final List<EingangsrechnungDO> resultList = query.setMaxResults(1).getResultList();
      return (resultList != null && resultList.size() > 0) ? resultList.get(0) : null;
    });
  }
}
