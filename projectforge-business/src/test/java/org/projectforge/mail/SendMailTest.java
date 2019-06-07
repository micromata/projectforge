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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.i18n.InternalErrorException;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import javax.mail.Transport;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Transport.class })
@PowerMockIgnore({ "javax.management.*", "javax.xml.parsers.*", "com.sun.org.apache.xerces.internal.jaxp.*", "ch.qos.logback.*", "org.slf4j.*" })
public class SendMailTest
{
  @Mock
  private MailSessionLocalSettingsConfigModel mailSessionLocalSettingsConfigModel;

  @Mock
  private SendMailConfig sendMailConfiguration;

  @Mock
  private ConfigurationService configurationService;

  @Mock
  private MailAttachment attachment;

  @InjectMocks
  private SendMail sendMail = new SendMail();

  @Before
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);
    mockStatic(Transport.class);
  }

  @Test
  public void sendMailNullTest()
  {
    final Mail message = null;
    final String icalContent = null;
    final Collection<? extends MailAttachment> attachments = null;
    assertFalse(sendMail.send(message, icalContent, attachments));
  }

  @Test
  public void sendMailNoToAddressTest()
  {
    final Mail message = new Mail();
    final String icalContent = null;
    final Collection<? extends MailAttachment> attachments = null;
    try {
      sendMail.send(message, icalContent, attachments);
      assertTrue(false);
    } catch (UserException e) {
      //OK
    }
  }

  @Test
  public void sendMailUserNullTest()
  {
    final Mail message = new Mail();
    PFUserDO toUser = null;
    message.setTo(toUser);
    final String icalContent = null;
    final Collection<? extends MailAttachment> attachments = null;
    try {
      sendMail.send(message, icalContent, attachments);
      assertTrue(false);
    } catch (UserException e) {
      //OK
    }
    toUser = new PFUserDO();
    message.setTo(toUser);
    try {
      sendMail.send(message, icalContent, attachments);
      assertTrue(false);
    } catch (UserException e) {
      //OK
    }
  }

  @Test
  public void sendMailWithoutConfigTest()
  {
    final Mail message = new Mail();
    final PFUserDO toUser = new PFUserDO();
    toUser.setEmail("devnull@test.de");
    message.setTo(toUser);
    final String icalContent = null;
    final Collection<? extends MailAttachment> attachments = null;
    assertFalse(sendMail.send(message, icalContent, attachments));
  }

  @Test
  public void sendMailEmptyConfigTest()
  {
    final MailSessionLocalSettingsConfigModel model = new MailSessionLocalSettingsConfigModel();
    when(configurationService.createMailSessionLocalSettingsConfigModel()).thenReturn(model);
    final Mail message = new Mail();
    final PFUserDO toUser = new PFUserDO();
    toUser.setEmail("devnull@test.de");
    message.setTo(toUser);
    final String icalContent = null;
    final Collection<? extends MailAttachment> attachments = null;
    assertFalse(sendMail.send(message, icalContent, attachments));
  }

  @Test
  public void sendMailEmailDisabledTest()
  {
    when(configurationService.createMailSessionLocalSettingsConfigModel()).thenReturn(mailSessionLocalSettingsConfigModel);
    final Mail message = new Mail();
    final PFUserDO toUser = new PFUserDO();
    toUser.setEmail("devnull@test.de");
    message.setTo(toUser);
    final String icalContent = null;
    final Collection<? extends MailAttachment> attachments = null;
    assertFalse(sendMail.send(message, icalContent, attachments));
  }

  @Test
  public void sendMailAsyncTest()
  {
    when(mailSessionLocalSettingsConfigModel.isEmailEnabled()).thenReturn(true);
    when(configurationService.createMailSessionLocalSettingsConfigModel()).thenReturn(mailSessionLocalSettingsConfigModel);
    final Mail message = new Mail();
    final PFUserDO toUser = new PFUserDO();
    toUser.setEmail("devnull@test.de");
    message.setTo(toUser);
    final String icalContent = null;
    final Collection<? extends MailAttachment> attachments = null;
    assertTrue(sendMail.send(message, icalContent, attachments));
  }

  @Test
  public void sendMailSyncNullContentTest()
  {
    when(mailSessionLocalSettingsConfigModel.isEmailEnabled()).thenReturn(true);
    when(mailSessionLocalSettingsConfigModel.getStandardEmailSender()).thenReturn("sender@test.de");
    when(configurationService.createMailSessionLocalSettingsConfigModel()).thenReturn(mailSessionLocalSettingsConfigModel);
    when(configurationService.getSendMailConfiguration()).thenReturn(sendMailConfiguration);
    when(sendMailConfiguration.getCharset()).thenReturn("UTF-8");
    final Mail message = new Mail();
    final PFUserDO toUser = new PFUserDO();
    toUser.setEmail("devnull@test.de");
    message.setTo(toUser);
    final String icalContent = null;
    final Collection<? extends MailAttachment> attachments = null;
    try {
      sendMail.send(message, icalContent, attachments, false);
      assertTrue(false);
    } catch (InternalErrorException e) {
      //OK
      //java.lang.NullPointerException: null
      //at com.sun.mail.handlers.text_plain.writeTo(text_plain.java:152)
    }
  }

  @Test
  public void sendMailSyncTest()
  {
    when(mailSessionLocalSettingsConfigModel.isEmailEnabled()).thenReturn(true);
    when(mailSessionLocalSettingsConfigModel.getStandardEmailSender()).thenReturn("sender@test.de");
    when(configurationService.createMailSessionLocalSettingsConfigModel()).thenReturn(mailSessionLocalSettingsConfigModel);
    when(configurationService.getSendMailConfiguration()).thenReturn(sendMailConfiguration);
    when(sendMailConfiguration.getCharset()).thenReturn("UTF-8");
    final Mail message = new Mail();
    final PFUserDO toUser = new PFUserDO();
    toUser.setEmail("devnull@test.de");
    message.setTo(toUser);
    message.setContent("");
    final String icalContent = null;
    final Collection<? extends MailAttachment> attachments = null;
    sendMail.send(message, icalContent, attachments, false);
  }

  @Test
  public void sendMailSyncWithIcalTest()
  {
    when(mailSessionLocalSettingsConfigModel.isEmailEnabled()).thenReturn(true);
    when(mailSessionLocalSettingsConfigModel.getStandardEmailSender()).thenReturn("sender@test.de");
    when(configurationService.createMailSessionLocalSettingsConfigModel()).thenReturn(mailSessionLocalSettingsConfigModel);
    when(configurationService.getSendMailConfiguration()).thenReturn(sendMailConfiguration);
    when(sendMailConfiguration.getCharset()).thenReturn("UTF-8");
    final Mail message = new Mail();
    final PFUserDO toUser = new PFUserDO();
    toUser.setEmail("devnull@test.de");
    message.setTo(toUser);
    message.setContent("");
    final String icalContent = "ABC";
    final Collection<? extends MailAttachment> attachments = null;
    sendMail.send(message, icalContent, attachments, false);
  }

  @Test
  public void sendMailSyncWithAttachmentTest()
  {
    when(mailSessionLocalSettingsConfigModel.isEmailEnabled()).thenReturn(true);
    when(mailSessionLocalSettingsConfigModel.getStandardEmailSender()).thenReturn("sender@test.de");
    when(configurationService.createMailSessionLocalSettingsConfigModel()).thenReturn(mailSessionLocalSettingsConfigModel);
    when(configurationService.getSendMailConfiguration()).thenReturn(sendMailConfiguration);
    when(sendMailConfiguration.getCharset()).thenReturn("UTF-8");
    final Mail message = new Mail();
    final PFUserDO toUser = new PFUserDO();
    toUser.setEmail("devnull@test.de");
    message.setTo(toUser);
    message.setContent("");
    final String icalContent = null;
    final Collection<MailAttachment> attachments = new ArrayList<>();
    when(attachment.getFilename()).thenReturn("test.abc");
    attachments.add(attachment);
    sendMail.send(message, icalContent, attachments, false);
  }

  @Test
  public void sendMailSyncWithIcalAndAttachmentTest()
  {
    when(mailSessionLocalSettingsConfigModel.isEmailEnabled()).thenReturn(true);
    when(mailSessionLocalSettingsConfigModel.getStandardEmailSender()).thenReturn("sender@test.de");
    when(configurationService.createMailSessionLocalSettingsConfigModel()).thenReturn(mailSessionLocalSettingsConfigModel);
    when(configurationService.getSendMailConfiguration()).thenReturn(sendMailConfiguration);
    when(sendMailConfiguration.getCharset()).thenReturn("UTF-8");
    final Mail message = new Mail();
    final PFUserDO toUser = new PFUserDO();
    toUser.setEmail("devnull@test.de");
    message.setTo(toUser);
    message.setContent("");
    final String icalContent = "ABC";
    final Collection<MailAttachment> attachments = new ArrayList<>();
    when(attachment.getFilename()).thenReturn("test.abc");
    sendMail.send(message, icalContent, attachments, false);
  }

}
