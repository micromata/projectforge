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

package org.projectforge.model.rest;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class RestPaths
{
  public static String buildPath(final String... pathElements)
  {
    if (pathElements == null) {
      return "";
    }
    final StringBuilder sb = new StringBuilder();
    sb.append("/" + REST);
    for (final String pathElement : pathElements) {
      sb.append("/").append(pathElement);
    }
    return sb.toString();
  }

  public static final String buildListPath(final String path)
  {
    return "/" + REST + "/" + path + "/" + LIST;
  }

  public static final String buildTreePath(final String path)
  {
    return "/" + REST + "/" + path + "/" + TREE;
  }

  public static final String REST = "rest";

  public static final String PUBLIC_REST = "publicRest";

  public static final String ADDRESS = "address";

  public static final String AUTHENTICATE = "authenticate";

  public static final String AUTHENTICATE_GET_TOKEN_METHOD = "getToken";

  public static final String AUTHENTICATE_GET_TOKEN = AUTHENTICATE + "/" + AUTHENTICATE_GET_TOKEN_METHOD;

  public static final String AUTHENTICATE_INITIAL_CONTACT_METHOD = "initialContact";

  public static final String AUTHENTICATE_INITIAL_CONTACT = AUTHENTICATE + "/" + AUTHENTICATE_INITIAL_CONTACT_METHOD;

  public static final String TASK = "task";

  public static final String TIMESHEET = "timesheet";

  public static final String TIMESHEET_TEMPLATE = "timesheetTemplate";

  public static final String LIST = "list";

  public static final String SAVE = "save";

  public static final String UPDATE = "update";

  public static final String SAVE_OR_UDATE = SAVE + "or" + UPDATE;

  public static final String DELETE = "delete";

  public static final String MARK_AS_DELETED = "markAsDeleted";

  public static final String UNDELETE = "undelete";

  public static final String TREE = "tree";

  public static final String TEAMCAL = "teamcal";

  public static final String TEAMEVENTS = "teamevents";

  public static final String VERSION_CHECK = "versionCheck";
}
