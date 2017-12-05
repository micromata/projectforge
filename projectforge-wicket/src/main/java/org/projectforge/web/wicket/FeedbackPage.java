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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.SendFeedback;
import org.projectforge.web.pacman.PacmanViewPage;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@SuppressWarnings("serial")
public class FeedbackPage extends AbstractStandardFormPage
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeedbackPage.class);

  @SpringBean
  private SendFeedback sendFeedback;

  private final FeedbackForm form;

  /**
   * @see org.projectforge.web.wicket.AbstractSecuredBasePage#isAccess4restrictedUsersAllowed()
   */
  @Override
  public boolean isAccess4restrictedUsersAllowed()
  {
    return true;
  }

  public FeedbackPage(final PageParameters parameters)
  {
    super(null);
    form = new FeedbackForm(this);
    final String receiver = Configuration.getInstance().getStringValue(ConfigurationParam.FEEDBACK_E_MAIL);
    body.add(form);
    form.data.setReceiver(receiver);
    form.data.setSender(ThreadLocalUserContext.getUser().getFullname());
    form.data.setSubject("Feedback from " + form.data.getSender());
    form.init();
  }

  void cancel()
  {
    setResponsePage(WicketUtils.getDefaultPage());
  }

  void sendFeedback()
  {
    log.info("Send feedback.");
    boolean result = false;
    try {
      result = sendFeedback.send(form.data);
    } catch (final Throwable ex) {
      log.error(ex.getMessage(), ex);
      result = false;
    }
    final MessagePage messagePage = new MessagePage(new PageParameters());
    if (result == true) {
      messagePage.setMessage(getString("feedback.mailSendSuccessful"));
    } else {
      messagePage.setMessage(getString("mail.error.exception"));
      messagePage.setWarning(true);
    }
    setResponsePage(messagePage);
  }

  void playPacman()
  {
    setResponsePage(new PacmanViewPage(new PageParameters()));
  }

  @Override
  protected String getTitle()
  {
    return getString("feedback.send.title");
  }

  /**
   * @see org.apache.wicket.Component#isVersioned()
   */
  @Override
  public boolean isVersioned()
  {
    return false;
  }
}
