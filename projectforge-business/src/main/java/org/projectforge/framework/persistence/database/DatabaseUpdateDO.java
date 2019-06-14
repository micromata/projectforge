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

package org.projectforge.framework.persistence.database;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.projectforge.Version;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * Represents data-base updates of ProjectForge core and plugins.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Table(name = "t_database_update", indexes = {
    @javax.persistence.Index(name = "idx_fk_t_database_update_executed_by_user_fk", columnList = "executed_by_user_fk")
})
public class DatabaseUpdateDO
{
  private String regionId;

  private Date updateDate;

  private Version version;

  private String description;

  private String executionResult;

  private PFUserDO executedBy;

  @Column(name = "region_id", length = 1000)
  public String getRegionId()
  {
    return regionId;
  }

  public void setRegionId(final String regionId)
  {
    this.regionId = regionId;
  }

  @Column(name = "version", length = 15)
  public String getVersionString()
  {
    return version != null ? version.toString() : null;
  }

  /**
   * @param version
   * @return this for chaining.
   */
  public DatabaseUpdateDO setVersionString(final String versionString)
  {
    if (versionString == null) {
      version = null;
    } else {
      version = new Version(versionString);
    }
    return this;
  }

  @Id
  @Column(name = "update_date")
  public Date getUpdateDate()
  {
    return updateDate;
  }

  /**
   * @param date
   * @return this for chaining.
   */
  public DatabaseUpdateDO setUpdateDate(final Date updateDate)
  {
    this.updateDate = updateDate;
    return this;
  }

  @Column(length = 4000)
  public String getDescription()
  {
    return description;
  }

  /**
   * @param description
   * @return this for chaining.
   */
  public DatabaseUpdateDO setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "executed_by_user_fk", nullable = false)
  public PFUserDO getExecutedBy()
  {
    return executedBy;
  }

  /**
   * @param executedBy
   * @return this for chaining.
   */
  public DatabaseUpdateDO setExecutedBy(final PFUserDO executedBy)
  {
    this.executedBy = executedBy;
    return this;
  }

  @Column(name = "execution_result", length = 1000)
  public String getExecutionResult()
  {
    return executionResult;
  }

  /**
   * @param executionResult
   * @return this for chaining.
   */
  public DatabaseUpdateDO setExecutionResult(String executionResult)
  {
    this.executionResult = executionResult;
    return this;
  }
}
