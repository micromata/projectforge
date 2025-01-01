/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.access;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.common.i18n.MessageParam;
import org.projectforge.common.i18n.MessageParamType;
import org.projectforge.common.i18n.UserException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.util.ResourceBundle;

/**
 * This class will be thrown by AccessChecker, if no access is given for the demanded action by an user.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class AccessException extends UserException {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccessException.class);

  private static final long serialVersionUID = 147795804616526958L;

  public final static String I18N_KEY_STANDARD = "access.exception.standard";

  public final static String I18N_KEY_STANDARD_WITH_TASK = "access.exception.standardWithTask";

  private TaskTree taskTree;

  protected PFUserDO user = null;

  protected Long taskId = null;

  protected AccessType accessType = null;

  protected String message = null;

  protected OperationType operationType = null;

  protected Class<?> clazz = null;

  public AccessException(final String i18nKey, final Object... params) {
    this(ThreadLocalUserContext.getLoggedInUser(), i18nKey, params);
  }

  public AccessException(final PFUserDO user, final String i18nKey, final Object... params) {
    super(i18nKey, params);
    this.user = user;
    log.info("AccessException: " + this);
  }

  public AccessException(final AccessType accessType, final OperationType operationType) {
    this(ThreadLocalUserContext.getLoggedInUser(), accessType, operationType);
  }

  public AccessException(final PFUserDO user, final AccessType accessType,
                         final OperationType operationType) {
    super(I18N_KEY_STANDARD, new MessageParam[]{new MessageParam(accessType), new MessageParam(operationType)});
    this.user = user;
    this.accessType = accessType;
    this.operationType = operationType;
    log.info("AccessException: " + this);
  }

  public AccessException(final Long taskId, final AccessType accessType,
                         final OperationType operationType) {
    this(ThreadLocalUserContext.getLoggedInUser(), taskId, accessType, operationType);
  }

  public AccessException(final PFUserDO user, final Long taskId, final AccessType accessType,
                         final OperationType operationType) {
    super(I18N_KEY_STANDARD_WITH_TASK,
        new MessageParam[]{new MessageParam(taskId), new MessageParam(accessType),
            new MessageParam(operationType)});
    this.user = user;
    this.taskId = taskId;
    this.accessType = accessType;
    this.operationType = operationType;
    log.info("AccessException: " + this);
  }

  /**
   * The order of the args is task id, accessType and operationType.
   *
   * @return the arguments for the message formatter from type Object[3].
   */
  public Object[] getMessageArgs(final ResourceBundle bundle) {
    final Object[] result = new Object[3];
    if (taskTree != null && this.taskId != null) {
      final TaskDO task = TaskTree.getInstance().getTaskById(taskId);
      if (task != null) {
        result[0] = task.getTitle();
      } else {
        result[0] = taskId;
      }
    } else {
      result[0] = taskId;
    }
    if (accessType != null) {
      result[1] = bundle.getString(accessType.getI18nKey());
    }
    if (operationType != null) {
      result[2] = bundle.getString(operationType.getI18nKey());
    }
    return result;
  }

  public MessageParam[] getMessageArgs() {
    final MessageParam[] result = new MessageParam[3];
    if (taskTree != null && this.taskId != null) {
      final TaskDO task = TaskTree.getInstance().getTaskById(taskId);
      if (task != null) {
        result[0] = new MessageParam(task.getTitle());
      } else {
        result[0] = new MessageParam(taskId);
      }
    } else {
      result[0] = new MessageParam(taskId);
    }
    if (accessType != null) {
      result[1] = new MessageParam(accessType.getI18nKey(), MessageParamType.I18N_KEY);
    }
    if (operationType != null) {
      result[2] = new MessageParam(operationType.getI18nKey(), MessageParamType.I18N_KEY);
    }
    return result;
  }

  public PFUserDO getUser() {
    return this.user;
  }

  public Long getTaskId() {
    return this.taskId;
  }

  public AccessType getAccessType() {
    return accessType;
  }

  public void setAccessType(final AccessType accessType) {
    this.accessType = accessType;
  }

  public TaskNode getTaskNode() {
    return TaskTree.getInstance().getTaskNodeById(this.taskId);
  }

  /**
   * Class infos about class, for which the AccessException was thrown, e. g. for logging.
   *
   * @return Returns the clazz.
   */
  public Class<?> getClazz() {
    return clazz;
  }

  /**
   * @param clazz The clazz to set.
   */
  public void setClazz(final Class<?> clazz) {
    this.clazz = clazz;
  }

  /**
   * @return Returns the operationType.
   */
  public OperationType getOperationType() {
    return operationType;
  }

  /**
   * @param operationType The operationType to set.
   */
  public void setOperationType(final OperationType operationType) {
    this.operationType = operationType;
  }

  /**
   * @param message The message to set.
   */
  public void setMessage(final String message) {
    this.message = message;
  }

  /**
   * @param user The user to set.
   */
  public void setUser(final PFUserDO user) {
    this.user = user;
  }

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    if (user != null) {
      builder.append("user", String.valueOf(user.getId()) + ":" + user.getUsername());
    }
    if (taskId != null) {
      final TaskDO task = taskTree != null ? TaskTree.getInstance().getTaskById(taskId) : null;
      final String ts = task != null ? ":" + task.getDisplayName() : "";
      builder.append("task", String.valueOf(taskId) + ts);
    }
    if (accessType != null) {
      builder.append("accessType", accessType.toString());
    }
    if (operationType != null) {
      builder.append("operationType", operationType.toString());
    }
    if (clazz != null) {
      builder.append("class", clazz.toString());
    }
    builder.append("i18nKey", getI18nKey());
    if (message != null) {
      builder.append("message", message);
    }
    if (getParams() != null) {
      builder.append("params", getParams());
    }
    return builder.toString();
  }
}
