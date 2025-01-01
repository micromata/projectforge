/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.PhoneType;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.messaging.SmsSender;
import org.projectforge.sms.SmsSenderConfig;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractStandardFormPage;

import java.util.Date;

public class SendSmsPage extends AbstractStandardFormPage {
  private static final long serialVersionUID = -1677859643101866297L;

  public final static String PARAMETER_KEY_ADDRESS_ID = "addressId";

  public final static String PARAMETER_KEY_PHONE_TYPE = "phoneType";

  public final static String PARAMETER_KEY_NUMBER = "number";

  protected static final String[] BOOKMARKABLE_SELECT_PROPERTIES = new String[]{PARAMETER_KEY_ADDRESS_ID + "|address",
          PARAMETER_KEY_PHONE_TYPE + "|phone", PARAMETER_KEY_NUMBER + "|no"};

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SendSmsPage.class);

  private AddressDO address;

  private SendSmsForm form;

  String result;

  @SuppressWarnings("serial")
  public SendSmsPage(final PageParameters parameters) {
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
    form.add(new Label("result", new PropertyModel<String>(this, "result")) {
      @Override
      public boolean isVisible() {
        return StringUtils.isNotBlank(result);
      }
    });
  }

  public Integer getAddressId() {
    return null;
  }

  public void setAddressId(final Integer addressId) {
    if (addressId != null) {
      address = WicketSupport.get(AddressDao.class).find(addressId);
    }
  }

  public String getNumber() {
    final String number = getData().getPhoneNumber();
    final int pos = number != null ? number.indexOf(':') : -1;
    if (pos > 0) {
      return number.substring(0, pos);
    }
    return number;
  }

  public void setNumber(final String number) {
    if (StringUtils.isNotBlank(number) == true) {
      getData().setPhoneNumber(number);
    }
  }

  public String getPhoneType() {
    return null;
  }

  public void setPhoneType(final String phoneType) {
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

  protected void send() {
    SmsSenderConfig smsSenderConfig = WicketSupport.get(SmsSenderConfig.class);
    final String number = NumberHelper.extractPhonenumber(getData().getPhoneNumber(),
            Configuration.getInstance().getStringValue(ConfigurationParam.DEFAULT_COUNTRY_PHONE_PREFIX));
    if (!smsSenderConfig.isSmsConfigured()) {
      log.error("Servlet url for sending sms not configured. SMS not supported.");
      return;
    }
    result = "";
    SmsSender smsSender = new SmsSender(smsSenderConfig);
    SmsSender.HttpResponseCode responseCode = smsSender.send(number, getData().getMessage());
    String errorKey = smsSender.getErrorMessage(responseCode);
    if (errorKey == null) {
      result = getLocalizedMessage("address.sendSms.sendMessage.result.successful", number,
          DateTimeFormatter.instance()
              .getFormattedDateTime(new Date()));
    }
    if (errorKey != null) {
      form.addError(errorKey);
    }
  }

  @Override
  protected void onAfterRender() {
    super.onAfterRender();
    result = null;
  }

  private SendSmsData getData() {
    return form.data;
  }

  /**
   * @return This page as link with the page parameters of this page.
   */
  @Override
  public String getPageAsLink() {
    return getPageAsLink(new PageParameters());
  }

  @Override
  protected String[] getBookmarkableInitialProperties() {
    return BOOKMARKABLE_SELECT_PROPERTIES;
  }

  @Override
  protected String getTitle() {
    return getString("address.sendSms.title");
  }
}
