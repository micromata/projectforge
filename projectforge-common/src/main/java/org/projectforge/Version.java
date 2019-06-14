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

package org.projectforge;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a version number (major-release, minor-release, patch-level and build-number).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class Version implements Comparable<Version>, Serializable
{
  private static final long serialVersionUID = 1446772593211999270L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Version.class);

  private int majorRelease, minorRelease, patchLevel, buildNumber, betaVersion = Integer.MAX_VALUE,
      releaseCandidateVersion = Integer.MAX_VALUE;

  private boolean snapshot;

  private String asString;

  /**
   * Supported formats: "#" ("3"), "#.#" ("3.5"), "#.#.#" ("3.5.4") or "#.#.#.#" ("3.5.4.2"). Append b# for marking
   * version as beta version.
   * 
   * @param version
   */
  public Version(final String version)
  {
    if (version == null) {
      return;
    }
    String str = version.toLowerCase();
    final int snapshotPos = str.indexOf("-snapshot");
    if (snapshotPos > 0) {
      snapshot = true;
      str = str.substring(0, snapshotPos);
    }
    final int betaPos = str.indexOf('b');
    String betaString = null;
    String releaseCandidateString = null;
    if (betaPos >= 0) {
      betaString = str.substring(betaPos + 1);
      str = str.substring(0, betaPos);
    } else {
      final int rcPos = str.indexOf("rc");
      if (rcPos >= 0) {
        releaseCandidateString = str.substring(rcPos + 2);
        str = str.substring(0, rcPos);
      }
    }
    final String[] sa = StringUtils.split(str, ".");
    if (sa.length > 0) {
      majorRelease = parseInt(version, sa[0]);
      if (sa.length > 1) {
        minorRelease = parseInt(version, sa[1]);
        if (sa.length > 2) {
          patchLevel = parseInt(version, sa[2]);
          if (sa.length > 3) {
            buildNumber = parseInt(version, sa[3]);
          }
        }
      }
    }
    if (betaString != null) {
      if (betaString.length() > 0) {
        betaVersion = parseInt(version, betaString);
      } else {
        betaVersion = 0;
      }
    }
    if (releaseCandidateString != null) {
      if (releaseCandidateString.length() > 0) {
        releaseCandidateVersion = parseInt(version, releaseCandidateString);
      } else {
        releaseCandidateVersion = 0;
      }
    }
    asString();
  }

  public Version(final int majorRelease, final int minorRelease)
  {
    this(majorRelease, minorRelease, 0, 0);
  }

  public Version(final int majorRelease, final int minorRelease, final int patchLevel)
  {
    this(majorRelease, minorRelease, patchLevel, 0);
  }

  public Version(final int majorRelease, final int minorRelease, final int patchLevel, final int buildNumber)
  {
    this.majorRelease = majorRelease;
    this.minorRelease = minorRelease;
    this.patchLevel = patchLevel;
    this.buildNumber = buildNumber;
    asString();
  }

  public int getMajorRelease()
  {
    return majorRelease;
  }

  public int getMinorRelease()
  {
    return minorRelease;
  }

  public int getPatchLevel()
  {
    return patchLevel;
  }

  public int getBuildNumber()
  {
    return buildNumber;
  }

  /**
   * @param betaVersion
   * @return this for chaining.
   */
  public Version setBetaVersion(final int betaVersion)
  {
    this.betaVersion = betaVersion;
    return this;
  }

  public int getBetaVersion()
  {
    return betaVersion;
  }

  public boolean isBeta()
  {
    return betaVersion < Integer.MAX_VALUE;
  }

  /**
   * @return the releaseCandidateVersion
   */
  public int getReleaseCandidateVersion()
  {
    return releaseCandidateVersion;
  }

  /**
   * @param releaseCandidateVersion the releaseCandidateVersion to set
   * @return this for chaining.
   */
  public Version setReleaseCandidateVersion(final int releaseCandidateVersion)
  {
    this.releaseCandidateVersion = releaseCandidateVersion;
    return this;
  }

  public boolean isReleaseCandidate()
  {
    return releaseCandidateVersion < Integer.MAX_VALUE;
  }

  /**
   * @return the snapshot
   */
  public boolean isSnapshot()
  {
    return snapshot;
  }

  /**
   * Compares major, minor, patch and build number.
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final Version o)
  {
    int compare = compare(this.majorRelease, o.majorRelease);
    if (compare != 0) {
      return compare;
    }
    compare = compare(this.minorRelease, o.minorRelease);
    if (compare != 0) {
      return compare;
    }
    compare = compare(this.patchLevel, o.patchLevel);
    if (compare != 0) {
      return compare;
    }
    compare = compare(this.buildNumber, o.buildNumber);
    if (compare != 0) {
      return compare;
    }
    if (this.isReleaseCandidate() == true) {
      if (o.isReleaseCandidate() == true) {
        return compare(this.releaseCandidateVersion, o.releaseCandidateVersion);
      } else if (o.isBeta() == true) {
        // RC is higher than beta.
        return 1;
      } else {
        // RC is lower than normal version.
        return -1;
      }
    }
    if (o.isReleaseCandidate() == true) {
      if (this.isBeta() == true) {
        // beta is lower than RC.
        return -1;
      } else {
        // normal version is higher than RC.
        return 1;
      }
    }
    if (this.isBeta() == true) {
      if (o.isBeta() == true) {
        return compare(this.betaVersion, o.betaVersion);
      } else {
        // beta is lower than normal version.
        return -1;
      }
    }
    if (o.isBeta() == true) {
      // normal is higher than beta.
      return 1;
    }
    return 0;
  }

  /**
   * @return Version as string: "#.#" ("3.0"), "#.#.#" ("3.5.4"), "#.#.#.#" ("3.5.4.2") or "#.*.#b#" ("3.5.4.2b2").
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return asString;
  }

  private void asString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append(majorRelease);
    sb.append('.').append(minorRelease);
    if (patchLevel != 0 || buildNumber != 0) {
      sb.append('.').append(patchLevel);
      if (buildNumber != 0) {
        sb.append('.').append(buildNumber);
      }
    }
    if (betaVersion < Integer.MAX_VALUE) {
      sb.append('b').append(betaVersion);
    } else if (releaseCandidateVersion < Integer.MAX_VALUE) {
      sb.append("rc").append(releaseCandidateVersion);
    }
    if (snapshot == true) {
      sb.append("-SNAPSHOT");
    }
    asString = sb.toString();
  }

  private int parseInt(final String version, final String str)
  {
    try {
      return new Integer(str);
    } catch (final NumberFormatException ex) {
      log.error("Can't parse version string '" + version + "'. '" + str + "'isn't a number");
    }
    return 0;
  }

  private int compare(final int i1, final int i2)
  {
    if (i1 < i2) {
      return -1;
    } else if (i1 > i2) {
      return 1;
    } else {
      return 0;
    }
  }
}
