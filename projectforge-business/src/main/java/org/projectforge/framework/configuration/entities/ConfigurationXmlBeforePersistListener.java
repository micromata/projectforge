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
