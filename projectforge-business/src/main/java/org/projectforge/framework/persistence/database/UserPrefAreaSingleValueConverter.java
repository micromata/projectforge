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

package org.projectforge.framework.persistence.database;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.user.UserPrefAreaRegistry;
import org.projectforge.framework.persistence.user.api.UserPrefArea;

import com.thoughtworks.xstream.converters.SingleValueConverter;

/**
 * Converts UserRightArea from and to strings.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class UserPrefAreaSingleValueConverter implements SingleValueConverter
{
  @SuppressWarnings("unchecked")
  @Override
  public boolean canConvert(final Class type)
  {
    return UserPrefArea.class.isAssignableFrom(type);
  }

  @Override
  public String toString(final Object obj)
  {
    if (obj == null) {
      return null;
    }
    return ((UserPrefArea) obj).getId();
  }

  @Override
  public Object fromString(final String str)
  {
    if (StringUtils.isBlank(str) == true) {
      return null;
    }
    return UserPrefAreaRegistry.instance().getEntry(str.trim());
  }
}
