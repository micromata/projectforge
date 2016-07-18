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

package org.projectforge.framework.xstream;

import java.util.Date;
import java.util.TimeZone;

import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;

/**
 * For export and import it's usefull to use an instance of this object to define for example the time zone, version etc.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public abstract class ProjectForgeRootElement
{
  private TimeZone timeZone;

  private Date created;

  @XmlField(asAttribute = true)
  private String version;

  /**
   * When was this element created (written)?
   */
  public Date getCreated()
  {
    return created;
  }

  /**
   * Set the current time stamp as created value (millis=0).
   */
  public ProjectForgeRootElement setCreated()
  {
    final DateHolder dh = new DateHolder(DatePrecision.SECOND);
    created = dh.getDate();
    return this;
  }

  /**
   * If you want to set this
   * @param created
   */
  public ProjectForgeRootElement setCreated(final Date created)
  {
    this.created = created;
    return this;
  }

  /**
   * The time zone used for the exported data (if human readable date format is used, e. g. with the ISODateConverter).
   */
  public TimeZone getTimeZone()
  {
    return timeZone;
  }

  public ProjectForgeRootElement setTimeZone(final TimeZone timeZone)
  {
    this.timeZone = timeZone;
    return this;
  }

  public String getVersion()
  {
    return version;
  }

  public ProjectForgeRootElement setVersion(final String version)
  {
    this.version = version;
    return this;
  }
}
