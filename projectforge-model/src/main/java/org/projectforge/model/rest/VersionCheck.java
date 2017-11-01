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
