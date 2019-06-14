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

package org.projectforge.framework.i18n;

import java.util.Arrays;
import java.util.Objects;

/**
 * A i18n key along with its parameters (optional).
 */
public class I18nKeyAndParams
{
  private final String key;

  private final Object[] params;

  public I18nKeyAndParams(final String key, final Object... params)
  {
    this.key = Objects.requireNonNull(key);
    this.params = (params != null) ? params : new Object[0];
  }

  public String getKey()
  {
    return key;
  }

  public Object[] getParams()
  {
    return params;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o instanceof I18nKeyAndParams) == false) {
      return false;
    }

    final I18nKeyAndParams other = (I18nKeyAndParams) o;
    return Objects.equals(key, other.key) &&
        Arrays.equals(params, other.params);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(key, params);
  }
}
