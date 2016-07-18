package org.projectforge.business.task;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlBeforePersistListener;
import de.micromata.genome.db.jpa.xmldump.api.XmlDumpRestoreContext;
import de.micromata.genome.jpa.metainf.EntityMetadata;

/**
 * The listener interface for receiving taskXmlBeforePersist events. The class that is interested in processing a
 * taskXmlBeforePersist event implements this interface, and the object created with that class is registered with a
 * component using the component's <code>addTaskXmlBeforePersistListener<code> method. When the taskXmlBeforePersist
 * event occurs, that object's appropriate method is invoked.
 *
 * @see TaskXmlBeforePersistEvent
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class TaskXmlBeforePersistListener implements JpaXmlBeforePersistListener
{

  /**
   * {@inheritDoc}
   *
   */
  @Override
  public Object preparePersist(EntityMetadata entityMetadata, Object entity, XmlDumpRestoreContext ctx)
  {
    TaskDO task = (TaskDO) entity;
    if (task.getParentTask() != null) {
      TaskDO ptask = (TaskDO) ctx.getPersistService().persist(ctx, entityMetadata, task.getParentTask());
      task.setParentTask(ptask);
    }
    return null;
  }

}
