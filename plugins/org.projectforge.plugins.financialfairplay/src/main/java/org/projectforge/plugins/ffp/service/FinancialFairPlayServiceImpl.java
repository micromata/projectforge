package org.projectforge.plugins.ffp.service;

import java.util.List;

import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.impl.CorePersistenceServiceImpl;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.springframework.stereotype.Service;

@Service
public class FinancialFairPlayServiceImpl extends CorePersistenceServiceImpl<Integer, FFPEventDO>
    implements FinancialFairPlayService
{

  @Override
  public boolean hasLoggedInUserInsertAccess()
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean hasLoggedInUserInsertAccess(FFPEventDO obj, boolean throwException)
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean hasLoggedInUserUpdateAccess(FFPEventDO obj, FFPEventDO dbObj, boolean throwException)
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean hasLoggedInUserDeleteAccess(FFPEventDO obj, FFPEventDO dbObj, boolean throwException)
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean hasDeleteAccess(PFUserDO user, FFPEventDO obj, FFPEventDO dbObj, boolean throwException)
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<String> getAutocompletion(String property, String searchString)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(FFPEventDO obj)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void rebuildDatabaseIndex4NewestEntries()
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void rebuildDatabaseIndex()
  {
    // TODO Auto-generated method stub

  }

}
