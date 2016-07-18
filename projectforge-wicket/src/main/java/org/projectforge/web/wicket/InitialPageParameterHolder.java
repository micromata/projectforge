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

package org.projectforge.web.wicket;

class InitialPageParameterHolder
{
  final String prefix, property, alias;

  /**
   * "p.taskId|task" represents a property which is set via the ISelectCallerPage.<br/>
   * "f.taskId|task" represents a property which is set via the filter object (in list pages):
   * {@link AbstractListPage#getFilterObjectForInitialParameters}.</br>
   * "pageSize|ps" represents a property which is set directly to the data object:
   * {@link AbstractSecuredPage#getDataObjectForInitialParameters()}.
   * 
   * @param propertyString
   */
  InitialPageParameterHolder(final String propertyString)
  {
    final int pos1 = propertyString.indexOf('.');
    if (pos1 >= 0) {
      prefix = propertyString.substring(0, pos1 + 1);
    } else {
      prefix = "";
    }
    final int pos2 = propertyString.indexOf('|');
    if (pos2 >= 0) {
      property = propertyString.substring(pos1 + 1, pos2);
      alias = propertyString.substring(pos2 + 1);
    } else {
      // No alias given.
      property = propertyString.substring(pos1 + 1);
      alias = property;
    }
  }

  boolean isDataObjectParameter()
  {
    return "".equals(prefix);
  }

  boolean isFilterParameter()
  {
    return prefix.startsWith("f.");
  }

  boolean isCallerPageParameter()
  {
    return prefix.startsWith("p.");
  }
}
