/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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
 * @author Kai Reinhard
 */
public class RestPaths
{
  public static final String REST = "rs";

  public static final String REST_PUBLIC = "rsPublic";

  public static final String REST_EXCEL_SUB_PATH = "exportAsExcel";

  public static final String REST_PDF_SUB_PATH = "exportAsPdf";

  public static final String REST_START_MULTI_SELECTION = "startMultiSelection";

  public static final String TASK = "task";

  public static final String TIMESHEET_TEMPLATE = "timesheetTemplate";

  public static final String LIST = "list";

  public static final String LIST_PAGE = "listPage";

  public static final String CANCEL = "cancel";

  public static final String CANCEL_MULTI_SELECTION = "cancelMultiSelection";

  public static final String EDIT = "edit";

  public static final String SAVE = "save";

  public static final String UPDATE = "update";

  public static final String SAVE_OR_UDATE = SAVE + "or" + UPDATE;

  public static final String DELETE = "delete";

  public static final String MARK_AS_DELETED = "markAsDeleted";

  public static final String FORCE_DELETE = "forceDelete";

  public static final String UNDELETE = "undelete";

  public static final String CLONE = "clone";

  /**
   * Layout free counterpart of {@link #CLONE}: answers the prepared clone as plain JSON, without a
   * UILayout and without saving anything. A path of its own because {@link #CLONE} is mapped by
   * AbstractPagesRest, which extends the class serving this one - the same path there would be
   * ambiguous for every legacy page.
   */
  public static final String CLONE_DATA = "cloneData";

  public static final String SET_COLUMN_STATES = "setColumnStates";

  /**
   * Reads back what SET_COLUMN_STATES stored, for pages that aren't built from a UILayout.
   */
  public static final String COLUMN_STATES = "columnStates";

  public static final String WATCH_FIELDS = "watchFields";

  public static final String FILTER_RESET = "filterReset";
}
