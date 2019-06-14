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

package org.projectforge.business.fibu.kost;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.ProjektStatus;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS, isolation = Isolation.REPEATABLE_READ)
public class Kost2Dao extends BaseDao<Kost2DO>
{
  public static final UserRightId USER_RIGHT_ID = UserRightId.FIBU_COST_UNIT;

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "projekt.name", "projekt.kunde.name",
      "nummer" };

  @Autowired
  private ProjektDao projektDao;

  @Autowired
  private Kost2ArtDao kost2ArtDao;

  @Autowired
  private KostCache kostCache;

  public Kost2Dao()
  {
    super(Kost2DO.class);
    userRightId = USER_RIGHT_ID;
  }

  /**
   * @return the kostCache
   */
  public KostCache getKostCache()
  {
    return kostCache;
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * @param kost2
   * @param projektId If null, then projekt will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setProjekt(final Kost2DO kost2, final Integer projektId)
  {
    final ProjektDO projekt = projektDao.getOrLoad(projektId);
    if (projekt != null) {
      kost2.setProjekt(projekt);
      kost2.setNummernkreis(projekt.getNummernkreis());
      kost2.setBereich(projekt.getBereich());
      kost2.setTeilbereich(projekt.getNummer());
    }
  }

  /**
   * @param kost2
   * @param kost2ArtId If null, then kost2Art will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setKost2Art(final Kost2DO kost2, final Integer kost2ArtId)
  {
    final Kost2ArtDO kost2Art = kost2ArtDao.getOrLoad(kost2ArtId);
    kost2.setKost2Art(kost2Art);
  }

  /**
   * @param kostString Format ######## or #.###.##.## is supported.
   * @see #getKost2(int, int, int, int)
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public Kost2DO getKost2(final String kostString)
  {
    final int[] kost = KostHelper.parseKostString(kostString);
    if (kost == null) {
      return null;
    }
    return getKost2(kost[0], kost[1], kost[2], kost[3]);
  }

  @SuppressWarnings("unchecked")
  public Kost2DO getKost2(final int nummernkreis, final int bereich, final int teilbereich, final int kost2Art)
  {
    final List<Kost2DO> list = (List<Kost2DO>) getHibernateTemplate().find(
        "from Kost2DO k where k.nummernkreis=? and k.bereich=? and k.teilbereich=? and k.kost2Art.id=?",
        new Object[] { nummernkreis, bereich, teilbereich, kost2Art });
    if (CollectionUtils.isEmpty(list) == true) {
      return null;
    }
    return list.get(0);
  }

  @SuppressWarnings("unchecked")
  public List<Kost2DO> getActiveKost2(final int nummernkreis, final int bereich, final int teilbereich)
  {
    final List<Kost2DO> list = (List<Kost2DO>) getHibernateTemplate()
        .find(
            "from Kost2DO k where k.nummernkreis=? and k.bereich=? and k.teilbereich=? and (k.kostentraegerStatus='ACTIVE' or k.kostentraegerStatus is null) order by k.kost2Art.id",
            new Object[] { nummernkreis, bereich, teilbereich });
    if (CollectionUtils.isEmpty(list) == true) {
      return null;
    }
    return list;
  }

  /**
   * @param projekt
   * @see #getActiveKost2(int, int, int)
   */
  public List<Kost2DO> getActiveKost2(final ProjektDO projekt)
  {
    if (projekt == null) {
      return null;
    }
    return getActiveKost2(projekt.getNummernkreis(), projekt.getBereich(), projekt.getTeilbereich());
  }

  @Override
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<Kost2DO> getList(final BaseSearchFilter filter)
  {
    final KostFilter myFilter;
    if (filter instanceof KostFilter) {
      myFilter = (KostFilter) filter;
    } else {
      myFilter = new KostFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    queryFilter.createAlias("kost2Art", "art");
    if (myFilter.isActive() == true) {
      queryFilter.add(Restrictions.eq("kostentraegerStatus", KostentraegerStatus.ACTIVE));
    } else if (myFilter.isNonActive() == true) {
      queryFilter.add(Restrictions.eq("kostentraegerStatus", KostentraegerStatus.NONACTIVE));
    } else if (myFilter.isEnded() == true) {
      queryFilter.add(Restrictions.eq("kostentraegerStatus", KostentraegerStatus.ENDED));
    } else if (myFilter.isNotEnded() == true) {
      queryFilter.add(Restrictions.or(Restrictions.ne("kostentraegerStatus", ProjektStatus.ENDED),
          Restrictions.isNull("kostentraegerStatus")));
    }
    queryFilter.addOrder(Order.asc("nummernkreis")).addOrder(Order.asc("bereich")).addOrder(Order.asc("teilbereich"))
        .addOrder(
            Order.asc("art.id"));
    return getList(queryFilter);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void onSaveOrModify(final Kost2DO obj)
  {
    if (obj.getProjektId() != null) {
      // Projekt ist gegeben. Dann müssen auch die Ziffern stimmen:
      final ProjektDO projekt = projektDao.getById(obj.getProjektId()); // Bei Neuanlage ist Projekt nicht wirklich gebunden.
      if (projekt.getNummernkreis() != obj.getNummernkreis()
          || projekt.getBereich() != obj.getBereich()
          || projekt.getNummer() != obj.getTeilbereich()) {
        throw new UserException("Inkonsistenz bei Kost2: "
            + obj.getNummernkreis()
            + "."
            + obj.getBereich()
            + "."
            + obj.getTeilbereich()
            + " != "
            + projekt.getNummernkreis()
            + "."
            + projekt.getBereich()
            + "."
            + projekt.getNummer()
            + " (Projekt)");
      }
    } else if (obj.getNummernkreis() == 4 || obj.getNummernkreis() == 5) {
      throw new UserException("fibu.kost2.error.projektNeededForNummernkreis");
    }
    List<Kost2DO> list = null;
    final String sql = "from Kost2DO k where k.nummernkreis = ? and k.bereich = ? and k.teilbereich = ? and k.kost2Art.id = ?";
    if (obj.getId() == null) {
      // New kost entry
      list = (List<Kost2DO>) getHibernateTemplate().find(sql,
          new Object[] { obj.getNummernkreis(), obj.getBereich(), obj.getTeilbereich(), obj.getKost2ArtId() });
    } else {
      // kost entry already exists. Check maybe changed:
      list = (List<Kost2DO>) getHibernateTemplate().find(sql + " and pk <> ?",
          new Object[] { obj.getNummernkreis(), obj.getBereich(), obj.getTeilbereich(), obj.getKost2ArtId(),
              obj.getId() });
    }
    if (CollectionUtils.isNotEmpty(list) == true) {
      throw new UserException("fibu.kost.error.collision");
    }
  }

  @Override
  protected void afterSaveOrModify(final Kost2DO kost2)
  {
    super.afterSaveOrModify(kost2);
    getKostCache().updateKost2(kost2);
  }

  @Override
  public Kost2DO newInstance()
  {
    return new Kost2DO();
  }
}
