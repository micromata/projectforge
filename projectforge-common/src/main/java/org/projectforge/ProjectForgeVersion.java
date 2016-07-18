// automatically generated for new releases

////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2014, Kai Reinhard
//           Dual-licensed.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge;

import java.util.ResourceBundle;

public class ProjectForgeVersion
{
  public static final String APP_ID = "ProjectForge";

  public static final String VERSION_STRING = ResourceBundle.getBundle("version").getString("version.number");

  public static final String RELEASE_TIMESTAMP = ResourceBundle.getBundle("version").getString("version.buildDate");

  public static final String RELEASE_DATE = RELEASE_TIMESTAMP.split(" ")[0];

  public static final String YEAR = RELEASE_TIMESTAMP.split("-")[0];

}
