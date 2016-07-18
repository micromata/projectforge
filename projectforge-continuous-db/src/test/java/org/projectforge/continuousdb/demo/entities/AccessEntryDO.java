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

package org.projectforge.continuousdb.demo.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;


/**
 * Represents a single generic access entry for the four main SQL functionalities.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Table(name = "T_GROUP_TASK_ACCESS_ENTRY", uniqueConstraints = { @UniqueConstraint(columnNames = { "group_task_access_fk", "access_type"})})
public class AccessEntryDO
{
  private boolean accessSelect = false;

  private boolean accessInsert = false;

  private boolean accessUpdate = false;

  private boolean accessDelete = false;

  private Integer id;

  private String AccessType;


  @Id
  @GeneratedValue
  @Column(name = "pk")
  public Integer getId()
  {
    return id;
  }

  public void setId(final Integer id)
  {
    this.id = id;
  }

  public AccessEntryDO()
  {
  }

  public void setAccess(final boolean accessSelect, final boolean accessInsert, final boolean accessUpdate, final boolean accessDelete)
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


  @Column(name = "access_type", length = 100)
  public String getAccessType()
  {
    return AccessType;
  }

  public void setAccessType(String accessType)
  {
    AccessType = accessType;
  }
}
