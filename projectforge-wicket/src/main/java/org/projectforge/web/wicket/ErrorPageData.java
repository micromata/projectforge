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

package org.projectforge.web.wicket;

import org.projectforge.web.SendFeedbackData;

/**
 * Data of feedback panel in error page.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ErrorPageData extends SendFeedbackData
{
  private static final long serialVersionUID = 7096854957182811998L;

  private String message;
  
  private String messageNumber;

  private String stackTrace;

  private String rootCause;

  private String rootCauseStackTrace;

  /**
   * Error message text.
   */
  public String getMessage()
  {
    return message;
  }

  public void setMessage(String message)
  {
    this.message = message;
  }
  
  public String getMessageNumber()
  {
    return messageNumber;
  }

  public void setMessageNumber(String messageNumber)
  {
    this.messageNumber = messageNumber;
  }

  public String getStackTrace()
  {
    return stackTrace;
  }

  public void setStackTrace(String stackTrace)
  {
    this.stackTrace = stackTrace;
  }

  public String getRootCause()
  {
    return rootCause;
  }

  public void setRootCause(String rootCause)
  {
    this.rootCause = rootCause;
  }
  
  public String getRootCauseStackTrace()
  {
    return rootCauseStackTrace;
  }
  
  public void setRootCauseStackTrace(String rootCauseStackTrace)
  {
    this.rootCauseStackTrace = rootCauseStackTrace;
  }
}
