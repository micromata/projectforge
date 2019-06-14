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

package org.projectforge.business.user;

import java.util.HashSet;
import java.util.Set;

import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger LOG = LoggerFactory.getLogger(UserXmlPreferenceXmlBeforePersistListener.class);

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
