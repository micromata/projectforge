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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Store;
import javax.mail.search.FlagTerm;

/**
 * Connects to a mail server and receives mails.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MailAccount
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MailAccount.class);

  public static final String INBOX = "INBOX";

  private Folder folder;

  private Store store;

  public MailAccount(Store store, Folder folder)
  {
    this.store = store;
    this.folder = folder;

  }

  //  /** Gets the stored email of the given user. */
  //  public Mail getMail(final int mailId)
  //  {
  //    final Mail mail = new Mail();
  //    try {
  //      setEnvelope(mail, folder.getMessage(mailId));
  //      mail.setContent(getContent(mail.getMessage()));
  //      disconnect();
  //    } catch (IndexOutOfBoundsException ex) {
  //      log.warn("Message number out of range: " + mailId);
  //    } catch (MessagingException ex) {
  //      log.warn("", ex);
  //    } catch (IOException ex) {
  //      log.warn("", ex);
  //    }
  //    return mail;
  //  }

  /**
   * Gets a list of all Emails matching the given filter.
   * 
   * @return ArrayList of all found Email.
   */
  public Mail[] getMails(final MailFilter filter)
  {
    //    if (folder == null || folder.isOpen() == false) {
    //      log.error("Folder is not opened, can't get mails: "
    //          + this.mailAcccountConfig.getUsername()
    //          + "@"
    //          + this.mailAcccountConfig.getHostname()
    //          + " via "
    //          + this.mailAcccountConfig.getProtocol());
    //      return null;
    //    }
    final List<Mail> table = new ArrayList<Mail>();
    try {
      int totalMessages = folder.getMessageCount();
      log.debug("New messages: " + folder.getNewMessageCount());
      log.debug("Total messages: " + totalMessages);
      if (totalMessages == 0) {
        return new Mail[0];
      }
      // Attributes & Flags for all messages ..
      final Message[] msgs;
      if (filter.isOnlyRecent() == true) {
        msgs = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
      } else {
        msgs = folder.getMessages();
      }
      // Use a suitable FetchProfile
      final FetchProfile fp = new FetchProfile();
      fp.add(FetchProfile.Item.ENVELOPE);
      fp.add(FetchProfile.Item.FLAGS);
      fp.add("X-Mailer");
      folder.fetch(msgs, fp);

      for (int i = 0; i < msgs.length; i++) {
        final Mail mail = new Mail();
        setEnvelope(mail, msgs[i]);
        mail.setContent(getContent(mail.getMessage()));
        // if (filter == null || (mail.isRecent() == true && filter.isRecent() == true)
        // || (mail.isSeen() == true && filter.isSeen() == true)
        // || (mail.isDeleted() == true && filter.isDeleted() == true)) {
        table.add(mail);
        // }
      }
      // No sort the table by date:
      final Mail[] mailArray = new Mail[table.size()];
      table.toArray(mailArray);
      Arrays.sort(mailArray);
      return mailArray;
    } catch (javax.mail.MessagingException ex) {
      log.info(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    } catch (IOException ex) {
      log.info(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Opens the connection to the mailserver. Don't forget to call disconnect if this method returns true!
   * 
   * @param mbox The folder name to open. If null then the default folder will be opened.
   * @param readwrite If false then the mbox is connected in readonly mode.
   * @return true on success, otherwise false.
   */
  //  @Deprecated
  //  public boolean connect(final String mbox, final boolean readwrite)
  //  {
  //    try {
  //      // Get a Properties object
  //      final Properties props = new Properties();
  //      if (configurationService.getUsersSSLSocketFactory() != null) {
  //        props
  //            .put("mail." + mailAcccountConfig.getProtocol() + ".ssl.socketFactory",
  //                configurationService.getUsersSSLSocketFactory());
  //      }
  //      final Session session = Session.getDefaultInstance(props, null);
  //
  //      // Get a Store object
  //      store = null;
  //      try {
  //        store = session.getStore(mailAcccountConfig.getProtocol());
  //      } catch (javax.mail.NoSuchProviderException ex) {
  //        log.error(ex.getMessage(), ex);
  //        // serverData.setErrorMessageKey("mail.error.noSuchProviderException");
  //        return false;
  //      }
  //      if (mailAcccountConfig.getPort() > 0) {
  //        store.connect(mailAcccountConfig.getHostname(), mailAcccountConfig.getPort(), mailAcccountConfig.getUsername(),
  //            mailAcccountConfig
  //                .getPassword());
  //      } else {
  //        store.connect(mailAcccountConfig.getHostname(), mailAcccountConfig.getUsername(),
  //            mailAcccountConfig.getPassword());
  //      }
  //      // Open the Folder
  //
  //      folder = store.getDefaultFolder();
  //      if (folder == null) {
  //        // serverData.setErrorMessageKey("mail.error.noDefaultFolder");
  //        return false;
  //      }
  //
  //      if (mbox != null) {
  //        folder = folder.getFolder(mbox);
  //        if (folder == null) {
  //          // serverData.setErrorMessageKey("mail.error.invalidFolder");
  //          return false;
  //        }
  //      }
  //      if (readwrite == true) {
  //        // try to open read/write and if that fails try read-only
  //        try {
  //          folder.open(Folder.READ_WRITE);
  //        } catch (MessagingException ex) {
  //          log.error(
  //              "Can't open mbox in read-write mode, try to open folder in read-only mode instead: " + ex.getMessage());
  //          folder.open(Folder.READ_ONLY);
  //        }
  //      } else {
  //        folder.open(Folder.READ_ONLY);
  //      }
  //    } catch (javax.mail.MessagingException ex) {
  //      // serverData.setErrorMessageKey("mail.error.messagingException");
  //      // serverData.setOriginalErrorMessage(ex.getMessage());
  //      log.info(ex.getMessage(), ex);
  //      return false;
  //    }
  //    return true;
  //  }

  /**
   * Disconnects the folder and store if given and is opened yet.
   * 
   * @return
   */
  //  @Deprecated
  //  public boolean disconnect()
  //  {
  //    boolean success = true;
  //    if (folder != null && folder.isOpen() == true) {
  //      try {
  //        folder.close(false);
  //      } catch (MessagingException ex) {
  //        log.error("Exception encountered while trying tho close the folder: " + ex, ex);
  //        success = false;
  //      }
  //    }
  //    if (store != null && store.isConnected() == true) {
  //      try {
  //        store.close();
  //      } catch (MessagingException ex) {
  //        log.error("Exception encountered while trying to close the store: " + ex, ex);
  //        success = false;
  //      }
  //    }
  //    return success;
  //  }

  protected void setEnvelope(final Mail mail, final Message message) throws javax.mail.MessagingException
  {
    mail.setMessage(message);
    Address[] addr;
    // ID
    mail.setMessageNumber(message.getMessageNumber());

    // FROM
    StringBuffer buf = new StringBuffer();
    addr = message.getFrom();
    if (addr != null) {
      for (int j = 0; j < addr.length; j++) {
        if (j > 0) {
          buf.append(",");
        }
        buf.append(addr[j].toString());
      }
    }
    mail.setFrom(buf.toString());

    // TO
    addr = message.getRecipients(Message.RecipientType.TO);
    buf = new StringBuffer();
    if (addr != null) {
      for (int j = 0; j < addr.length; j++) {
        if (j > 0) {
          buf.append(",");
        }
        buf.append(addr[j].toString());
      }
    }
    mail.addTo(buf.toString());

    // SUBJECT
    mail.setSubject(message.getSubject());

    // DATE
    final Date date = message.getSentDate();
    if (date != null) {
      mail.setDate(date);
    } else { // Needed for compareTo (assume 1.1.1970)
      mail.setDate(new Date(0));
    } // FLAGS
    final Flags flags = message.getFlags();
    final Flags.Flag[] systemFlags = flags.getSystemFlags(); // get the system flags

    for (int i = 0; i < systemFlags.length; i++) {
      final Flags.Flag flag = systemFlags[i];
      if (flag == Flags.Flag.ANSWERED) {
        // Ignore this flag
      } else if (flag == Flags.Flag.DELETED) {
        mail.setDeleted(true);
      } else if (flag == Flags.Flag.DRAFT) {
        // Ignore this flag
      } else if (flag == Flags.Flag.FLAGGED) {
        // Ignore this flag
      } else if (flag == Flags.Flag.RECENT) {
        mail.setRecent(true);
      } else if (flag == Flags.Flag.SEEN) {
        mail.setSeen(true);
      } else {
        // skip it
      }
    }
  }

  private String getContent(final Part msg) throws MessagingException, IOException
  {
    final StringBuffer buf = new StringBuffer();
    getContent(msg, buf);
    return buf.toString();
  }

  private void getContent(final Part msg, final StringBuffer buf) throws MessagingException, IOException
  {
    if (log.isDebugEnabled() == true) {
      log.debug("CONTENT-TYPE: " + msg.getContentType());
    }
    String filename = msg.getFileName();
    if (filename != null) {
      log.debug("FILENAME: " + filename);
    }
    // Using isMimeType to determine the content type avoids
    // fetching the actual content data until we need it.
    if (msg.isMimeType("text/plain")) {
      log.debug("This is plain text");
      try {
        buf.append(msg.getContent());
      } catch (UnsupportedEncodingException ex) {
        buf.append("Unsupported charset by java mail, sorry: " + "CONTENT-TYPE=[" + msg.getContentType() + "]");
      }
    } else if (msg.isMimeType("text/html")) {
      log.debug("This is html text");
      buf.append(msg.getContent());
    } else if (msg.isMimeType("multipart/*")) {
      log.debug("This is a Multipart");
      final Multipart multiPart = (Multipart) msg.getContent();
      int count = multiPart.getCount();
      for (int i = 0; i < count; i++) {
        if (i > 0) {
          buf.append("\n----------\n");
        }
        getContent(multiPart.getBodyPart(i), buf);
      }
    } else if (msg.isMimeType("message/rfc822")) {
      log.debug("This is a Nested Message");
      buf.append(msg.getContent());
    } else {
      log.debug("This is an unknown type");
      // If we actually want to see the data, and it's not a
      // MIME type we know, fetch it and check its Java type.
      final Object obj = msg.getContent();
      if (obj instanceof String) {
        buf.append(obj);
      } else if (obj instanceof InputStream) {
        log.debug("Inputstream");
        buf.append("Attachement: ");
        if (filename != null) {
          buf.append(filename);
        } else {
          buf.append("Unsupported format (not a file).");
        }
      } else {
        log.error("Should not occur");
        buf.append("Unsupported type");
      }
    }
  }
}
