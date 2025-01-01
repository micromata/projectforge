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

package org.projectforge.renderer.custom;

import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author Sebastian Hardt (s.hardt@micromata.de) This Interface is for the different output Formatters for the PDF exporter.
 */
public abstract class Formatter
{
  /**
   * Takes a Map of Parameters and returns a Map of data for the PDF Renderer
   *
   * @param parameters
   * @return
   */
  public abstract Map<String, Object> getData(List<TimesheetDO> timeSheets, Long taskId, HttpServletRequest request,
      HttpServletResponse response, TimesheetFilter actionFilter);

  public Map<String, Object> getData(final List<TimesheetDO> timeSheets, final Long taskId, final Request request, final Response response,
      final TimesheetFilter actionFilter)
      {
    return getData(timeSheets, taskId, (HttpServletRequest) request.getContainerRequest(),
        (HttpServletResponse) response.getContainerResponse(), actionFilter);
      }

  public String getLocalizedString(final String key)
  {
    return ThreadLocalUserContext.getLocalizedString(key);
  }
}
