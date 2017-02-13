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

package org.projectforge.continuousdb;

import org.projectforge.Version;

/**
 * Represents a update entry (Groovy or Java).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class UpdateEntry implements Comparable<UpdateEntry>
{
  private static final long serialVersionUID = -8205244215928531249L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UpdateEntry.class);

  protected transient UpdatePreCheckStatus preCheckStatus = UpdatePreCheckStatus.UNKNOWN;

  protected transient UpdateRunningStatus runningStatus;

  public abstract Version getVersion();

  public abstract void setVersion(final Version version);

  /**
   * @return true if this update entry is the initial entry for schema creation of a new module.
   */
  public abstract boolean isInitial();

  /**
   * Should be of iso format: 2011-02-28 (yyyy-MM-dd).
   */
  public abstract String getDate();

  /**
   * Identifier of the software region: core for ProjectForge core or plugin identifier.
   */
  public abstract String getRegionId();

  public UpdatePreCheckStatus getPreCheckStatus()
  {
    return preCheckStatus;
  }

  public void setPreCheckStatus(final UpdatePreCheckStatus preCheckStatus)
  {
    this.preCheckStatus = preCheckStatus;
  }

  public UpdateRunningStatus getRunningStatus()
  {
    return runningStatus;
  }

  public void setRunningStatus(final UpdateRunningStatus runningStatus)
  {
    this.runningStatus = runningStatus;
  }

  public abstract String getDescription();

  public abstract void setDescription(final String description);

  protected abstract UpdatePreCheckStatus runPreCheck();

  public UpdatePreCheckStatus runPreCheckSafely()
  {
    try {
      return runPreCheck();
    } catch (RuntimeException e) {
      log.error("Exception while running preCheck: " + e.getMessage(), e);
      return UpdatePreCheckStatus.FAILED;
    }
  }

  public abstract UpdateRunningStatus runUpdate();

  /**
   * Please note: this method is only called for initial update entries! Does nothing at default.
   */
  public int createMissingIndices()
  {
    return 0;
  }

  public abstract String getPreCheckResult();

  public abstract String getRunningResult();

  @Override
  public String toString()
  {
    final StringBuffer buf = new StringBuffer();
    buf.append(this.getClass()).append("=[");
    if (getRegionId() != null) {
      buf.append("regionId=[").append(getRegionId()).append("]");
    }
    if (getVersion() != null) {
      buf.append("regionId=[").append(getVersion()).append("]");
    }
    if (getDate() != null) {
      buf.append("date=[").append(getDate()).append("]");
    }
    if (getRunningStatus() != null) {
      buf.append("runningStatus=[").append(getRunningStatus()).append("]");
    }
    if (getRunningResult() != null) {
      buf.append("runningResult=[").append(getRunningResult()).append("]");
    }
    if (getDescription() != null) {
      buf.append("description=[").append(getDescription()).append("]");
    }
    buf.append("]");
    return buf.toString();
  }

  /**
   * Compares the dates of the both entries in descending order. For equal dates, the region id and the version is
   * compared.
   *
   * @param o
   */
  @Override
  public int compareTo(final UpdateEntry o)
  {
    int res = o.getDate().compareTo(getDate());
    if (res != 0) {
      return res;
    }
    res = o.getRegionId().compareTo(getRegionId());
    if (res != 0) {
      return res;
    }
    return o.getVersion().compareTo(getVersion());
  }
}
