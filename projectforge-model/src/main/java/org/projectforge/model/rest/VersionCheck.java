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

package org.projectforge.model.rest;

import java.io.Serializable;
import java.util.Locale;
import java.util.TimeZone;

public class VersionCheck implements Serializable
{
  private final String sourceVersion;
  private String targetVersion;
  private final Locale locale;
  private final TimeZone timezone;

  public VersionCheck()
  {
    this(null, null, null);
  }

  public VersionCheck(final String sourceVersion, final Locale locale, final TimeZone timezone)
  {
    this(sourceVersion, null, locale, timezone);
  }

  public VersionCheck(final String sourceVersion, final String targetVersion, final Locale locale, final TimeZone timezone)
  {
    this.sourceVersion = sourceVersion;
    this.targetVersion = targetVersion;
    this.locale = locale;
    this.timezone = timezone;
  }

  public String getSourceVersion()
  {
    return sourceVersion;
  }

  public Locale getLocale()
  {
    return locale;
  }

  public TimeZone getTimezone()
  {
    return timezone;
  }

  public String getTargetVersion()
  {
    return targetVersion;
  }

  public void setTargetVersion(final String targetVersion)
  {
    this.targetVersion = targetVersion;
  }

  @Override
  public String toString()
  {
    return "VersionCheck{" +
        "sourceVersion='" + sourceVersion + '\'' +
        ", targetVersion='" + targetVersion + '\'' +
        ", locale=" + locale +
        ", timezone=" + timezone +
        '}';
  }
}
