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

package org.projectforge.business.converter;

import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.model.rest.AbstractBaseObject;

/**
 * For conversion of DefaultBaseDO to base object.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DOConverter
{
  public static void copyFields(final AbstractBaseObject dest, final DefaultBaseDO src)
  {
    dest.setId(src.getId());
    dest.setDeleted(src.isDeleted());
    dest.setCreated(src.getCreated());
    dest.setLastUpdate(src.getLastUpdate());
  }

  public static void copyFields(final DefaultBaseDO dest, final AbstractBaseObject src)
  {
    dest.setId(src.getId());
    dest.setDeleted(src.isDeleted());
    dest.setCreated(src.getCreated());
    dest.setLastUpdate(src.getLastUpdate());
  }
}
