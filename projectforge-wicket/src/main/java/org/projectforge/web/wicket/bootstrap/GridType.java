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

package org.projectforge.web.wicket.bootstrap;

import org.projectforge.common.StringHelper;

/**
 * Used for defining class attribute value for elements.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public enum GridType
{
  CONTAINER("container"),CONTAINER_FLUID("container-fluid"), FIRST("first"), HAS_CHILDS("has-childs"), HAS_SIBLINGS("has-siblings"), NOT_FIRST("not-first"), //
  OFFSET1("col-md-offset-1"), OFFSET2("col-md-offset-2"), OFFSET3("col-md-offset-3"), //
  ROW("row"), ROW_HAS_CHILDS("has-sub-rows");

  private String classAttrValue;

  public static String gridTypesToClassString(final GridType... gridTypes)
  {
    if (gridTypes == null || gridTypes.length == 0) {
      return null;
    }
    if (gridTypes.length == 1) {
      return gridTypes[0].classAttrValue;
    }
    boolean first = true;
    final StringBuffer buf = new StringBuffer();
    for (final GridType gridType : gridTypes) {
      first = StringHelper.append(buf, first, gridType.classAttrValue, " ");
    }
    return buf.toString();
  }

  public String getClassAttrValue()
  {
    return classAttrValue;
  }

  private GridType(final String classAttrValue)
  {
    this.classAttrValue = classAttrValue;
  }
}
