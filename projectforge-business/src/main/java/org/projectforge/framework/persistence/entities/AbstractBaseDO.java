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

package org.projectforge.framework.persistence.entities;

import de.micromata.genome.db.jpa.history.api.NoHistory;
import org.apache.commons.lang3.ClassUtils;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.ToStringUtil;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.jpa.impl.BaseDaoJpaAdapter;
import org.projectforge.framework.persistence.user.entities.TenantDO;

import javax.persistence.*;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
@MappedSuperclass
public abstract class AbstractBaseDO<I extends Serializable> implements ExtendedBaseDO<I>, Serializable
{
  private static final long serialVersionUID = -2225460450662176301L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractBaseDO.class);

  private TenantDO tenant;

  @NoHistory
  @PropertyInfo(i18nKey = "created")
  private Date created;

  @NoHistory
  @PropertyInfo(i18nKey = "modified")
  private Date lastUpdate;

  @PropertyInfo(i18nKey = "deleted")
  private boolean deleted;

  private transient boolean minorChange = false;

  private transient Map<String, Object> attributeMap;

  /**
   * If any re-calculations have to be done before displaying, indexing etc. This method have an implementation if a
   * data object has transient fields which are calculated by other fields. This default implementation does nothing.
   */
  @Override
  public void recalculate()
  {
  }

  @Override
  @Transient
  public Integer getTenantId()
  {
    return this.tenant != null ? this.tenant.getId() : null;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDO#getTenant()
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id")
  @Override
  public TenantDO getTenant()
  {
    return this.tenant;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDO#setTenant(TenantDO)
   */
  @Override
  public AbstractBaseDO<I> setTenant(final TenantDO tenant)
  {
    this.tenant = tenant;
    return this;
  }

  @Override
  @Basic
  public boolean isDeleted()
  {
    return deleted;
  }

  @Override
  public void setDeleted(final boolean deleted)
  {
    this.deleted = deleted;
  }

  @Override
  @Basic
  public Date getCreated()
  {
    return created;
  }

  @Override
  public void setCreated(final Date created)
  {
    this.created = created;
  }

  @Override
  public void setCreated()
  {
    this.created = new Date();
  }

  /**
   *
   * Last update will be modified automatically for every update of the database object.
   *
   * @return
   */
  @Override
  @Basic
  @Column(name = "last_update")
  public Date getLastUpdate()
  {
    return lastUpdate;
  }

  @Override
  public void setLastUpdate(final Date lastUpdate)
  {
    this.lastUpdate = lastUpdate;
  }

  @Override
  public void setLastUpdate()
  {
    this.lastUpdate = new Date();
  }

  /**
   * Default value is false.
   *
   * @see org.projectforge.framework.persistence.api.BaseDO#isMinorChange()
   */
  @Override
  @Transient
  public boolean isMinorChange()
  {
    return minorChange;
  }

  @Override
  public void setMinorChange(final boolean value)
  {
    this.minorChange = value;
  }

  @Override
  public Object getTransientAttribute(final String key)
  {
    if (attributeMap == null) {
      return null;
    }
    return attributeMap.get(key);
  }

  @Override
  public Object removeTransientAttribute(String key) {
    Object obj = getTransientAttribute(key);
    if (obj != null) {
      attributeMap.remove(key);
    }
    return obj;
  }

  @Override
  public void setTransientAttribute(final String key, final Object value)
  {
    if (attributeMap == null) {
      attributeMap = new HashMap<String, Object>();
    }
    attributeMap.put(key, value);
  }

  /**
   * as json.
   */
  @Override
  public String toString() {
    return ToStringUtil.toJsonString(this);
  }

  /**
   * Copies all values from the given src object excluding the values created and lastUpdate. Do not overwrite created
   * and lastUpdate from the original database object.
   *
   * @param src
   * @param ignoreFields Does not copy these properties (by field name).
   * @return true, if any modifications are detected, otherwise false;
   */
  @Override
  public ModificationStatus copyValuesFrom(final BaseDO<? extends Serializable> src, final String... ignoreFields)
  {
    return copyValues(src, this, ignoreFields);
  }

  /**
   * Copies all values from the given src object excluding the values created and lastUpdate. Do not overwrite created
   * and lastUpdate from the original database object.
   *
   * @param src
   * @param dest
   * @param ignoreFields Does not copy these properties (by field name).
   * @return true, if any modifications are detected, otherwise false;
   */
  @SuppressWarnings("unchecked")
  public static ModificationStatus copyValues(final BaseDO src, final BaseDO dest, final String... ignoreFields)
  {
    return BaseDaoJpaAdapter.copyValues(src, dest, ignoreFields);
  }

  @Deprecated
  public static ModificationStatus getModificationStatus(final ModificationStatus currentStatus,
      final ModificationStatus status)
  {
    return currentStatus.combine(status);
  }

  /**
   * Returns whether or not to append the given <code>Field</code>.
   * <ul>
   * <li>Ignore transient fields
   * <li>Ignore static fields
   * <li>Ignore inner class fields</li>
   * </ul>
   *
   * @param field The Field to test.
   * @return Whether or not to consider the given <code>Field</code>.
   */
  protected static boolean accept(final Field field)
  {
    if (field.getName().indexOf(ClassUtils.INNER_CLASS_SEPARATOR_CHAR) != -1) {
      // Reject field from inner class.
      return false;
    }
    if (Modifier.isTransient(field.getModifiers()) == true) {
      // transients.
      return false;
    }
    if (Modifier.isStatic(field.getModifiers()) == true) {
      // transients.
      return false;
    }
    if ("created".equals(field.getName()) == true || "lastUpdate".equals(field.getName()) == true) {
      return false;
    }
    return true;
  }
}
