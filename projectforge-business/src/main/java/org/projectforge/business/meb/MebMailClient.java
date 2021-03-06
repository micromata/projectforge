/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.meb;

import de.micromata.genome.logging.GLog;
import de.micromata.genome.logging.GenomeLogCategory;
import de.micromata.genome.logging.ValMessageLogAttribute;
import de.micromata.genome.util.validation.ValContext;
import de.micromata.mgc.email.MailReceiverLocalSettingsConfigModel;
import org.projectforge.framework.time.PFDay;
import org.projectforge.mail.Mail;
import org.projectforge.mail.MailAccount;
import org.projectforge.mail.MailFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import javax.mail.Flags;
import javax.mail.MessagingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

/**
 * Gets the messages from a mail account and assigns them to the MEB user's inboxes.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Controller
public class MebMailClient
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MebMailClient.class);

  @Autowired
  private ApplicationContext applicationContext;

  private MebDao mebDao;

  @Value("${genome.email.receive.enabled}")
  private boolean enabled;

  @Value("${genome.email.receive.host}")
  private String host;

  @Value("${genome.email.receive.protocol}")
  private String protocol;

  @Value("${genome.email.receive.port}")
  private String port;

  @Value("${genome.email.receive.user}")
  private String user;

  @Value("${genome.email.receive.defaultFolder}")
  private String defaultFolder;

  @Value("${genome.email.receive.password}")
  private String password;

  @Value("${genome.email.receive.auth}")
  private boolean auth;

  @Value("${genome.email.receive.starttls.enable}")
  private boolean starttlsEnable;

  @Value("${genome.email.receive.enableSelfSignedCerts}")
  private boolean enableSelfSignedCerts;

  @Value("${genome.email.receive.socketFactory.port}")
  private String socketFactoryPort;

  @Value("${genome.email.receive.socketFactory.class}")
  private String socketFactoryClass;

  @Value("${genome.email.receive.auth.plain.disable}")
  private boolean authPlainDisable;

  @Value("${genome.email.receive.debug}")
  private boolean debug;

  private boolean mgcMailAccountDisabled = false;

  private de.micromata.mgc.email.MailAccount mgcMailAccount;

  protected de.micromata.mgc.email.MailAccount getMailAccount()
  {
    if (mgcMailAccount != null) {
      return mgcMailAccount;
    }
    if (false) {
      return null;
    }
    MailReceiverLocalSettingsConfigModel cfg = createMailReceiverLocalSettingsConfigModel();
    if (!cfg.isEnabled()) {
      mgcMailAccountDisabled = true;
      return null;
    }
    ValContext ctx = new ValContext();
    cfg.validate(ctx);
    if (ctx.hasErrors()) {
      GLog.warn(GenomeLogCategory.Configuration, "Mail Receiver account not valid",
          new ValMessageLogAttribute(ctx.getMessages()));
      return null;
    }
    return mgcMailAccount = new de.micromata.mgc.email.MailAccount(cfg);
  }

  private MailReceiverLocalSettingsConfigModel createMailReceiverLocalSettingsConfigModel()
  {
    return new MailReceiverLocalSettingsConfigModel()
        .setEnabled(String.valueOf(enabled))
        .setHost(host)
        .setPort(String.valueOf(port))
        .setProtocol(protocol)
        .setUser(user)
        .setPassword(password)
        .setDefaultFolder(defaultFolder)
        .setAuth(String.valueOf(auth))
        .setEnableTLS(String.valueOf(starttlsEnable))
        .setEnableSelfSignedCerts(String.valueOf(enableSelfSignedCerts))
        .setSocketFactoryPort(String.valueOf(socketFactoryPort))
        .setSocketFactoryClass(socketFactoryClass)
        .setAuthPlainDisable(String.valueOf(authPlainDisable))
        .setDebug(String.valueOf(debug));
  }

  public boolean isMailAccountAvailable()
  {
    return getMailAccount() != null;
  }

  /**
   * @param onlyRecentMails If true then only unseen mail will be got from the mail server and afterwards they will be
   *                        set as seen.
   * @return Number of new imported messages.
   */
  public synchronized int getNewMessages(final boolean onlyRecentMails, final boolean markRecentMailsAsSeen)
  {

    final MailFilter filter = new MailFilter();
    if (onlyRecentMails) {
      filter.setOnlyRecent(true);
    }
    de.micromata.mgc.email.MailAccount mcacc = getMailAccount();
    if (mcacc == null) {
      return 0;
    }

    Integer res = mcacc.runWithFolder(markRecentMailsAsSeen, () -> {
      MailAccount mailAccount = new MailAccount(mcacc.getStore(), mcacc.getFolder());
      // If mark messages as seen is set then open mbox read-write.
      //      mailAccount.connect("INBOX", markRecentMailsAsSeen);

      final Mail[] mails = mailAccount.getMails(filter);
      if (mails == null) {
        return 0;
      }
      int counter = 0;
      for (final Mail mail : mails) {
        final MebEntryDO entry = new MebEntryDO();
        PFDay mailDate = PFDay.from(mail.getDate());
        entry.setDate(mailDate.getLocalDate());
        final String content = mail.getContent();
        final BufferedReader reader = new BufferedReader(new StringReader(content.trim()));
        try {
          StringBuffer buf = null;
          while (reader.ready()) {
            final String line = reader.readLine();
            if (line == null) {
              break;
            }
            if (line.startsWith("date=")) {
              if (line.length() > 5) {
                final String dateString = line.substring(5);
                PFDay date = PFDay.from(MebDao.parseDate(dateString));
                entry.setDate(date.getLocalDate());
              }
            } else if (line.startsWith("sender=")) {
              if (line.length() > 7) {
                final String sender = line.substring(7);
                entry.setSender(sender);
              }
            } else if (line.startsWith("msg=")) {
              if (line.length() > 4) {
                final String msg = line.substring(4);
                buf = new StringBuffer();
                buf.append(msg);
              }
            } else if (buf != null) {
              buf.append(line);
            } else {
              entry.setSender(line); // First row is the sender.
              buf = new StringBuffer(); // The message follows.
            }
          }
          if (buf != null) {
            int min = Math.min(4000, buf.length());
            if (min != buf.length()) {
              log.warn("MEB Mail is shortened for saving in db");
            }
            entry.setMessage(buf.substring(0, min).toString().trim());
          }
        } catch (IOException ex) {
          log.error("Exception encountered " + ex, ex);
        }
        if (mebDao.checkAndAddEntry(entry, "MAIL")) {
          counter++;
        }
        if (markRecentMailsAsSeen) {
          try {
            mail.getMessage().setFlag(Flags.Flag.SEEN, true);
            //mail.getMessage().saveChanges();
          } catch (MessagingException ex) {
            log.error("Exception encountered while setting message flag SEEN as true: " + ex, ex);
          }
        }
        // log.info(mail);
      }
      return counter;

    });
    if (res != null) {
      return res;
    }
    return 0;
  }

  private MebDao getMebDao()
  {
    if (mebDao == null) {
      mebDao = applicationContext.getBean(MebDao.class);
    }
    return mebDao;
  }
}
