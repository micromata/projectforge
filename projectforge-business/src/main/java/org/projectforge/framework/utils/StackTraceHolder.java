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

import java.io.Serializable;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class StackTraceHolder implements Serializable
{
  private static final long serialVersionUID = -3352841763605483751L;

  private static final int DEFAULT_DEPTH = 10;

  private StackTraceElement[] debugStackTrace;

  public StackTraceHolder()
  {
    this(DEFAULT_DEPTH);
  }

  public StackTraceHolder(final int depth)
  {
    final StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    if (stackTraceElements == null) {
      return;
    }
    int d = stackTraceElements.length;
    if (depth < d) {
      d = depth;
    }
    debugStackTrace = new StackTraceElement[d];
    for (int i = 0; i < d; i++) {
      debugStackTrace[i] = stackTraceElements[i];
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    if (debugStackTrace == null) {
      return null;
    }
    final StringBuffer buf = new StringBuffer();
    for (final StackTraceElement el : debugStackTrace) {
      buf.append("\nat " + el.getClassName()).append(".").append(el.getMethodName()).append("(").append(el.getFileName()).append(":")
      .append(el.getLineNumber()).append(")");
    }
    return buf.toString();
  }
}
