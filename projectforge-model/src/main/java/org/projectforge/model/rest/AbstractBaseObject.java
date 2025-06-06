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

package org.projectforge.model.rest;

import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * For documentation please refer the ProjectForge-API: DefaultBaseDO object.
 * Base fields (like DefaultBaseDO of ProjectForge webapp package).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class AbstractBaseObject
{
  private Long id;

  private boolean deleted;

  @JsonSerialize(using = CustomerDateAndTimeSerialize.class)
  @JsonDeserialize(using = CustomerDateAndTimeDeserialize.class)
  private Date created, lastUpdate;

  public AbstractBaseObject()
  {
  }

  public Long getId()
  {
    return id;
  }

  public void setId(final Long id)
  {
    this.id = id;
  }

  public boolean isDeleted()
  {
    return deleted;
  }

  public void setDeleted(final boolean deleted)
  {
    this.deleted = deleted;
  }

  public Date getCreated()
  {
    return created;
  }

  public void setCreated(final Date created)
  {
    this.created = created;
  }

  public Date getLastUpdate()
  {
    return lastUpdate;
  }

  public void setLastUpdate(final Date lastUpdate)
  {
    this.lastUpdate = lastUpdate;
  }
}
