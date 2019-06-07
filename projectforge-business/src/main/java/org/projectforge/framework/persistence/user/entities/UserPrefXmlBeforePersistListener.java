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

package org.projectforge.framework.persistence.user.entities;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.task.TaskDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlBeforePersistListener;
import de.micromata.genome.db.jpa.xmldump.api.XmlDumpRestoreContext;
import de.micromata.genome.jpa.metainf.EntityMetadata;

/**
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class UserPrefXmlBeforePersistListener implements JpaXmlBeforePersistListener
{
  private static final Logger LOG = LoggerFactory.getLogger(UserPrefXmlBeforePersistListener.class);

  @Override
  public Object preparePersist(EntityMetadata entityMetadata, Object entity, XmlDumpRestoreContext ctx)
  {
    final UserPrefDO userPref = (UserPrefDO) entity;
    final Set<UserPrefEntryDO> entries = userPref.getUserPrefEntries();
    if (entries == null || entries.size() == 0) {
      return null;
    }
    for (final UserPrefEntryDO entry : entries) {
      if ("task".equals(entry.getParameter()) == true) {
        updateEntryValue(ctx, entry, TaskDO.class);
      } else if ("user".equals(entry.getParameter()) == true || //
          "reporter".equals(entry.getParameter()) == true // Of ToDo's
          || "assignee".equals(entry.getParameter()) == true // Of ToDo's
          ) {
        updateEntryValue(ctx, entry, PFUserDO.class);
      } else if ("group".equals(entry.getParameter()) == true) {
        updateEntryValue(ctx, entry, GroupDO.class);
      } else if ("kost2".equals(entry.getParameter()) == true) {
        updateEntryValue(ctx, entry, Kost2DO.class);
      } else if ("kunde".equals(entry.getParameter()) == true) {
        updateEntryValue(ctx, entry, KundeDO.class);
      } else if ("projekt".equals(entry.getParameter()) == true) {
        updateEntryValue(ctx, entry, ProjektDO.class);
      }
    }
    return null;
  }

  private void updateEntryValue(final XmlDumpRestoreContext ctx, final UserPrefEntryDO entry,
      final Class<?> entityClass)
  {
    if (StringUtils.isEmpty(entry.getValue()) == true || "null".equals(entry.getValue()) == true) {
      return;
    }
    final Integer oldId = entry.getValueAsInteger();
    Object oldent = ctx.findEntityByOldPk(oldId, entityClass);
    if (oldent == null) {
      LOG.warn("Cannot find oldentity by pk: " + entityClass + "(" + oldId + ")");
      return;
    }

    EntityMetadata em = ctx.getEmgr().getEmgrFactory().getMetadataRepository().getEntityMetadata(entityClass);
    Object newId = em.getIdColumn().getGetter().get(oldent);
    if (newId != null) {
      entry.setValue(newId.toString());
    }
  }
}
