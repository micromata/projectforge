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

package org.projectforge.web.wicket.flowlayout;


/**
 * Used by IconPanels.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public enum IconType
{
  ACCEPT("ok-sign"), //
  ALERT("warning-sign"), //
  BUILDING("building"), //
  CALENDAR("calendar"), //
  COG("cog"), //
  DENY("minus-sign"), //
  DOCUMENT("file"), //
  DOWNLOAD("download"), //
  EDIT("pencil"), //
  FOLDER_OPEN("folder-open"), //
  GLOBE("globe"), //
  GOTO("hand-right"), //
  HELP("info-sign"), //
  HOME("home"), //
  JIRA_SUPPORT("star"), //
  KEYBOARD("th"), //
  LIST("list"), //
  MINUS_SIGN("minus-sign"), //
  MODIFIED("exclamation-sign"), //
  PHONE("phone"), //
  PHONE_MOBILE("mobile-phone"), //
  PLUS("plus"), //
  PLUS_SIGN("plus-sign"), //
  PRINT("print"), //
  REFRESH("refresh"), //
  REMOVE("remove"), //
  REMOVE_SIGN("remove-sign"), //
  SEARCH("search"), //
  SUBSCRIPTION("globe"), //
  SWAP("retweet"), //
  TABLET("tablet"), //
  TASK("tasks"), //
  TRASH("trash"), //
  UPLOAD("upload"), //
  USER("user"), //
  WRENCH("wrench"), //
  ZOOM_IN("zoom-in"), //
  ZOOM_OUT("zoom-out"), //

  DELETE("trash"), //
  INSERT("save"), //
  SELECT("file"), //
  UPDATE("edit");

  private String cssIdentifier;

  public String getClassAttrValue()
  {
    return "glyphicon glyphicon-" + cssIdentifier;
  }

  public boolean isIn(final IconType... type)
  {
    for (final IconType t : type) {
      if (this == t) {
        return true;
      }
    }
    return false;
  }

  private IconType(final String classAttrValue)
  {
    this.cssIdentifier = classAttrValue;
  }
}
