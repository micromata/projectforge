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

package org.projectforge.business.orga;

import java.util.ArrayList;
import java.util.List;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.impl.CorePersistenceServiceImpl;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VisitorbookServiceImpl extends CorePersistenceServiceImpl<Integer, VisitorbookDO>
    implements VisitorbookService
{
  @Autowired
  private VisitorbookDao visitorbookDao;

  @Override
  public List<Integer> getAssignedContactPersonsIds(VisitorbookDO data)
  {
    List<Integer> assignedContactPersons = new ArrayList<>();
    if (data != null && data.getContactPersons() != null) {
      for (EmployeeDO employee : data.getContactPersons()) {
        assignedContactPersons.add(employee.getPk());
      }
    }
    return assignedContactPersons;
  }

  @Override
  public VisitorbookTimedDO addNewTimeAttributeRow(final VisitorbookDO visitor, final String groupName)
  {
    final VisitorbookTimedDO nw = new VisitorbookTimedDO();
    nw.setVisitor(visitor);
    nw.setGroupName(groupName);
    visitor.getTimeableAttributes().add(nw);
    return nw;
  }

  @Override
  public List<VisitorbookDO> getList(BaseSearchFilter filter)
  {
    return visitorbookDao.getList(filter);
  }

  @Override
  public boolean hasLoggedInUserInsertAccess()
  {
    return visitorbookDao.hasLoggedInUserInsertAccess();
  }

  @Override
  public boolean hasLoggedInUserInsertAccess(VisitorbookDO obj, boolean throwException)
  {
    return visitorbookDao.hasLoggedInUserInsertAccess(obj, throwException);
  }

  @Override
  public boolean hasLoggedInUserUpdateAccess(VisitorbookDO obj, VisitorbookDO dbObj, boolean throwException)
  {
    return visitorbookDao.hasLoggedInUserUpdateAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasLoggedInUserDeleteAccess(VisitorbookDO obj, VisitorbookDO dbObj, boolean throwException)
  {
    return visitorbookDao.hasLoggedInUserDeleteAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasDeleteAccess(PFUserDO user, VisitorbookDO obj, VisitorbookDO dbObj, boolean throwException)
  {
    return visitorbookDao.hasDeleteAccess(user, obj, dbObj, throwException);
  }

  @Override
  public List<String> getAutocompletion(String property, String searchString)
  {
    return visitorbookDao.getAutocompletion(property, searchString);
  }

  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(VisitorbookDO obj)
  {
    return visitorbookDao.getDisplayHistoryEntries(obj);
  }

  @Override
  public void rebuildDatabaseIndex4NewestEntries()
  {
    visitorbookDao.rebuildDatabaseIndex4NewestEntries();
  }

  @Override
  public void rebuildDatabaseIndex()
  {
    visitorbookDao.rebuildDatabaseIndex();
  }

}
