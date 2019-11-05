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

package org.projectforge.business.orga;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Repository
public class VisitorbookDao extends BaseDao<VisitorbookDO> {
  public static final UserRightId USER_RIGHT_ID = UserRightId.ORGA_VISITORBOOK;
  @Autowired
  private TimeableService timeableService;

  protected VisitorbookDao() {
    super(VisitorbookDO.class);
    userRightId = USER_RIGHT_ID;
  }

  @Override
  public List<VisitorbookDO> getList(final BaseSearchFilter filter) {
    final VisitorbookFilter myFilter;
    if (filter instanceof VisitorbookFilter) {
      myFilter = (VisitorbookFilter) filter;
    } else {
      myFilter = new VisitorbookFilter(filter);
    }

    final QueryFilter queryFilter = createQueryFilter(myFilter);
    final List<VisitorbookDO> resultList = getList(queryFilter);

    final Predicate<VisitorbookDO> afterStartTimeOrSameDay = visitor -> timeableService.getTimeableAttrRowsForGroupName(visitor, "timeofvisit").stream()
            .anyMatch(timeAttr -> !timeAttr.getStartTime().before(myFilter.getStartTime())); // before() == false -> after or same day

    final Predicate<VisitorbookDO> beforeStopTimeOrSameDay = visitor -> timeableService.getTimeableAttrRowsForGroupName(visitor, "timeofvisit").stream()
            .anyMatch(timeAttr -> !timeAttr.getStartTime().after(myFilter.getStopTime())); // after() == false -> before or same day

    if (myFilter.getStartTime() != null && myFilter.getStopTime() == null) {
      return resultList
              .stream()
              .filter(afterStartTimeOrSameDay)
              .collect(Collectors.toList());
    }

    if (myFilter.getStartTime() == null && myFilter.getStopTime() != null) {
      return resultList
              .stream()
              .filter(beforeStopTimeOrSameDay)
              .collect(Collectors.toList());
    }

    if (myFilter.getStartTime() != null && myFilter.getStopTime() != null) {
      return resultList
              .stream()
              .filter(afterStartTimeOrSameDay)
              .filter(beforeStopTimeOrSameDay)
              .collect(Collectors.toList());
    }

    // no time filter set
    return resultList;
  }

  @Override
  public VisitorbookDO newInstance() {
    return new VisitorbookDO();
  }
}
