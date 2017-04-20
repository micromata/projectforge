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

package org.projectforge.business.fibu;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.persistence.TypedQuery;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class EingangsrechnungDao extends BaseDao<EingangsrechnungDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EingangsrechnungDao.class);

  public static final UserRightId USER_RIGHT_ID = UserRightId.FIBU_EINGANGSRECHNUNGEN;

  private static final Class<?>[] ADDITIONAL_SEARCH_DOS = new Class[] { EingangsrechnungsPositionDO.class };

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "positionen.text" };

  @Autowired
  private KontoDao kontoDao;

  @Autowired
  private PfEmgrFactory pfEmgrFactory;

  public EingangsrechnungDao()
  {
    super(EingangsrechnungDO.class);
    userRightId = USER_RIGHT_ID;
  }

  /**
   * List of all years with invoices: select min(datum), max(datum) from t_fibu_rechnung.
   *
   * @return
   */
  @SuppressWarnings("unchecked")
  public int[] getYears()
  {
    final List<Object[]> list = getSession().createQuery("select min(datum), max(datum) from EingangsrechnungDO t")
        .list();
    return SQLHelper.getYears(list);
  }

  public EingangsrechnungsStatistik buildStatistik(final List<EingangsrechnungDO> list)
  {
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
  public void setKonto(final EingangsrechnungDO eingangsrechnung, final Integer kontoId)
  {
    final KontoDO konto = kontoDao.getOrLoad(kontoId);
    eingangsrechnung.setKonto(konto);
  }

  /**
   * Sets the scales of percentage and currency amounts. <br/>
   * Gutschriftsanzeigen dürfen keine Rechnungsnummer haben. Wenn eine Rechnungsnummer für neue Rechnungen gegeben
   * wurde, so muss sie fortlaufend sein. Berechnet das Zahlungsziel in Tagen, wenn nicht gesetzt, damit es indiziert
   * wird.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#onSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void onSaveOrModify(final EingangsrechnungDO obj)
  {
    if (obj.getZahlBetrag() != null) {
      obj.setZahlBetrag(obj.getZahlBetrag().setScale(2, RoundingMode.HALF_UP));
    }
    obj.recalculate();
    if (CollectionUtils.isEmpty(obj.getPositionen()) == true) {
      throw new UserException("fibu.rechnung.error.rechnungHatKeinePositionen");
    }
    final int size = obj.getPositionen().size();
    for (int i = size - 1; i > 0; i--) {
      // Don't remove first position, remove only the last empty positions.
      final EingangsrechnungsPositionDO position = obj.getPositionen().get(i);
      if (position.getId() == null && position.isEmpty() == true) {
        obj.getPositionen().remove(i);
      } else {
        break;
      }
    }
    RechnungDao.writeUiStatusToXml(obj);
  }

  @Override
  public void afterLoad(final EingangsrechnungDO obj)
  {
    RechnungDao.readUiStatusFromXml(obj);
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  @Override
  public List<EingangsrechnungDO> getList(final BaseSearchFilter filter)
  {
    final RechnungFilter myFilter;
    if (filter instanceof RechnungFilter) {
      myFilter = (RechnungFilter) filter;
    } else {
      myFilter = new RechnungFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (myFilter.getFromDate() != null || myFilter.getToDate() != null) {
      if (myFilter.getFromDate() != null && myFilter.getToDate() != null) {
        queryFilter.add(Restrictions.between("datum", myFilter.getFromDate(), myFilter.getToDate()));
      } else if (myFilter.getFromDate() != null) {
        queryFilter.add(Restrictions.ge("datum", myFilter.getFromDate()));
      } else if (myFilter.getToDate() != null) {
        queryFilter.add(Restrictions.le("datum", myFilter.getToDate()));
      }
    } else {
      queryFilter.setYearAndMonth("datum", myFilter.getYear(), myFilter.getMonth());
    }
    queryFilter.addOrder(Order.desc("datum"));
    queryFilter.addOrder(Order.desc("kreditor"));
    final List<EingangsrechnungDO> list = getList(queryFilter);
    if (myFilter.isShowAll() == true || myFilter.isDeleted() == true) {
      return list;
    }
    final List<EingangsrechnungDO> result = new ArrayList<EingangsrechnungDO>();
    for (final EingangsrechnungDO rechnung : list) {
      if (myFilter.isShowUnbezahlt() == true) {
        if (rechnung.isBezahlt() == false) {
          result.add(rechnung);
        }
      } else if (myFilter.isShowBezahlt() == true) {
        if (rechnung.isBezahlt() == true) {
          result.add(rechnung);
        }
      } else if (myFilter.isShowUeberFaellig() == true) {
        if (rechnung.isUeberfaellig() == true) {
          result.add(rechnung);
        }
      } else {
        log.debug("Unknown filter setting (probably caused by serialize/de-serialize problems): " + myFilter.listType);
      }
    }
    return result;
  }

  /**
   * Gets history entries of super and adds all history entries of the EingangsrechnungsPositionDO childs.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#getDisplayHistoryEntries(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final EingangsrechnungDO obj)
  {
    final List<DisplayHistoryEntry> list = super.getDisplayHistoryEntries(obj);
    if (hasLoggedInUserHistoryAccess(obj, false) == false) {
      return list;
    }
    if (CollectionUtils.isNotEmpty(obj.getPositionen()) == true) {
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
        if (CollectionUtils.isNotEmpty(position.getKostZuweisungen()) == true) {
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
    Collections.sort(list, new Comparator<DisplayHistoryEntry>()
    {
      @Override
      public int compare(final DisplayHistoryEntry o1, final DisplayHistoryEntry o2)
      {
        return (o2.getTimestamp().compareTo(o1.getTimestamp()));
      }
    });
    return list;
  }

  @Override
  protected Class<?>[] getAdditionalHistorySearchDOs()
  {
    return ADDITIONAL_SEARCH_DOS;
  }

  /**
   * Returns also true, if idSet contains the id of any order position.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#contains(java.util.Set,
   * org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected boolean contains(final Set<Integer> idSet, final EingangsrechnungDO entry)
  {
    if (super.contains(idSet, entry) == true) {
      return true;
    }
    for (final EingangsrechnungsPositionDO pos : entry.getPositionen()) {
      if (idSet.contains(pos.getId()) == true) {
        return true;
      }
    }
    return false;
  }

  @Override
  public List<EingangsrechnungDO> sort(final List<EingangsrechnungDO> list)
  {
    Collections.sort(list);
    return list;
  }

  @Override
  public EingangsrechnungDO newInstance()
  {
    return new EingangsrechnungDO();
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#useOwnCriteriaCacheRegion()
   */
  @Override
  protected boolean useOwnCriteriaCacheRegion()
  {
    return true;
  }

  public EingangsrechnungDO findNewestByKreditor(final String kreditor)
  {
    return pfEmgrFactory.runRoTrans(emgr -> {
      final String sql = "SELECT er FROM EingangsrechnungDO er WHERE er.kreditor = :kreditor AND er.deleted = false ORDER BY er.created DESC";
      final TypedQuery<EingangsrechnungDO> query = emgr.createQueryDetached(EingangsrechnungDO.class, sql, "kreditor", kreditor);
      final List<EingangsrechnungDO> resultList = query.setMaxResults(1).getResultList();
      return (resultList != null && resultList.size() > 0) ? resultList.get(0) : null;
    });
  }
}
