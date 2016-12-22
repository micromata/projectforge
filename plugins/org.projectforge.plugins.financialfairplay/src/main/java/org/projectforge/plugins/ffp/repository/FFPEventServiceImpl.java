package org.projectforge.plugins.ffp.repository;

import java.util.List;

import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.impl.CorePersistenceServiceImpl;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Standard implementation of the ffp event service interface.
 *
 * @author Florian Blumenstein
 */
@Service
public class FFPEventServiceImpl extends CorePersistenceServiceImpl<Integer, FFPEventDO>
    implements FFPEventService
{
  @Autowired
  private FFPEventDao eventDao;

  @Override
  public boolean hasLoggedInUserInsertAccess()
  {
    return eventDao.hasLoggedInUserInsertAccess();
  }

  @Override
  public boolean hasLoggedInUserInsertAccess(FFPEventDO obj, boolean throwException)
  {
    return eventDao.hasLoggedInUserInsertAccess(obj, throwException);
  }

  @Override
  public boolean hasLoggedInUserUpdateAccess(FFPEventDO obj, FFPEventDO dbObj, boolean throwException)
  {
    return eventDao.hasLoggedInUserUpdateAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasLoggedInUserDeleteAccess(FFPEventDO obj, FFPEventDO dbObj, boolean throwException)
  {
    return eventDao.hasLoggedInUserDeleteAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasDeleteAccess(PFUserDO user, FFPEventDO obj, FFPEventDO dbObj, boolean throwException)
  {
    return eventDao.hasDeleteAccess(user, obj, dbObj, throwException);
  }

  @Override
  public List<String> getAutocompletion(String property, String searchString)
  {
    return eventDao.getAutocompletion(property, searchString);
  }

  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(FFPEventDO obj)
  {
    return eventDao.getDisplayHistoryEntries(obj);
  }

  @Override
  public void rebuildDatabaseIndex4NewestEntries()
  {
    eventDao.rebuildDatabaseIndex4NewestEntries();
  }

  @Override
  public void rebuildDatabaseIndex()
  {
    eventDao.rebuildDatabaseIndex();
  }

  @Override
  public FFPEventDao getDao()
  {
    return eventDao;
  }

}
