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

package org.projectforge.business.user;

import java.util.HashSet;
import java.util.Set;

import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.framework.persistence.database.XmlDumpHook;
import org.projectforge.framework.persistence.xstream.XStreamSavingConverter;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @deprecated see UserXmlPreferenceXmlBeforePersistListener
 */
@Deprecated
public class UserXmlPreferencesXmlDumpHook implements XmlDumpHook
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(UserXmlPreferencesXmlDumpHook.class);

  /**
   * @see org.projectforge.framework.persistence.database.XmlDumpHook#onBeforeRestore(org.projectforge.framework.persistence.xstream.XStreamSavingConverter,
   *      java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void onBeforeRestore(UserXmlPreferencesDao userXmlPreferencesDao,
      final XStreamSavingConverter xstreamSavingConverter, final Object obj)
  {
    if (obj instanceof UserXmlPreferencesDO) {
      final UserXmlPreferencesDO userPrefs = (UserXmlPreferencesDO) obj;
      if (TaskTree.USER_PREFS_KEY_OPEN_TASKS.equals(userPrefs.getKey()) == false) {
        return;
      }
      final Object userPrefsObj = userXmlPreferencesDao.deserialize(null, userPrefs, true);
      if (userPrefsObj == null || userPrefsObj instanceof Set == false) {
        return;
      }
      Set<Integer> oldIds = null;
      try {
        oldIds = (Set<Integer>) userPrefsObj;
      } catch (final ClassCastException ex) {
        log.error("Oups, Set of task id's is not of type Set<Integer>, can't migrate this list.");
      }
      if (oldIds.size() == 0) {
        return;
      }
      final Set<Integer> newIds = new HashSet<Integer>();
      for (final Integer oldId : oldIds) {
        final Integer newId = xstreamSavingConverter.getNewIdAsInteger(TaskDO.class, oldId);
        newIds.add(newId);
      }
      userXmlPreferencesDao.serialize(userPrefs, newIds);
      return;
    }
  }
}
