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

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.persistence.database.XmlDumpHook;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserPrefDO;
import org.projectforge.framework.persistence.user.entities.UserPrefEntryDO;
import org.projectforge.framework.persistence.xstream.XStreamSavingConverter;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @deprecated UserPrefXmlBeforePersistListener
 */
@Deprecated
public class UserPrefDOXmlDumpHook implements XmlDumpHook
{
  /**
   * @see org.projectforge.framework.persistence.database.XmlDumpHook#onBeforeRestore(org.projectforge.framework.persistence.xstream.XStreamSavingConverter,
   *      java.lang.Object)
   */
  @Override
  public void onBeforeRestore(UserXmlPreferencesDao userXmlPreferencesDao,
      final XStreamSavingConverter xstreamSavingConverter, final Object obj)
  {
    if (obj instanceof UserPrefDO) {
      final UserPrefDO userPref = (UserPrefDO) obj;
      final Set<UserPrefEntryDO> entries = userPref.getUserPrefEntries();
      if (entries == null || entries.size() == 0) {
        return;
      }
      for (final UserPrefEntryDO entry : entries) {
        if ("task".equals(entry.getParameter()) == true) {
          updateEntryValue(xstreamSavingConverter, entry, TaskDO.class);
        } else if ("user".equals(entry.getParameter()) == true || //
            "reporter".equals(entry.getParameter()) == true // Of ToDo's
            || "assignee".equals(entry.getParameter()) == true // Of ToDo's
        ) {
          updateEntryValue(xstreamSavingConverter, entry, PFUserDO.class);
        } else if ("group".equals(entry.getParameter()) == true) {
          updateEntryValue(xstreamSavingConverter, entry, GroupDO.class);
        } else if ("kost2".equals(entry.getParameter()) == true) {
          updateEntryValue(xstreamSavingConverter, entry, Kost2DO.class);
        } else if ("kunde".equals(entry.getParameter()) == true) {
          updateEntryValue(xstreamSavingConverter, entry, KundeDO.class);
        } else if ("projekt".equals(entry.getParameter()) == true) {
          updateEntryValue(xstreamSavingConverter, entry, ProjektDO.class);
        }
      }
      return;
    }
  }

  private void updateEntryValue(final XStreamSavingConverter xstreamSavingConverter, final UserPrefEntryDO entry,
      final Class<?> entityClass)
  {
    if (StringUtils.isEmpty(entry.getValue()) == true || "null".equals(entry.getValue()) == true) {
      return;
    }
    final Integer oldId = entry.getValueAsInteger();
    final Integer newId = xstreamSavingConverter.getNewIdAsInteger(entityClass, oldId);
    if (newId != null) {
      entry.setValue(newId.toString());
    }
  }
}
