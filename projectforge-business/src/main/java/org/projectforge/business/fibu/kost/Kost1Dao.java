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

import org.projectforge.business.fibu.ProjektStatus;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class Kost1Dao extends BaseDao<Kost1DO> {
  public static final UserRightId USER_RIGHT_ID = UserRightId.FIBU_COST_UNIT;

  @Autowired
  private KostCache kostCache;

  public Kost1Dao() {
    super(Kost1DO.class);
    userRightId = USER_RIGHT_ID;
  }

  /**
   * @param kostString Format ######## or #.###.##.## is supported.
   * @see #getKost1(int, int, int, int)
   */
  public Kost1DO getKost1(final String kostString) {
    final int[] kost = KostHelper.parseKostString(kostString);
    if (kost == null) {
      return null;
    }
    return getKost1(kost[0], kost[1], kost[2], kost[3]);
  }

  public Kost1DO getKost1(final int nummernkreis, final int bereich, final int teilbereich, final int endziffer) {
    return SQLHelper.ensureUniqueResult(em
            .createNamedQuery(Kost1DO.FIND_BY_NK_BEREICH_TEILBEREICH_ENDZIFFER, Kost1DO.class)
            .setParameter("nummernkreis", nummernkreis)
            .setParameter("bereich", bereich)
            .setParameter("teilbereich", teilbereich)
            .setParameter("endziffer", endziffer));
  }

  @Override
  public List<Kost1DO> getList(final BaseSearchFilter filter) {
    final KostFilter myFilter;
    if (filter instanceof KostFilter) {
      myFilter = (KostFilter) filter;
    } else {
      myFilter = new KostFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (myFilter.isActive()) {
      queryFilter.add(QueryFilter.eq("kostentraegerStatus", KostentraegerStatus.ACTIVE));
    } else if (myFilter.isNonActive()) {
      queryFilter.add(QueryFilter.eq("kostentraegerStatus", KostentraegerStatus.NONACTIVE));
    } else if (myFilter.isEnded()) {
      queryFilter.add(QueryFilter.eq("kostentraegerStatus", KostentraegerStatus.ENDED));
    } else if (myFilter.isNotEnded()) {
      queryFilter.add(QueryFilter.or(QueryFilter.ne("kostentraegerStatus", ProjektStatus.ENDED),
              QueryFilter.isNull("kostentraegerStatus")));
    }
    queryFilter.addOrder(SortProperty.asc("nummernkreis")).addOrder(SortProperty.asc("bereich")).addOrder(SortProperty.asc("teilbereich"))
            .addOrder(SortProperty.asc("endziffer"));
    return getList(queryFilter);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void onSaveOrModify(final Kost1DO obj) {
    Kost1DO other = null;
    if (obj.getId() == null) {
      // New entry
      other = getKost1(obj.getNummernkreis(), obj.getBereich(), obj.getTeilbereich(), obj.getEndziffer());
    } else {
      // entry already exists. Check maybe changed:
      other = SQLHelper.ensureUniqueResult(em.createNamedQuery(Kost1DO.FIND_OTHER_BY_NK_BEREICH_TEILBEREICH_ENDZIFFER, Kost1DO.class)
              .setParameter("nummernkreis", obj.getNummernkreis())
              .setParameter("bereich", obj.getBereich())
              .setParameter("teilbereich", obj.getTeilbereich())
              .setParameter("endziffer", obj.getEndziffer())
              .setParameter("id", obj.getId()));
    }
    if (other != null) {
      throw new UserException("fibu.kost.error.collision");
    }
  }

  @Override
  protected void afterSaveOrModify(final Kost1DO kost1) {
    super.afterSaveOrModify(kost1);
    kostCache.updateKost1(kost1);
  }

  @Override
  public Kost1DO newInstance() {
    return new Kost1DO();
  }
}
