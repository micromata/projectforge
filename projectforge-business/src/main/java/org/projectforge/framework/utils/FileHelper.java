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

package org.projectforge.framework.utils;

import java.io.File;
import java.util.Date;

import org.projectforge.framework.time.DateHelper;

/**
 * Some helper methods ...
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class FileHelper
{
  public static final String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.";

  public static final String SUBSTITUTE_CHARS = "ÄÖÜäöüß";

  public static final String[] SUBSTITUTED_BY = { "Ae", "Oe", "Ue", "ae", "oe", "ue", "ss"};

  /**
   * Return the given path itself if it is already absolute, otherwise absolute path of given path relative to given parent.
   * @param parent
   * @param path
   * @return
   */
  public static String getAbsolutePath(String parent, String path)
  {
    File file = new File(path);
    if (file.isAbsolute() == true) {
      return path;
    }
    file = new File(parent, path);
    return file.getAbsolutePath();
  }

  /**
   * Creates a safe filename from the given string by converting all non specified characters will replaces by an underscore or will be
   * substitue. Example: "Schrödinger" -&gt; "Schroedinger", "http://www.micromata.de" -&gt; "http_www.micromata.de".
   * 
   * @param str
   * @param maxlength The maximum length of the result.
   * @return
   */
  public static String createSafeFilename(String str, int maxlength)
  {
    return createSafeFilename(str, null, maxlength, false);
  }

  /**
   * FileHelper.createSafeFilename("basename", ".pdf", 8, true)) -> "basename_2010-08-12.pdf".
   * @param str
   * @param suffix
   * @param maxlength
   * @param appendTimestamp
   * @return
   */
  public static String createSafeFilename(final String str, final String suffix, final int maxlength, final boolean appendTimestamp)
  {
    final StringBuffer buf = new StringBuffer();
    boolean escaped = false;
    int count = 0;
    for (int i = 0; i < str.length() && count < maxlength; i++) {
      char ch = str.charAt(i);
      if (ALLOWED_CHARS.indexOf(ch) >= 0) {
        buf.append(ch);
        count++;
        escaped = false;
        continue;
      } else if (SUBSTITUTE_CHARS.indexOf(ch) >= 0) {
        String substitution = SUBSTITUTED_BY[SUBSTITUTE_CHARS.indexOf(ch)];
        int remain = maxlength - count;
        if (substitution.length() > remain) {
          // String must be shorten to ensure max length.
          buf.append(substitution.substring(0, remain));
        } else {
          buf.append(substitution);
        }
        count += substitution.length();
        escaped = false;
        continue;
      } else if (escaped == false) {
        buf.append("_");
        count++;
        escaped = true;
      }
    }
    if (appendTimestamp == true) {
      buf.append('_').append(DateHelper.getDateAsFilenameSuffix(new Date()));
    }
    if (suffix != null) {
      buf.append(suffix);
    }
    return buf.toString();
  }
}
