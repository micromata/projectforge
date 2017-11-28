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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.api.ProjectForgeException;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.i18n.InternalErrorException;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.utils.ExceptionHelper;
import org.projectforge.web.SendFeedback;
import org.projectforge.web.SendFeedbackData;

/**
 * Standard error page should be shown in production mode.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ErrorPage extends AbstractSecuredPage
{
  private static final long serialVersionUID = -637809894879133209L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ErrorPage.class);

  public static final String ONLY4NAMESPACE = "org.projectforge";

  private String title;

  String errorMessage, messageNumber;

  @SpringBean
  private SendFeedback sendFeedback;

  @SpringBean
  private ConfigurationService configService;

  private final ErrorForm form;

  private boolean showFeedback;

  /**
   * Get internationalized message inclusive the message params if exists.
   *
   * @param securedPage Needed for localization.
   * @param exception
   * @param doLog       If true, then a log entry with level INFO will be produced.
   * @return
   */
  public static String getExceptionMessage(final AbstractSecuredBasePage securedPage,
      final ProjectForgeException exception,
      final boolean doLog)
  {
    // Feedbackpanel!
    if (exception instanceof UserException) {
      final UserException ex = (UserException) exception;
      if (doLog == true) {
        log.info(ex.toString() + ExceptionHelper.getFilteredStackTrace(ex, ONLY4NAMESPACE));
      }
      return securedPage.translateParams(ex.getI18nKey(), ex.getMsgParams(), ex.getParams());
    } else if (exception instanceof InternalErrorException) {
      final InternalErrorException ex = (InternalErrorException) exception;
      if (doLog == true) {
        log.info(ex.toString() + ExceptionHelper.getFilteredStackTrace(ex, ONLY4NAMESPACE));
      }
      return securedPage.translateParams(ex.getI18nKey(), ex.getMsgParams(), ex.getParams());
    } else if (exception instanceof AccessException) {
      final AccessException ex = (AccessException) exception;
      if (doLog == true) {
        log.info(ex.toString() + ExceptionHelper.getFilteredStackTrace(ex, ONLY4NAMESPACE));
      }
      if (ex.getParams() != null) {
        return securedPage.getLocalizedMessage(ex.getI18nKey(), ex.getParams());
      } else {
        return securedPage.translateParams(ex.getI18nKey(), ex.getMessageArgs(), ex.getParams());
      }
    }
    throw new UnsupportedOperationException("For developer: Please add unknown ProjectForgeException here!", exception);
  }

  public ErrorPage()
  {
    this(null);
  }

  public ErrorPage(final Throwable throwable)
  {
    super(null);
    errorMessage = getString("errorpage.unknownError");
    messageNumber = null;
    Throwable rootCause = null;
    showFeedback = true;
    if (throwable != null) {
      rootCause = ExceptionHelper.getRootCause(throwable);
      if (rootCause instanceof ProjectForgeException) {
        errorMessage = getExceptionMessage(this, (ProjectForgeException) rootCause, true);
      } else if (throwable instanceof ServletException) {
        messageNumber = String.valueOf(System.currentTimeMillis());
        log.error("Message #" + messageNumber + ": " + throwable.getMessage(), throwable);
        if (rootCause != null) {
          log.error("Message #" + messageNumber + " rootCause: " + rootCause.getMessage(), rootCause);
        }
        errorMessage = getLocalizedMessage(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM, messageNumber);
      } else if (throwable instanceof PageExpiredException) {
        log.info("Page expired (session time out).");
        showFeedback = false;
        errorMessage = getString("message.wicket.pageExpired");
        title = getString("message.title");
      } else {
        messageNumber = String.valueOf(System.currentTimeMillis());
        log.error("Message #" + messageNumber + ": " + throwable.getMessage(), throwable);
        errorMessage = getLocalizedMessage(UserException.I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM, messageNumber);
      }
    }
    form = new ErrorForm(this);
    String receiver = null;
    try {
      receiver = Configuration.getInstance().getStringValue(ConfigurationParam.FEEDBACK_E_MAIL);
    } catch (final Exception ex) {
      log.error("Exception occured while trying to get configured e-mail for feedback: " + ex.getMessage(), ex);
    }
    form.data.setReceiver(receiver);
    form.data.setMessageNumber(messageNumber);
    form.data.setMessage(throwable != null ? throwable.getMessage() : "");
    form.data.setStackTrace(throwable != null ? ExceptionHelper.printStackTrace(throwable) : "");
    form.data.setSender(ThreadLocalUserContext.getUser().getFullname());
    final String subject = "ProjectForge-Error #" + form.data.getMessageNumber() + " from " + form.data.getSender();
    form.data.setSubject(subject);
    if (rootCause != null) {
      form.data.setRootCause(rootCause.getMessage());
      form.data.setRootCauseStackTrace(ExceptionHelper.printStackTrace(rootCause));
    }
    final boolean visible = showFeedback == true && messageNumber != null && StringUtils.isNotBlank(receiver);
    body.add(form);
    if (visible == true) {
      form.init();
    }
    form.setVisible(visible);
    final Label errorMessageLabel = new Label("errorMessage", errorMessage);
    body.add(errorMessageLabel.setVisible(errorMessage != null));
    final FeedbackPanel feedbackPanel = new FeedbackPanel("feedback");
    feedbackPanel.setOutputMarkupId(true);
    body.add(feedbackPanel);

    sendProactiveMessageToSupportTeam();
  }

  private void sendProactiveMessageToSupportTeam()
  {
    if (StringUtils.isBlank(configService.getPfSupportMailAddress()) == Boolean.FALSE
        && configService.getSendMailConfiguration() != null) {
      log.info("Sending proactive mail to support.");
      Calendar cal = Calendar.getInstance();
      Date date = cal.getTime();
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm");
      String dateString = formatter.format(date);
      SendFeedbackData errorData = new SendFeedbackData();
      errorData.setSender(configService.getSendMailConfiguration().getDefaultSendMailAddress());
      errorData.setReceiver(configService.getPfSupportMailAddress());
      errorData.setSubject("Error occured: #" + form.data.getMessageNumber() + " on " + configService.getDomain());
      errorData.setDescription("Error occured at: " + dateString + "(" + cal.getTimeZone().getID()
          + ") with number: #" + form.data.getMessageNumber()
          + " from user: " + form.data.getSender() + " \n "
          + "Exception stack trace: \n" +
          form.data.getRootCauseStackTrace() + "\n");
      sendFeedback.send(errorData);
    } else {
      log.info("No messaging for proactive support configured.");
    }
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

  @Override
  protected String getTitle()
  {
    return title != null ? title : getString("errorpage.title");
  }

  /**
   * @see org.apache.wicket.Component#isVersioned()
   */
  @Override
  public boolean isVersioned()
  {
    return false;
  }

  /**
   * @see org.apache.wicket.Page#isErrorPage()
   */
  @Override
  public boolean isErrorPage()
  {
    return true;
  }
}
