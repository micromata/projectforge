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

package org.projectforge.common;

import java.io.File;
import java.io.IOException;

/**
 * Some helper methods ...
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class CanonicalFileUtils
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CanonicalFileUtils.class);

  public static String absolutePath(File file) {
    if (file == null) {
      return null;
    }
    try {
      return file.getCanonicalPath();
    } catch (IOException ex) {
      log.error("Internal error while trying to get canonical path of " + file.getPath());
      return file.getAbsolutePath();
    }
  }

  public static File absolute(File file) {
    if (file == null) {
      return null;
    }
    try {
      return file.getCanonicalFile();
    } catch (IOException ex) {
      log.error("Internal error while trying to get canonical path of " + file.getPath());
      return file.getAbsoluteFile();
    }
  }
}
