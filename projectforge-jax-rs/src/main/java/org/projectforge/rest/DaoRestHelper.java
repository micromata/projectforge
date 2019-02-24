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

package org.projectforge.rest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.projectforge.business.address.*;
import org.projectforge.business.book.BookDao;
import org.projectforge.business.book.BookFilter;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.model.rest.AddressObject;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.rest.converter.AddressDOConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.getUserId;
public class DaoRestHelper {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DaoRestHelper.class);

  static void setFilter(BaseSearchFilter filter, String searchString, Long modifiedSince, Integer modfiedByUserId,
                        Long startTimeOfModification, Long stopTimeOfModification) {
    filter.setSearchString(searchString);
    if (modifiedSince != null) {
      filter.setModifiedSince(new Date(modifiedSince));
    }
    if (modfiedByUserId != null) {
      filter.setModifiedByUserId(modfiedByUserId);
    }
    if (startTimeOfModification != null) {
      filter.setStartTimeOfModification(new Date(startTimeOfModification));
    }
    if (stopTimeOfModification != null) {
      filter.setStartTimeOfModification(new Date(stopTimeOfModification));
    }
  }
}
