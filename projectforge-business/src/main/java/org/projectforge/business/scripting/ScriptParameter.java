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

package org.projectforge.business.scripting;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.TimePeriod;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;


public class ScriptParameter implements Serializable
{
  private static final long serialVersionUID = 2270347937331330184L;

  @XStreamAsAttribute
  protected ScriptParameterType type;

  @XStreamAsAttribute
  protected String parameterName;

  protected String stringValue;

  /**
   * intValue is of type long.
   */
  @XStreamAsAttribute
  protected Long intValue;

  @XStreamAsAttribute
  protected BigDecimal decimalValue;

  @XStreamAsAttribute
  protected Boolean booleanValue;

  @XStreamAsAttribute
  protected LocalDate dateValue;

  protected transient TaskDO task;

  protected transient PFUserDO user;

  protected TimePeriod timePeriodValue;

  public ScriptParameter()
  {
  }

  public ScriptParameter(final String parameterName, final ScriptParameterType type)
  {
    this.parameterName = parameterName;
    this.type = type;
  }

  public Object getValue()
  {
    if (type == null) {
      return null;
    } else if (type == ScriptParameterType.STRING) {
      return stringValue;
    } else if (type == ScriptParameterType.DECIMAL) {
      return decimalValue;
    } else if (type == ScriptParameterType.DATE) {
      return dateValue;
    } else if (type == ScriptParameterType.TIME_PERIOD) {
      return timePeriodValue;
    } else if (type == ScriptParameterType.INTEGER) {
      return intValue;
    } else if (type == ScriptParameterType.BOOLEAN) {
      return booleanValue;
    } else if (type == ScriptParameterType.TASK) {
      return task;
    } else if (type == ScriptParameterType.USER) {
      return user;
    }
    throw new UnsupportedOperationException("Parameter type '" + type + "' not supported.");
  }

  public void setValue(final Object value)
  {
    if (type == ScriptParameterType.STRING) {
      stringValue = (String) value;
    } else if (type == ScriptParameterType.DECIMAL) {
      decimalValue = (BigDecimal) value;
    } else if (type == ScriptParameterType.DATE) {
      dateValue = (LocalDate) value;
    } else if (type == ScriptParameterType.TIME_PERIOD) {
      timePeriodValue = (TimePeriod) value;
    } else if (type == ScriptParameterType.INTEGER) {
      intValue = (Long) value;
    } else if (type == ScriptParameterType.BOOLEAN) {
      booleanValue = (Boolean) value;
    } else if (type == ScriptParameterType.TASK) {
      setTask((TaskDO) value);
    } else if (type == ScriptParameterType.USER) {
      setUser((PFUserDO) value);
    } else {
      throw new UnsupportedOperationException("Parameter type '" + type + "' not supported.");
    }
  }

  public Class<?> getValueClass() {
     if (type == ScriptParameterType.DECIMAL) {
      return BigDecimal.class;
    } else if (type == ScriptParameterType.DATE) {
      return LocalDate.class;
    } else if (type == ScriptParameterType.TIME_PERIOD) {
      return TimePeriod.class;
    } else if (type == ScriptParameterType.INTEGER) {
      return Long.class;
     } else if (type == ScriptParameterType.BOOLEAN) {
       return Boolean.class;
    } else if (type == ScriptParameterType.TASK) {
      return TaskDO.class;
    } else if (type == ScriptParameterType.USER) {
      return PFUserDO.class;
    } else {
       return String.class;
    }
  }

  public ScriptParameterType getType()
  {
    return type;
  }

  public String getParameterName()
  {
    return parameterName;
  }

  public String getStringValue()
  {
    return stringValue;
  }

  public void setStringValue(String stringValue)
  {
    if (type != ScriptParameterType.STRING) {
      throw new IllegalArgumentException("Cannot set value for non string parameter: " + type);
    }
    this.stringValue = stringValue;
  }

  public Long getIntValue()
  {
    return intValue;
  }

  public void setIntValue(Long intValue)
  {
    if (type != ScriptParameterType.INTEGER) {
      throw new IllegalArgumentException("Cannot set value for non integer parameter: " + type);
    }
    this.intValue = intValue;
  }

  public BigDecimal getDecimalValue()
  {
    return decimalValue;
  }

  public void setDecimalValue(BigDecimal decimalValue)
  {
    if (type != ScriptParameterType.DECIMAL) {
      throw new IllegalArgumentException("Cannot set value for non decimal parameter: " + type);
    }
    this.decimalValue = decimalValue;
  }

  public Boolean getBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(Boolean booleanValue) {
    if (type != ScriptParameterType.BOOLEAN) {
      throw new IllegalArgumentException("Cannot set value for non boolean parameter: " + type);
    }
    this.booleanValue = booleanValue;
  }

  public LocalDate getDateValue()
  {
    return dateValue;
  }

  public void setDateValue(LocalDate dateValue)
  {
    if (type != ScriptParameterType.DATE) {
      throw new IllegalArgumentException("Cannot set date for non date parameter: " + type);
    }
    this.dateValue = dateValue;
  }

  public TimePeriod getTimePeriodValue()
  {
    return timePeriodValue;
  }

  public void setTimePeriodValue(TimePeriod timePeriodValue)
  {
    if (type != ScriptParameterType.TIME_PERIOD) {
      throw new IllegalArgumentException("Cannot set value for non time period parameter: " + type);
    }
    this.timePeriodValue = timePeriodValue;
  }

  public void setType(ScriptParameterType type)
  {
    this.type = type;
  }

  public void setParameterName(String parameterName)
  {
    this.parameterName = parameterName;
  }

  public TaskDO getTask()
  {
    return task;
  }

  public void setTask(TaskDO task)
  {
    if (type != ScriptParameterType.TASK) {
      throw new IllegalArgumentException("Cannot set date for non task parameter: " + type);
    }
    this.task = task;
    if (task != null) {
      intValue = task.getId();
    } else {
      intValue = null;
    }
  }

  public PFUserDO getUser()
  {
    return user;
  }

  public void setUser(PFUserDO user)
  {
    if (type != ScriptParameterType.USER) {
      throw new IllegalArgumentException("Cannot set date for non user parameter: " + type);
    }
    this.user = user;
    if (user != null) {
      intValue = user.getId();
    } else {
      intValue = null;
    }
  }

  public String getAsString()
  {
    final StringBuilder buf = new StringBuilder();
    buf.append("[").append(parameterName).append(',').append(type).append(',');
    if (type != null && type.isIn(ScriptParameterType.TASK, ScriptParameterType.USER)) {
      buf.append(intValue);
    } else {
      buf.append(getValue());
    }
    buf.append(']');
    return buf.toString();
  }

  @Override
  public String toString()
  {
    final ToStringBuilder tos = new ToStringBuilder(this);
    tos.append(parameterName);
    tos.append(type);
    if (type.isIn(ScriptParameterType.TASK, ScriptParameterType.USER)) {
      tos.append(intValue);
    } else {
      tos.append(getValue());
    }
    return tos.toString();
  }
}
