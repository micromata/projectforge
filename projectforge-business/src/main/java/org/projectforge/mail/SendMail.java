/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.genome.util.runtime.config.MailSessionLocalSettingsConfigModel;
import de.micromata.genome.util.validation.ValContext;
import de.micromata.genome.util.validation.ValMessage;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.scripting.GroovyEngine;
import org.projectforge.framework.i18n.InternalErrorException;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Helper class for creating and transporting E-Mails. Groovy script is use-able for e-mail template mechanism.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class SendMail
{
  private static final String STANDARD_SUBJECT_PREFIX = "[ProjectForge] ";

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SendMail.class);

  @Autowired
  private ConfigurationService configurationService;

  private Random random = new Random();

  /**
   * Get the ProjectForge standard subject: "[ProjectForge] ..."
   *
   * @param subject
   */
  public static String getProjectForgeSubject(final String subject)
  {
    return STANDARD_SUBJECT_PREFIX + subject;
  }

  /**
   * @param composedMessage the message to send
   * @param icalContent the ical content to add
   * @param attachments other attachments to add
   * @return true for successful sending, otherwise an exception will be thrown.
   * @throws UserException          if to address is not given.
   * @throws InternalErrorException due to technical failures.
   */
  public boolean send(final Mail composedMessage, final String icalContent,
      final Collection<? extends MailAttachment> attachments)
  {
    return send(composedMessage, icalContent, attachments, true);
  }

  public boolean send(final Mail composedMessage, final String icalContent,
      final Collection<? extends MailAttachment> attachments, boolean async)
  {
    if (composedMessage == null) {
      log.error("No message object of type org.projectforge.mail.Mail given. E-Mail not sent.");
      return false;
    }
    final List<InternetAddress> to = composedMessage.getTo();
    if (to == null || to.size() == 0) {
      log.error("No to address given. Sending of mail cancelled: " + composedMessage.toString());
      throw new UserException("mail.error.missingToAddress");
    }
    MailSessionLocalSettingsConfigModel cf = configurationService.createMailSessionLocalSettingsConfigModel();
    if (cf == null || !cf.isEmailEnabled()) {
      log.error("No e-mail host configured. E-Mail not sent: " + composedMessage.toString());
      return false;
    }

    if (async) {
      CompletableFuture.runAsync(() -> sendIt(composedMessage, icalContent, attachments));
    } else {
      sendIt(composedMessage, icalContent, attachments);
    }

    return true;
  }

  private Session getSession()
  {
    MailSessionLocalSettingsConfigModel cf = configurationService.createMailSessionLocalSettingsConfigModel();
    if (!cf.isEmailEnabled()) {
      log.error("Sending email is not enabled");
      throw new InternalErrorException("mail.error.exception");
    }
    ValContext ctx = new ValContext();
    cf.validate(ctx);
    if (ctx.hasErrors()) {
      log.error("SMPT configuration has validation errors");
      for (ValMessage msg : ctx.getMessages()) {
        log.error(msg.toString());
      }
      throw new InternalErrorException("mail.error.exception");
    }
    Properties addp = new Properties();
    addp.put("mail.mime.charset", "UTF-8");
    if (cf.getStandardEmailSender() != null) {
      addp.put("mail.from", cf.getStandardEmailSender());
    }
    return cf.createMailSession(addp);
  }

  private void sendIt(final Mail composedMessage, final String icalContent,
      final Collection<? extends MailAttachment> attachments)
  {
    log.info("Start sending e-mail message: " + StringUtils.join(composedMessage.getTo(), ", "));
    try {
      final Session session = getSession();
      final MimeMessage message = new MimeMessage(session);
      if (composedMessage.getFrom() != null) {
        message.setFrom(new InternetAddress(composedMessage.getFrom()));
      } else {
        message.setFrom();
      }
      message.setRecipients(Message.RecipientType.TO,
          composedMessage.getTo().toArray(new Address[composedMessage.getTo().size()]));
      final String subject = composedMessage.getSubject();
      final SendMailConfig sendMailConfig = configurationService.getSendMailConfiguration();
      message.setSubject(subject, sendMailConfig.getCharset());
      message.setSentDate(new Date());

      if (StringUtils.isBlank(icalContent) && attachments == null) {
        // create message without attachments
        if (composedMessage.getContentType() != null) {
          message.setText(composedMessage.getContent(), composedMessage.getCharset(), composedMessage.getContentType());
        } else {
          message.setText(composedMessage.getContent(), sendMailConfig.getCharset());
        }
      } else {
        // create message with attachments
        final MimeMultipart mp = createMailAttachmentContent(message, composedMessage, icalContent, attachments, sendMailConfig);
        message.setContent(mp);
      }

      message.saveChanges(); // don't forget this
      Transport.send(message);

    } catch (final Exception ex) {
      log.error("While creating and sending message: " + composedMessage.toString(), ex);
      throw new InternalErrorException("mail.error.exception");
    }
    log.info("E-Mail successfully sent: " + composedMessage.toString());
  }

  private MimeMultipart createMailAttachmentContent(MimeMessage message, final Mail composedMessage, final String icalContent,
      final Collection<? extends MailAttachment> attachments,
      final SendMailConfig sendMailConfig) throws MessagingException
  {
    // create and fill the first message part
    final MimeBodyPart mbp1 = new MimeBodyPart();
    String type = "text/";
    if (StringUtils.isNotBlank(composedMessage.getContentType())) {
      type += composedMessage.getContentType();
      type += "; charset=";
      type += composedMessage.getCharset();
    } else {
      type = "text/html; charset=";
      type += sendMailConfig.getCharset();
    }
    mbp1.setContent(composedMessage.getContent(), type);
    mbp1.setHeader("Content-Transfer-Encoding", "8bit");
    // create the Multipart and its parts to it
    final MimeMultipart mp = new MimeMultipart();
    mp.addBodyPart(mbp1);

    if (StringUtils.isNotBlank(icalContent)) {
      message.addHeaderLine("method=REQUEST");
      message.addHeaderLine("charset=UTF-8");
      message.addHeaderLine("component=VEVENT");

      final MimeBodyPart icalBodyPart = new MimeBodyPart();
      icalBodyPart.setHeader("Content-Class", "urn:content-  classes:calendarmessage");
      icalBodyPart.setHeader("Content-ID", "calendar_message");
      icalBodyPart.setDataHandler(new DataHandler(
          new ByteArrayDataSource(icalContent.getBytes(), "text/calendar")));
      final String s = Integer.toString(random.nextInt(Integer.MAX_VALUE));
      icalBodyPart.setFileName("ICal-" + s + ".ics");

      mp.addBodyPart(icalBodyPart);
    }

    if (attachments != null && !attachments.isEmpty()) {
      // create an Array of message parts for Attachments
      final MimeBodyPart mbp[] = new MimeBodyPart[attachments.size()];
      // remember you can extend this functionality with META-INF/mime.types
      // See http://docs.oracle.com/javaee/5/api/javax/activation/MimetypesFileTypeMap.html
      final MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
      int i = 0;
      for (final MailAttachment attachment : attachments) {
        // create the next message part
        mbp[i] = new MimeBodyPart();
        // only by file name
        String mimeType = mimeTypesMap.getContentType(attachment.getFilename());
        if (StringUtils.isBlank(mimeType)) {
          mimeType = "application/octet-stream";
        }
        // attach the file to the message
        final DataSource ds = new ByteArrayDataSource(attachment.getContent(), mimeType);
        mbp[i].setDataHandler(new DataHandler(ds));
        mbp[i].setFileName(attachment.getFilename());
        mp.addBodyPart(mbp[i]);
        i++;
      }
    }
    return mp;
  }

  /**
   * @param composedMessage
   * @param groovyTemplate
   * @param data
   * @see GroovyEngine#executeTemplateFile(String)
   */
  public String renderGroovyTemplate(final Mail composedMessage, final String groovyTemplate,
      final Map<String, Object> data,
      final PFUserDO recipient)
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    data.put("createdLabel", ThreadLocalUserContext.getLocalizedString("created"));
    data.put("loggedInUser", user);
    data.put("recipient", recipient);
    data.put("msg", composedMessage);
    log.debug("groovyTemplate=" + groovyTemplate);
    final GroovyEngine engine = new GroovyEngine(configurationService, data, recipient.getLocale(),
        recipient.getTimeZoneObject());
    return engine.executeTemplateFile(groovyTemplate);
  }

}
