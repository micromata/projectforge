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

package org.projectforge.framework.configuration.entities;

import java.math.BigDecimal;
import java.util.Date;
import java.util.TimeZone;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationType;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist;

/**
 * For configuration entries persisted in the data base. Please access the configuration parameters via
 * {@link Configuration}
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_CONFIGURATION", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "parameter", "tenant_id" }) }, indexes = {
    @javax.persistence.Index(name = "idx_fk_t_configuration_tenant_id", columnList = "tenant_id")
})
@JpaXmlPersist(beforePersistListener = ConfigurationXmlBeforePersistListener.class)
@AUserRightId("ADMIN_CORE")
public class ConfigurationDO extends DefaultBaseDO
{
  public static final int PARAM_LENGTH = 4000;

  private static final long serialVersionUID = -1369978022611555731L;

  private boolean global;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String parameter;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String stringValue;

  private Integer intValue;

  private BigDecimal floatValue;

  private ConfigurationType configurationType;

  /**
   * Default constructor
   */
  public ConfigurationDO()
  {

  }

  /**
   * Is used for versions without tenant column
   *
   * @param id
   * @param created
   * @param deleted
   * @param lastUpdate
   * @param configurationType
   * @param floatValue
   * @param intValue
   * @param parameter
   * @param stringValue
   */
  public ConfigurationDO(Integer id, Date created, boolean deleted, Date lastUpdate,
      ConfigurationType configurationType, BigDecimal floatValue, Integer intValue, String parameter,
      String stringValue)
  {
    this.setId(id);
    this.setCreated(created);
    this.setDeleted(deleted);
    this.setLastUpdate(lastUpdate);
    this.setConfigurationType(configurationType);
    this.setFloatValue(floatValue);
    this.setIntValue(intValue);
    this.setParameter(parameter);
    this.setStringValue(stringValue);
  }

  /**
   * Key under which the configuration value is stored in the database.
   */
  @Column(length = 255, nullable = false)
  public String getParameter()
  {
    return parameter;
  }

  public void setParameter(final String name)
  {
    this.parameter = name;
  }

  /**
   * @return The full i18n key including the i18n prefix "administration.configuration.param.".
   */
  @Transient
  public String getI18nKey()
  {
    return "administration.configuration.param." + this.parameter;
  }

  /**
   * @return The full i18n key including the i18n prefix "administration.configuration.param." and the suffix
   * ".description".
   */
  @Transient
  public String getDescriptionI18nKey()
  {
    return "administration.configuration.param." + this.parameter + ".description";
  }

  /**
   * @return The string value. If entry is not from type STRING then a RuntimeException will be thrown.
   */
  @Column(length = PARAM_LENGTH)
  public String getStringValue()
  {
    if (stringValue != null) {
      checkType(ConfigurationType.STRING);
    }
    return stringValue;
  }

  public void setStringValue(final String stringValue)
  {
    if (stringValue != null) {
      checkType(ConfigurationType.STRING);
    }
    this.stringValue = stringValue;
  }

  @Transient
  public String getTimeZoneId()
  {
    if (stringValue != null) {
      checkType(ConfigurationType.STRING);
    }
    return stringValue;
  }

  public void setTimeZoneId(final String id)
  {
    if (stringValue != null) {
      checkType(ConfigurationType.STRING);
    }
    if (id != null) {
      final TimeZone timeZone = TimeZone.getTimeZone(id);
      if (timeZone == null) {
        throw new UnsupportedOperationException("Unsupported time zone: " + id);
      }
    }
    this.stringValue = id;
  }

  @Transient
  public TimeZone getTimeZone()
  {
    if (stringValue != null) {
      checkType(ConfigurationType.STRING);
    } else {
      return null;
    }
    final TimeZone timeZone = TimeZone.getTimeZone(stringValue);
    return timeZone;
  }

  public void setTimeZone(final TimeZone timeZone)
  {
    if (timeZone == null) {
      this.stringValue = null;
    } else {
      this.stringValue = timeZone.getID();
    }
  }

  @Column
  public Integer getIntValue()
  {
    if (intValue != null) {
      checkType(ConfigurationType.INTEGER);
    }
    return intValue;
  }

  public void setIntValue(final Integer intValue)
  {
    if (intValue != null) {
      checkType(ConfigurationType.INTEGER);
    }
    this.intValue = intValue;
  }

  @Transient
  public Integer getTaskId()
  {
    if (intValue != null) {
      checkType(ConfigurationType.TASK);
    }
    return getIntValue();
  }

  public void setTaskId(final Integer taskId)
  {
    if (taskId != null) {
      checkType(ConfigurationType.TASK);
    }
    setIntValue(taskId);
  }

  @Transient
  public Integer getCalendarId()
  {
    if (intValue != null) {
      checkType(ConfigurationType.CALENDAR);
    }
    return getIntValue();
  }

  public void setCalendarId(final Integer calendarId)
  {
    if (calendarId != null) {
      checkType(ConfigurationType.CALENDAR);
    }
    setIntValue(calendarId);
  }

  @Column(scale = 5, precision = 18)
  public BigDecimal getFloatValue()
  {
    if (floatValue != null) {
      checkType(ConfigurationType.FLOAT);
    }
    return floatValue;
  }

  public void setFloatValue(final BigDecimal floatValue)
  {
    if (floatValue != null) {
      checkType(ConfigurationType.FLOAT);
    }
    this.floatValue = floatValue;
  }

  @Transient
  public Boolean getBooleanValue()
  {
    if (this.configurationType == ConfigurationType.BOOLEAN) {
      return Boolean.TRUE.toString().equals(stringValue);
    } else {
      return null;
    }
  }

  public void setBooleanValue(final Boolean booleanValue)
  {
    this.stringValue = booleanValue != null ? booleanValue.toString() : Boolean.FALSE.toString();
  }

  @Transient
  public Object getValue()
  {
    if (this.configurationType.isIn(ConfigurationType.STRING, ConfigurationType.TEXT,
        ConfigurationType.TIME_ZONE) == true) {
      return this.stringValue;
    } else if (this.configurationType == ConfigurationType.INTEGER
        || this.configurationType == ConfigurationType.TASK
        || this.configurationType == ConfigurationType.CALENDAR) {
      return this.intValue;
    } else if (this.configurationType == ConfigurationType.FLOAT
        || this.configurationType == ConfigurationType.PERCENT) {
      return this.floatValue;
    } else if (this.configurationType == ConfigurationType.BOOLEAN) {
      return this.getBooleanValue();
    } else {
      throw new UnsupportedOperationException("Unsupported value type: " + this.configurationType);
    }
  }

  public void setValue(final Object value)
  {
    if (value == null) {
      stringValue = null;
      intValue = null;
      floatValue = null;
      return;
    }
    if (value instanceof String) {
      setStringValue((String) value);
    } else {
      throw new UnsupportedOperationException("Unsupported value type: " + value.getClass().getName());
    }
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  public ConfigurationType getConfigurationType()
  {
    return configurationType;
  }

  public void setConfigurationType(final ConfigurationType configurationType)
  {
    setType(configurationType);
    // this.configurationType = configurationType;
  }

  public void internalSetConfigurationType(final ConfigurationType type)
  {
    this.configurationType = type;
    if (this.configurationType.isIn(ConfigurationType.STRING, ConfigurationType.BOOLEAN, ConfigurationType.TEXT,
        ConfigurationType.TIME_ZONE) == true) {
      this.intValue = null;
      this.floatValue = null;
    } else if (this.configurationType.isIn(ConfigurationType.INTEGER, ConfigurationType.TASK) == true) {
      this.stringValue = null;
      this.floatValue = null;
    } else if (this.configurationType.isIn(ConfigurationType.INTEGER, ConfigurationType.CALENDAR) == true) {
      this.stringValue = null;
      this.floatValue = null;
    } else if (this.configurationType.isIn(ConfigurationType.FLOAT, ConfigurationType.PERCENT) == true) {
      this.stringValue = null;
      this.intValue = null;
    } else {
      throw new UnsupportedOperationException("Unkown type: " + type);
    }
  }

  protected void checkType(final ConfigurationType type)
  {
    if (this.configurationType != null) {
      if (this.configurationType == type) {
        return;
      } else if (type == ConfigurationType.STRING
          && this.configurationType.isIn(ConfigurationType.TEXT, ConfigurationType.BOOLEAN,
          ConfigurationType.TIME_ZONE) == true) {
        return;
      } else if (type == ConfigurationType.INTEGER && this.configurationType == ConfigurationType.TASK) {
        return;
      } else if (type == ConfigurationType.INTEGER && this.configurationType == ConfigurationType.CALENDAR) {
        return;
      } else if (type == ConfigurationType.FLOAT && this.configurationType == ConfigurationType.PERCENT) {
        return;
      }
    }
    throw new UnsupportedOperationException("Configuration object of type '"
        + this.configurationType
        + "' does not support value of type '"
        + type
        + "'!");
  }

  protected ConfigurationDO setType(final ConfigurationType type)
  {
    if (this.configurationType == null) {
      this.configurationType = type;
    } else if (this.configurationType == type) {
      // Do nothing.
    } else if (type == ConfigurationType.STRING
        && this.configurationType.isIn(ConfigurationType.TEXT, ConfigurationType.BOOLEAN,
        ConfigurationType.TIME_ZONE) == true) {
      // Do nothing.
    } else if (type == ConfigurationType.INTEGER && this.configurationType == ConfigurationType.TASK) {
      // Do nothing.
    } else if (type == ConfigurationType.INTEGER && this.configurationType == ConfigurationType.CALENDAR) {
      // Do nothing.
    } else if (type == ConfigurationType.FLOAT && this.configurationType == ConfigurationType.PERCENT) {
      // Do nothing.
    } else {
      throw new UnsupportedOperationException("Configuration object of type '"
          + this.configurationType
          + "' cannot be changed to type '"
          + type
          + "'!");
    }
    return this;
  }

  /**
   * If true this parameter is valid for all tenants (in multi-tenancy environments), otherwise this parameter is valid
   * only for the tenant this parameter is assigned to.
   *
   * @return the global
   */
  @Column(name = "is_global", columnDefinition = "boolean default false")
  public Boolean getGlobal()
  {
    return global;
  }

  /**
   * @param global the global to set
   * @return this for chaining.
   */
  public ConfigurationDO setGlobal(final Boolean global)
  {
    if (global == null) {
      this.global = false;
    } else {
      this.global = global;
    }
    return this;
  }
}
