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
 * Logs errors, warnings and actions while executing algorithm.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ActionLog implements Serializable
{
  private static final long serialVersionUID = -4557854574067360656L;

  private int counterSuccess = 0;

  private int counterErrors = 0;

  private StringBuffer infoLog;

  private StringBuffer errorLog;

  public ActionLog reset()
  {
    counterSuccess = counterErrors = 0;
    infoLog = errorLog = null;
    return this;
  }

  public void incrementCounterSuccess()
  {
    counterSuccess++;
  }

  public int getCounterSuccess()
  {
    return counterSuccess;
  }

  public void incrementCounterErrors()
  {
    counterErrors++;
  }

  public int getCounterErrors()
  {
    return counterErrors;
  }

  public ActionLog logInfo(final String msg)
  {
    if (infoLog == null) {
      infoLog = new StringBuffer();
    }
    infoLog.append(msg).append("\n");
    return this;
  }

  public String getInfoLog()
  {
    return infoLog.toString();
  }

  public ActionLog logError(final String msg)
  {
    if (errorLog == null) {
      errorLog = new StringBuffer();
    }
    errorLog.append(msg).append("\n");
    return this;
  }

  public String getErrorLog()
  {
    return errorLog.toString();
  }
}
