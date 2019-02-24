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
import org.projectforge.business.book.BookDO;
import org.projectforge.business.book.BookDao;
import org.projectforge.business.book.BookFilter;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
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

/**
 * REST-Schnittstelle für {@link AddressDao}
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Controller
@Path(RestPaths.BOOKS)
public class BookDaoRest {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BookDaoRest.class);

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private BookDao bookDao;

  @GET
  @Path(RestPaths.LIST)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getList(@QueryParam("searchString") final String searchString,
                          @QueryParam("modifiedSince") final Long modifiedSince,
                          @QueryParam("modifiedByUser") final Integer modifiedByUserId,
                          @QueryParam("startTimeOfModification") final Long startTimeOfModification,
                          @QueryParam("stopTimeOfModification") final Long stopTimeOfModification,
                          @QueryParam("present") final Boolean present,
                          @QueryParam("missed") final Boolean missed,
                          @QueryParam("disposed") final Boolean disposed) {
    BookFilter filter = new BookFilter(new BaseSearchFilter());
    DaoRestHelper.setFilter(filter, searchString, modifiedSince, modifiedByUserId, startTimeOfModification, stopTimeOfModification);
    final List<BookDO> list = bookDao.getList(filter);
    final String json = JsonUtils.toJson(list);
    log.info("Rest call finished (" + list.size() + " books)...");
    return Response.ok(json).build();
  }

  /**
   * @param bookDO
   * @return
   */
  @PUT
  @Path(RestPaths.SAVE_OR_UDATE)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response saveOrUpdate(final BookDO bookDO) {
    BookDO origBook = bookDao.getById(bookDO.getId());

    final String json = "";
    log.info("Save or update address REST call finished.");
    return Response.ok(json).build();
  }

  @DELETE
  @Path(RestPaths.DELETE)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response delete(final AddressObject addressObject) {
    return Response.ok().build();
  }
}
