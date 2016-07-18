package org.projectforge.business.user;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.springframework.beans.factory.annotation.Autowired;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlBeforePersistListener;
import de.micromata.genome.db.jpa.xmldump.api.XmlDumpRestoreContext;
import de.micromata.genome.jpa.metainf.EntityMetadata;

/**
 * Converts the pk references.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class UserXmlPreferenceXmlBeforePersistListener implements JpaXmlBeforePersistListener
{
  private static final Logger LOG = Logger.getLogger(UserXmlPreferenceXmlBeforePersistListener.class);

  @Autowired
  private UserXmlPreferencesDao userXmlPreferencesDao;

  @Override
  public Object preparePersist(EntityMetadata entityMetadata, Object entity, XmlDumpRestoreContext ctx)
  {
    final UserXmlPreferencesDO userPrefs = (UserXmlPreferencesDO) entity;
    if (TaskTree.USER_PREFS_KEY_OPEN_TASKS.equals(userPrefs.getKey()) == false) {
      return null;
    }
    final Object userPrefsObj = userXmlPreferencesDao.deserialize(null, userPrefs, true);
    if (userPrefsObj == null || userPrefsObj instanceof Set == false) {
      return null;
    }
    Set<Integer> oldIds = null;
    try {
      oldIds = (Set<Integer>) userPrefsObj;
    } catch (final ClassCastException ex) {
      LOG.error("Oups, Set of task id's is not of type Set<Integer>, can't migrate this list.");
    }
    if (oldIds.size() == 0) {
      return null;
    }
    final Set<Integer> newIds = new HashSet<Integer>();
    EntityMetadata taskem = ctx.getEmgr().getEmgrFactory().getMetadataRepository().getEntityMetadata(TaskDO.class);
    for (final Integer oldId : oldIds) {
      Object task = ctx.findEntityByOldPk(oldId, taskem.getJavaType());
      if (task == null) {
        LOG.warn("Cannot find Task with old pk " + oldId);
        continue;
      }
      Integer newId = (Integer) taskem.getIdColumn().getGetter().get(task);
      newIds.add(newId);
    }
    userXmlPreferencesDao.serialize(userPrefs, newIds);
    return null;
  }

}
