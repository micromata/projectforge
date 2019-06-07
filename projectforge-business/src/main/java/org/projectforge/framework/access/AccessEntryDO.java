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

package org.projectforge.framework.access;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import java.util.Objects;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.search.annotations.Indexed;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;

/**
 * Represents a single generic access entry for the four main SQL functionalities.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_GROUP_TASK_ACCESS_ENTRY",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "group_task_access_fk", "access_type" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_group_task_access_entry_group_task_access_fk",
            columnList = "group_task_access_fk"),
        @javax.persistence.Index(name = "idx_fk_t_group_task_access_entry_tenant_id", columnList = "tenant_id")
    })
public class AccessEntryDO implements Comparable<AccessEntryDO>, Serializable, BaseDO<Integer>
{
  private static final long serialVersionUID = 5973002212430487361L;

  // private static final Logger log = Logger.getLogger(AccessEntryDO.class);

  private TenantDO tenant;

  private AccessType accessType = null;

  private boolean accessSelect = false;

  private boolean accessInsert = false;

  private boolean accessUpdate = false;

  private boolean accessDelete = false;

  private Integer id;

  @Override
  @Id
  @GeneratedValue
  @Column(name = "pk")
  public Integer getId()
  {
    return id;
  }

  @Override
  public void setId(final Integer id)
  {
    this.id = id;
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
   * @see org.projectforge.framework.persistence.api.BaseDO#getTenantId()
   */
  @Override
  @Transient
  public Integer getTenantId()
  {
    return tenant != null ? tenant.getId() : null;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDO#setTenant(TenantDO)
   */
  @Override
  public AccessEntryDO setTenant(final TenantDO tenant)
  {
    this.tenant = tenant;
    return this;
  }

  /**
   * @return Always false.
   * @see org.projectforge.framework.persistence.api.BaseDO#isMinorChange()
   */
  @Override
  @Transient
  public boolean isMinorChange()
  {
    return false;
  }

  /**
   * Throws UnsupportedOperationException.
   * 
   * @see org.projectforge.framework.persistence.api.BaseDO#setMinorChange(boolean)
   */
  @Override
  public void setMinorChange(final boolean value)
  {
    throw new UnsupportedOperationException();
  }

  public AccessEntryDO()
  {
  }

  public AccessEntryDO(final AccessType accessType)
  {
    this.accessType = accessType;
  }

  public AccessEntryDO(final AccessType type, final boolean accessSelect, final boolean accessInsert,
      final boolean accessUpdate,
      final boolean accessDelete)
  {
    this.accessType = type;
    setAccess(accessSelect, accessInsert, accessUpdate, accessDelete);
  }

  public boolean hasPermission(final OperationType opType)
  {
    if (opType == OperationType.SELECT) {
      return this.accessSelect;
    } else if (opType == OperationType.INSERT) {
      return this.accessInsert;
    } else if (opType == OperationType.UPDATE) {
      return this.accessUpdate;
    } else {
      return this.accessDelete;
    }
  }

  /**
   */
  @Column(name = "access_type")
  @Enumerated(EnumType.STRING)
  public AccessType getAccessType()
  {
    return this.accessType;
  }

  public void setAccessType(final AccessType type)
  {
    this.accessType = type;
  }

  public void setAccess(final boolean accessSelect, final boolean accessInsert, final boolean accessUpdate,
      final boolean accessDelete)
  {
    this.accessSelect = accessSelect;
    this.accessInsert = accessInsert;
    this.accessUpdate = accessUpdate;
    this.accessDelete = accessDelete;
  }

  /**
   */
  @Column(name = "access_select")
  public boolean getAccessSelect()
  {
    return this.accessSelect;
  }

  public void setAccessSelect(final boolean value)
  {
    this.accessSelect = value;
  }

  @Column(name = "access_insert")
  public boolean getAccessInsert()
  {
    return this.accessInsert;
  }

  public void setAccessInsert(final boolean value)
  {
    this.accessInsert = value;
  }

  @Column(name = "access_update")
  public boolean getAccessUpdate()
  {
    return this.accessUpdate;
  }

  public void setAccessUpdate(final boolean value)
  {
    this.accessUpdate = value;
  }

  @Column(name = "access_delete")
  public boolean getAccessDelete()
  {
    return this.accessDelete;
  }

  public void setAccessDelete(final boolean value)
  {
    this.accessDelete = value;
  }

  /*
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final AccessEntryDO o)
  {
    return this.accessType.compareTo(o.accessType);
  }

  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof AccessEntryDO) {
      final AccessEntryDO other = (AccessEntryDO) o;
      if (Objects.equals(this.getAccessType(), other.getAccessType()) == false)
        return false;
      if (Objects.equals(this.getId(), other.getId()) == false)
        return false;
      return true;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    if (getAccessType() != null)
      hcb.append(getAccessType().ordinal());
    hcb.append(getId());
    return hcb.toHashCode();
  }

  @Override
  public String toString()
  {
    final ToStringBuilder sb = new ToStringBuilder(this);
    sb.append("id", getId());
    sb.append("type", this.accessType);
    sb.append("select", this.accessSelect);
    sb.append("insert", this.accessInsert);
    sb.append("update", this.accessUpdate);
    sb.append("delete", this.accessDelete);
    return sb.toString();
  }

  /**
   * Copies the values accessSelect, accessInsert, accessUpdate and accessDelete from the given src object excluding the
   * values created and modified. Null values will be excluded.
   * 
   * @param src
   */
  @Override
  public ModificationStatus copyValuesFrom(final BaseDO<? extends Serializable> src, final String... ignoreFields)
  {
    return AbstractBaseDO.copyValues(src, this, ignoreFields);
  }

  @Override
  public Object getTransientAttribute(final String key)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setTransientAttribute(final String key, final Object value)
  {
    throw new UnsupportedOperationException();
  }
}
