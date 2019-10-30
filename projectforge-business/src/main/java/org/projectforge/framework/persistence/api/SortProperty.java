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

package org.projectforge.framework.persistence.api;

public class SortProperty {
  private SortOrder sortOrder;
  private String property;

  public SortProperty(String property, SortOrder sortOrder) {
    this.property = property;
    this.sortOrder = sortOrder;
  }

  public SortOrder getSortOrder() {
    return sortOrder;
  }

  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public static SortProperty asc(String property) {
    return new SortProperty(property, SortOrder.ASCENDING);
  }

  public static SortProperty desc(String property) {
    return new SortProperty(property, SortOrder.DESCENDING);
  }
}
