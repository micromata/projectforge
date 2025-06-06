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

package org.projectforge.rest.config;

import org.projectforge.model.rest.RestPaths;

public class Rest {
  public static final String URL = "/" + RestPaths.REST;
  public static final String PUBLIC_URL = "/" + RestPaths.REST_PUBLIC;

  public static final String CALENDAR_EXPORT_BASE_URI = "/export/ProjectForge.ics"; // See CalendarFeedService

  public static final String SMS_BASE_URI = "/export/sms"; // See MessagingServiceRet
}
