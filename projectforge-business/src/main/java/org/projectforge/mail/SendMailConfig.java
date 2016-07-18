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

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.configuration.ConfigurationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.micromata.genome.logging.GLog;
import de.micromata.genome.logging.GenomeLogCategory;
import de.micromata.genome.logging.ValMessageLogAttribute;
import de.micromata.genome.util.runtime.config.MailSessionLocalSettingsConfigModel;
import de.micromata.genome.util.validation.ValContext;

@Component
public class SendMailConfig implements ConfigurationData
{

  @Value("${projectforge.sendMail.charset}")
  private String charset;

  @Autowired
  private ConfigurationService configService;

  private MailSessionLocalSettingsConfigModel mailSessionLocalSettingsConfigModel;

  private boolean mailSendConfigOk = true;

  @PostConstruct
  public void init()
  {
    if (StringUtils.isBlank(charset)) {
      this.charset = "UTF-8";
    }
  }

  private MailSessionLocalSettingsConfigModel getMailSessionLocalSettingsConfigModel()
  {
    if (mailSessionLocalSettingsConfigModel != null) {
      return mailSessionLocalSettingsConfigModel;
    }
    MailSessionLocalSettingsConfigModel cf = configService.createMailSessionLocalSettingsConfigModel();
    ValContext ctx = new ValContext();
    cf.validate(ctx);
    if (ctx.hasErrors() == true) {
      GLog.warn(GenomeLogCategory.Configuration, "Mail Sending has errors",
          new ValMessageLogAttribute(ctx.getMessages()));
      mailSendConfigOk = false;
    }
    mailSendConfigOk = cf.isEmailEnabled();
    return cf;

  }

  public boolean isMailSendConfigOk()
  {
    getMailSessionLocalSettingsConfigModel();
    return mailSendConfigOk;
  }

  /** The charset of sent messages. Default is "UTF-8". */
  public String getCharset()
  {
    return charset;
  }

  public String getDefaultSendMailAddress()
  {
    return getMailSessionLocalSettingsConfigModel().getDefaultEmailSender();
  }

}
