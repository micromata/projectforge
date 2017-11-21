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

package org.projectforge.web.address;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.PhoneType;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.URLHelper;
import org.projectforge.web.wicket.AbstractStandardFormPage;

public class SendSmsPage extends AbstractStandardFormPage
{
  private static final long serialVersionUID = -1677859643101866297L;

  public final static String PARAMETER_KEY_ADDRESS_ID = "addressId";

  public final static String PARAMETER_KEY_PHONE_TYPE = "phoneType";

  public final static String PARAMETER_KEY_NUMBER = "number";

  protected static final String[] BOOKMARKABLE_SELECT_PROPERTIES = new String[] { PARAMETER_KEY_ADDRESS_ID + "|address",
      PARAMETER_KEY_PHONE_TYPE + "|phone", PARAMETER_KEY_NUMBER + "|no" };

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SendSmsPage.class);

  @SpringBean
  private AddressDao addressDao;

  @SpringBean
  private ConfigurationService configurationService;

  private AddressDO address;

  private SendSmsForm form;

  String result;

  @SuppressWarnings("serial")
  public SendSmsPage(final PageParameters parameters)
  {
    super(parameters);
    form = new SendSmsForm(this);
    body.add(form);
    evaluateInitialPageParameters(getPageParameters());
    form.init();
    final String javaScript = "function showSendQuestionDialog() {\n  return window.confirm('"
        + getString("address.sendSms.sendMessageQuestion")
        + "');\n}\n"
        + " $(document).ready(function() {\n"
        + "    var onEditCallback = function(remaining) {\n"
        + "        $('#charsRemaining').text(remaining + ' "
        + getString("charactersLeft")
        + "');\n"
        + "    }\n"
        // TODO: Doesn't work with autocompletion!
        //        + "    $('textarea[maxlength]').limitMaxlength({\n"
        //        + "        onEdit: onEditCallback,\n"
        //        + "    });\n"
        + " });\n";

    body.add(new Label("javascript", javaScript).setEscapeModelStrings(false));
    form.add(new Label("result", new PropertyModel<String>(this, "result"))
    {
      @Override
      public boolean isVisible()
      {
        return StringUtils.isNotBlank(result);
      }
    });
  }

  public Integer getAddressId()
  {
    return null;
  }

  public void setAddressId(final Integer addressId)
  {
    if (addressId != null) {
      address = addressDao.getById(addressId);
    }
  }

  public String getNumber()
  {
    final String number = getData().getPhoneNumber();
    final int pos = number != null ? number.indexOf(':') : -1;
    if (pos > 0) {
      return number.substring(0, pos);
    }
    return number;
  }

  public void setNumber(final String number)
  {
    if (StringUtils.isNotBlank(number) == true) {
      getData().setPhoneNumber(number);
    }
  }

  public String getPhoneType()
  {
    return null;
  }

  public void setPhoneType(final String phoneType)
  {
    PhoneType type = null;
    try {
      type = PhoneType.valueOf(phoneType);
    } catch (final IllegalArgumentException ex) {
    }
    String number = null;
    if (type == PhoneType.MOBILE) {
      number = address.getMobilePhone();
    } else if (type == PhoneType.PRIVATE_MOBILE) {
      number = address.getPrivateMobilePhone();
    }
    if (number != null) {
      getData().setPhoneNumber(
          SendSmsForm.getPhoneNumberAndPerson(address, number,
              Configuration.getInstance().getStringValue(ConfigurationParam.DEFAULT_COUNTRY_PHONE_PREFIX)));
    }
  }

  protected void send()
  {
    final String number = NumberHelper.extractPhonenumber(getData().getPhoneNumber(),
        Configuration.getInstance().getStringValue(ConfigurationParam.DEFAULT_COUNTRY_PHONE_PREFIX));
    if (StringUtils.isBlank(configurationService.getSmsUrl()) == true) {
      log.error("Servlet url for sending sms not configured. SMS not supported.");
      return;
    }
    log.info("User sends message to destination number: '" + StringHelper.hideStringEnding(number, 'x', 3));
    final HttpClient client = new HttpClient();
    String url = configurationService.getSmsUrl();
    url = StringUtils.replaceOnce(url, "#number", number);
    url = StringUtils.replaceOnce(url, "#message", URLHelper.encode(getData().getMessage()));
    final GetMethod method = new GetMethod(url);
    String errorKey = null;
    result = "";
    try {
      client.executeMethod(method);
      final String response = method.getResponseBodyAsString();
      if (response == null) {
        errorKey = "address.sendSms.sendMessage.result.unknownError";
      } else if (response.startsWith("0") == true) {
        result = getLocalizedMessage("address.sendSms.sendMessage.result.successful", number,
            DateTimeFormatter.instance()
                .getFormattedDateTime(new Date()));
      } else if (response.startsWith("1") == true) {
        errorKey = "address.sendSms.sendMessage.result.messageMissed";
      } else if (response.startsWith("2") == true) {
        errorKey = "address.sendSms.sendMessage.result.wrongOrMissingNumber";
      } else if (response.startsWith("3") == true) {
        errorKey = "address.sendSms.sendMessage.result.messageToLarge";
      } else {
        errorKey = "address.sendSms.sendMessage.result.unknownError";
      }
    } catch (final HttpException ex) {
      errorKey = "Call failed. Please contact administrator.";
      log.fatal(errorKey + ": " + configurationService.getSmsUrl()
          + StringHelper.hideStringEnding(String.valueOf(number), 'x', 3));
      throw new RuntimeException(ex);
    } catch (final IOException ex) {
      errorKey = "Call failed. Please contact administrator.";
      log.fatal(errorKey + ": " + configurationService.getSmsUrl()
          + StringHelper.hideStringEnding(String.valueOf(number), 'x', 3));
      throw new RuntimeException(ex);
    }
    if (errorKey != null) {
      form.addError(errorKey);
    }
  }

  @Override
  protected void onAfterRender()
  {
    super.onAfterRender();
    result = null;
  }

  private SendSmsData getData()
  {
    return form.data;
  }

  /**
   * @return This page as link with the page parameters of this page.
   */
  @Override
  public String getPageAsLink()
  {
    return getPageAsLink(new PageParameters());
  }

  @Override
  protected String[] getBookmarkableInitialProperties()
  {
    return BOOKMARKABLE_SELECT_PROPERTIES;
  }

  @Override
  protected String getTitle()
  {
    return getString("address.sendSms.title");
  }
}
