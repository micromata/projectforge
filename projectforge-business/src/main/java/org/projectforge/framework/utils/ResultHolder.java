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

import java.util.ArrayList;

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

/**
 * Stores messages in an array. Used by methods as return value.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ResultHolder
{
  private ResultHolderStatus status;

  private final ArrayList<IMessage> messages = new ArrayList<IMessage>();

  public interface IMessage
  {
    public String getMessage();
  }

  public class Message implements IMessage
  {
    private final String message;

    public Message(final String message)
    {
      this.message = message;
    }

    @Override
    public String getMessage()
    {
      return message;
    }
  }

  public class I18nMessage implements IMessage
  {
    private final String messageKey;

    private Object[] params;

    public I18nMessage(final String messageKey)
    {
      this.messageKey = messageKey;
    }

    public I18nMessage(final String messageKey, final Object... params)
    {
      this.messageKey = messageKey;
      this.params = params;
    }

    @Override
    public String getMessage()
    {
      return ThreadLocalUserContext.getLocalizedMessage(messageKey, params);
    }
  }

  public ResultHolderStatus getStatus()
  {
    return status;
  }

  public ResultHolder setStatus(final ResultHolderStatus status)
  {
    this.status = status;
    return this;
  }

  public void addMessage(final IMessage message)
  {
    this.messages.add(message);
  }

  public ArrayList<IMessage> getMessages()
  {
    return this.messages;
  }
}
