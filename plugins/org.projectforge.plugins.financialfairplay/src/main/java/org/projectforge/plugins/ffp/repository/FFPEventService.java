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

package org.projectforge.plugins.ffp.repository;

import java.util.List;

import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.model.FFPDebtDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;

/**
 * Access to ffp event.
 *
 * @author Florian Blumenstein
 */
public interface FFPEventService extends IPersistenceService<FFPEventDO>, IDao<FFPEventDO>
{
  FFPEventDao getEventDao();

  FFPDebtDao getDebtDao();

  List<FFPDebtDO> calculateDebt(FFPEventDO event);

  List<FFPDebtDO> getDeptList(PFUserDO user);

  void createDept(FFPEventDO event);

  void updateDebtFrom(FFPDebtDO debt);

  void updateDebtTo(FFPDebtDO debt);

  Integer getOpenDebts(PFUserDO user);

  boolean debtExists(FFPEventDO event);
}
