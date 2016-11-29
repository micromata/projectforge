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

package org.projectforge.mail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Message;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * Represents a mail. Mails can be received from a MailAccount or can be sent via SendMail.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class Mail implements Comparable<Mail>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Mail.class);

  public static final String CONTENTTYPE_HTML = "html";

  public static final String CONTENTTYPE_TEXT = "plain";

  private Message message;

  private int messageNumber = -1;

  private Date date;

  private boolean deleted;

  private boolean recent;

  private boolean seen;

  private String from;

  private String fromRealname;

  private List<InternetAddress> to = new ArrayList<>();

  private String toRealname;

  private String subject;

  private String content;

  private String contentType;

  private String charset = "UTF-8";

  public Mail()
  {
  }

  /**
   * Message from the mail server (for messages received).
   */
  public Message getMessage()
  {
    return message;
  }

  public void setMessage(Message message)
  {
    this.message = message;
  }

  /**
   * The unique message number from the mail server (for received messages only).
   */
  public int getMessageNumber()
  {
    return messageNumber;
  }

  public void setMessageNumber(int messageNumber)
  {
    this.messageNumber = messageNumber;
  }

  /**
   * For received messages only.
   */
  public Date getDate()
  {
    return date;
  }

  public void setDate(Date date)
  {
    this.date = date;
  }

  /**
   * Flag of the message (flag from the mail server for received messages only).
   */
  public boolean isDeleted()
  {
    return deleted;
  }

  public void setDeleted(boolean deleted)
  {
    this.deleted = deleted;
  }

  /**
   * Flag of the message (flag from the mail server for received messages only).
   */
  public boolean isRecent()
  {
    return recent;
  }

  public void setRecent(boolean recent)
  {
    this.recent = recent;
  }

  /**
   * Flag of the message (flag from the mail server for received messages only).
   */
  public boolean isSeen()
  {
    return seen;
  }

  public void setSeen(boolean seen)
  {
    this.seen = seen;
  }

  public String getFrom()
  {
    return from;
  }

  public void setFrom(String from)
  {
    this.from = from;
  }

  public String getFromRealname()
  {
    return fromRealname;
  }

  public void setFromRealname(String fromRealname)
  {
    this.fromRealname = fromRealname;
  }

  public void addTo(String to)
  {
    try {
      this.to.add(new InternetAddress(to));
    } catch (AddressException e) {
      log.warn("Could not create InternetAddress from mail: " + to);
    }
  }

  public List<InternetAddress> getTo()
  {
    return to;
  }

  public void setTo(PFUserDO user)
  {
    addTo(user.getEmail());
    if (StringUtils.isBlank(getToRealname())) {
      setToRealname(user.getFullname());
    }
  }

  public void setTo(String mailAdress, String realName)
  {
    addTo(mailAdress);
    if (StringUtils.isBlank(getToRealname())) {
      setToRealname(realName);
    }
  }

  public String getToRealname()
  {
    return toRealname;
  }

  public void setToRealname(String toRealname)
  {
    this.toRealname = toRealname;
  }

  public String getSubject()
  {
    return subject;
  }

  public void setSubject(String subject)
  {
    this.subject = subject;
  }

  /**
   * For your convenience. Sets the subject and prepends the ProjectForge standard: "[ProjectForge] "
   *
   * @param subject
   */
  public void setProjectForgeSubject(String subject)
  {
    this.subject = SendMail.getProjectForgeSubject(subject);
  }

  public String getContent()
  {
    return content;
  }

  public void setContent(String content)
  {
    this.content = content;
  }

  /**
   * If given, then the content type of the message will be set (e. g. "text/html").
   *
   * @return
   */
  public String getContentType()
  {
    return contentType;
  }

  public void setContentType(String contentType)
  {
    this.contentType = contentType;
  }

  /**
   * Default "UTF-8"
   */
  public String getCharset()
  {
    return charset;
  }

  public void setCharset(String charset)
  {
    this.charset = charset;
  }

  @Override
  public String toString()
  {
    ToStringBuilder sb = new ToStringBuilder(this);
    sb.append("from", getFrom());
    sb.append("fromRealname", getFromRealname());
    sb.append("to", getTo());
    sb.append("toRealname", getToRealname());
    sb.append("subject", getSubject());
    sb.append("contentType", getContentType());
    sb.append("charset", getCharset());
    if (content != null) {
      sb.append("content", getContent());
    }
    if (messageNumber != -1) {
      sb.append("no", getMessageNumber());
    }
    if (date != null) {
      sb.append("date", getDate());
    }
    if (deleted == true) {
      sb.append("deleted", isDeleted());
    }
    if (recent == true) {
      sb.append("recent", isRecent());
    }
    if (seen == true) {
      sb.append("seen", isSeen());
    }
    return sb.toString();
  }

  public int compareTo(Mail o)
  {
    if (this.messageNumber < o.messageNumber) {
      return -1;
    } else if (this.messageNumber == o.messageNumber) {
      return 0;
    } else {
      return 1;
    }
  }
}
