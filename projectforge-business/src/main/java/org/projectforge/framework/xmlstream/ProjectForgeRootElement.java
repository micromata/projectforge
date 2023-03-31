/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.xmlstream;

import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.PFDateTime;

import java.util.Date;
import java.util.TimeZone;

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
    final PFDateTime dateTime = PFDateTime.now().withPrecision(DatePrecision.SECOND);
    created = dateTime.getUtilDate();
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
