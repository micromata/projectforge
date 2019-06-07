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

package org.projectforge.framework.configuration.entities;

import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.configuration.ConfigurationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlBeforePersistListener;
import de.micromata.genome.db.jpa.xmldump.api.XmlDumpRestoreContext;
import de.micromata.genome.jpa.metainf.EntityMetadata;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class ConfigurationXmlBeforePersistListener implements JpaXmlBeforePersistListener
{
  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationXmlBeforePersistListener.class);

  @Override
  public Object preparePersist(EntityMetadata entityMetadata, Object entity, XmlDumpRestoreContext ctx)
  {
    final ConfigurationDO configurationDO = (ConfigurationDO) entity;
    if (configurationDO.getConfigurationType() != ConfigurationType.TASK) {
      return null;
    }
    final Integer oldTaskId = configurationDO.getTaskId();
    Integer newTaskId = ctx.findNewPkForOldPk(oldTaskId, TaskDO.class, Integer.class);
    if (newTaskId == null) {
      LOG.warn("Cannot find TaskDO with oldPk: " + oldTaskId);
      return null;
    }
    configurationDO.setTaskId(newTaskId);

    return null;
  }

}
