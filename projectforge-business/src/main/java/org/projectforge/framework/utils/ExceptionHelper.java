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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;

/**
 * Some helper methods ...
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ExceptionHelper
{
  /**
   * Gets the stackTrace of the given message in the form "at
   * org.projectforge.framework.access.AccessChecker.hasPermission(AccessChecker.java:79) at
   * org.projectforge.common.task.TaskDao.hasInsertAccess(TaskDao.java:176) at ..." without entries does not match the beginning of
   * only4Namespace. <br/> Also stack trace entries containing "CGLIB$$" will be ignored in the output.
   * @param ex
   * @param only4Namespace
   * @return
   */
  public static String getFilteredStackTrace(Throwable ex, String only4Namespace)
  {
    StringBuffer buf = new StringBuffer();
    StackTraceElement[] sta = ex.getStackTrace();
    boolean ignored = false;
    if (sta != null && sta.length > 0) {
      for (StackTraceElement ste : sta) {
        if (ignored == true) {
          if (ignore(ste.getClassName(), only4Namespace) == true) {
            continue;
          }
        } else {
          ignored = false;
        }
        if (ignore(ste.getClassName(), only4Namespace) == true) {
          buf.append(" at ...");
          ignored = true;
          continue;
        }
        buf.append(" at ");
        buf.append(ste.toString());
      }
    }
    return buf.toString();
  }
  
  public static String printStackTrace(Throwable ex)
  {
    final StringWriter writer = new StringWriter();
    final PrintWriter pw = new PrintWriter(writer);
    ex.printStackTrace(pw);
    pw.flush();
    pw.close();
    return writer.toString();
  }
  
  /**
   * @param ex
   * @return the root cause or if not root cause exists, the exception itself.
   */
  public static Throwable getRootCause(Throwable ex) {
    if (ex.getCause() != null)
      return getRootCause(ex.getCause());
    return ex;
  }

  private static boolean ignore(String className, String only4Namespace)
  {
    return className.startsWith(only4Namespace) == false || StringUtils.contains(className, "CGLIB$$") == true;
  }
}
