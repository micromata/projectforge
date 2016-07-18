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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.projectforge.Version;

/**
 * Represents a update (written in Java).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class UpdateEntryImpl extends UpdateEntry
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UpdateEntryImpl.class);

  private static final long serialVersionUID = -1178486631632477422L;

  private final static TimeZone UTC = TimeZone.getTimeZone("UTC");

  private String regionId;

  private Version version;

  private String date;

  private String description;

  private boolean initial;

  public UpdateEntryImpl()
  {
  }

  /**
   * Constructor only for initial schema updates (for new plug-ins). Otherwise the versionString is mandatory.
   * 
   * @param regionId
   * @param isoDateString Date string must be of iso format: yyyy-MM-dd.
   * @param description
   */
  public UpdateEntryImpl(final String regionId, final String isoDateString, final String description)
  {
    this(regionId, "0.0", isoDateString, description);
    this.initial = true;
  }

  /**
   * @param regionId
   * @param versionString
   * @param isoDateString Date string must be of iso format: yyyy-MM-dd.
   * @param description
   */
  public UpdateEntryImpl(final String regionId, final String versionString, final String isoDateString,
      final String description)
  {
    this.regionId = regionId;
    final Date testDate = parseUTCIsoDate(isoDateString);
    if (testDate == null) {
      log.error("Given date doesn't match the iso format yyyy-MM-dd: " + isoDateString);
    }
    this.date = isoDateString;
    this.version = new Version(versionString);
    this.description = description;
  }

  private Date parseUTCIsoDate(String isoDateString)
  {
    final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    df.setTimeZone(UTC);
    Date date;
    try {
      date = df.parse(isoDateString);
    } catch (final ParseException ex) {
      return null;
    }
    return date;

  }

  @Override
  public String getRegionId()
  {
    return this.regionId;
  }

  public void setRegionId(final String regionId)
  {
    this.regionId = regionId;
  }

  @Override
  public Version getVersion()
  {
    return version;
  }

  @Override
  public void setVersion(final Version version)
  {
    this.version = version;
  }

  /**
   * @return true if this update entry is the initial entry for schema creation of a new module.
   */
  @Override
  public boolean isInitial()
  {
    return initial;
  }

  @Override
  public String getDate()
  {
    return this.date;
  }

  @Override
  public String getDescription()
  {
    return this.description;
  }

  @Override
  public void setDescription(final String description)
  {
    this.description = description;
  }

  @Override
  public String getPreCheckResult()
  {
    return this.preCheckStatus != null ? this.preCheckStatus.toString() : "";
  }

  @Override
  public String getRunningResult()
  {
    return this.runningStatus != null ? this.runningStatus.toString() : "";
  }

  @Override
  public String toString()
  {
    final ReflectionToStringBuilder tos = new ReflectionToStringBuilder(this);
    return tos.toString();
  }
}
