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

package org.projectforge.business.fibu.kost;

import org.projectforge.business.scripting.ScriptingDao;

public class Kost1ScriptingDao extends ScriptingDao<Kost1DO>
{
  private Kost1Dao __baseDao;

  public Kost1ScriptingDao(Kost1Dao kost1Dao)
  {
    super(kost1Dao);
    this.__baseDao = kost1Dao;
  }

  /**
   * @see Kost1Dao#getKost1(String)
   */
  public Kost1DO getKost1(String kostString)
  {
    return __baseDao.getKost1(kostString);
  }
}
