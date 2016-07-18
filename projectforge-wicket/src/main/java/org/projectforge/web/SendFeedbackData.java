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

package org.projectforge.web;

import java.io.Serializable;


public class SendFeedbackData  implements Serializable
{
  private static final long serialVersionUID = -1798940124320349936L;

  private String description;

  private String receiver;

  private String sender;

  private String subject;

  public String getDescription()
  {
    return description;
  }

  public void setDescription(String description)
  {
    this.description = description;
  }

  public String getReceiver()
  {
    return receiver;
  }

  public void setReceiver(String receiver)
  {
    this.receiver = receiver;
  }

  public String getSender()
  {
    return sender;
  }

  public void setSender(String sender)
  {
    this.sender = sender;
  }

  public String getSubject()
  {
    return subject;
  }
  
  public void setSubject(String subject)
  {
    this.subject = subject;
  }
}
