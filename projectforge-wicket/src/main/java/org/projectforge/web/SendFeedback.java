/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.mail.Mail;
import org.projectforge.mail.SendMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SendFeedback {
  @Autowired
  private SendMail sendMail;

  /**
   * Sender will be set automatically to logged in context user, if not given.
   *
   * @param data
   */
  public boolean send(final SendFeedbackData data) {
    if (data.getSender() == null) {
      data.setSender(ThreadLocalUserContext.getUser().getFullname());
    }
    final Map<String, Object> params = new HashMap<String, Object>();
    params.put("data", data);
    final Mail msg = new Mail();
    msg.addTo(data.getReceiver());
    msg.setProjectForgeSubject(data.getSubject());
    params.put("subject", data.getSubject());
    final String content = sendMail.renderGroovyTemplate(msg,
            "mail/feedback.txt",
            params,
            I18nHelper.getLocalizedMessage("administration.configuration.param.feedbackEMail.label"),
            ThreadLocalUserContext.getUser());
    msg.setContent(content);
    msg.setContentType(Mail.CONTENTTYPE_TEXT);
    return sendMail.send(msg, null, null);
  }
}
