/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.persistence.Tuple;
import org.apache.commons.lang3.ArrayUtils;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PosteingangDao extends BaseDao<PosteingangDO> {
    public static final UserRightId USER_RIGHT_ID = UserRightId.ORGA_INCOMING_MAIL;
    private static final String[] ENABLED_AUTOCOMPLETION_PROPERTIES = {"absender", "person", "inhalt"};

    protected PosteingangDao() {
        super(PosteingangDO.class);
        userRightId = USER_RIGHT_ID;
    }

    @Override
    public boolean isAutocompletionPropertyEnabled(String property) {
        return ArrayUtils.contains(ENABLED_AUTOCOMPLETION_PROPERTIES, property);
    }

    /**
     * List of all years with invoices: select min(datum), max(datum) from t_fibu_rechnung.
     */
    public int[] getYears() {
        final Tuple minMaxDate = persistenceService.selectNamedSingleResult(
                PosteingangDO.SELECT_MIN_MAX_DATE,
                Tuple.class);
        return SQLHelper.getYearsByTupleOfLocalDate(minMaxDate);
    }

    @Override
    public List<PosteingangDO> select(final BaseSearchFilter filter) {
        final PostFilter myFilter;
        if (filter instanceof PostFilter) {
            myFilter = (PostFilter) filter;
        } else {
            myFilter = new PostFilter(filter);
        }
        final QueryFilter queryFilter = new QueryFilter(filter);
        queryFilter.setYearAndMonth("datum", myFilter.getYear(), myFilter.getMonth());
        queryFilter.addOrder(SortProperty.desc("datum"));
        queryFilter.addOrder(SortProperty.asc("absender"));
        final List<PosteingangDO> list = select(queryFilter);
        return list;
    }

    @Override
    public PosteingangDO newInstance() {
        return new PosteingangDO();
    }
}
