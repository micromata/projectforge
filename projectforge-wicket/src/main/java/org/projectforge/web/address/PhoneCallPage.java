/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.sipgate.SipgateConfiguration;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.rest.AddressPagesRest;
import org.projectforge.rest.AddressViewPageRest;
import org.projectforge.rest.core.PagesResolver;
import org.projectforge.rest.sipgate.SipgateDirectCallService;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractStandardFormPage;

import java.util.Date;

public class PhoneCallPage extends AbstractStandardFormPage {
  private static final long serialVersionUID = -5040319693295350276L;

  public final static String PARAMETER_KEY_ADDRESS_ID = "addressId";

  public final static String PARAMETER_KEY_NUMBER = "number";

  protected static final String[] BOOKMARKABLE_SELECT_PROPERTIES = new String[]{PARAMETER_KEY_ADDRESS_ID + "|address",
      PARAMETER_KEY_NUMBER + "|no", "callerPage|cp"};

  private static final String SEPARATOR = " | ";

  private static final String USER_PREF_KEY_MY_RECENT_PHONE_ID = "PhoneCall:recentPhoneId";

  private static final String USER_PREF_KEY_MY_RECENT_CALLER_ID = "PhoneCall:recentCallerId";

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PhoneCallPage.class);

  private final PhoneCallForm form;

  String result;

  public PhoneCallPage(final PageParameters parameters) {
    super(parameters);
    form = new PhoneCallForm(this);
    body.add(form);
    evaluateInitialPageParameters(getPageParameters());
    form.init();
  }

  public Long getAddressId() {
    return form.address != null ? form.address.getId() : null;
  }

  public void setAddressId(final Integer addressId) {
    if (addressId != null) {
      final AddressDO address = WicketSupport.get(AddressDao.class).find(addressId);
      form.address = address;
    }
  }

  public String getNumber() {
    return form.phoneNumber;
  }

  public void setNumber(final String number) {
    if (StringUtils.isNotBlank(number) == true) {
      form.setPhoneNumber(extractPhonenumber(number));
    }
  }

  public String getCallerPage() {
    return form.callerPage;
  }

  public void setCallerPage(final String callerPage) {
    form.setCallerPage(callerPage);
  }

  /**
   * Find a phone number, search order is business, mobile, private mobile and private.
   *
   * @return Number if found, otherwise empty string.
   */
  protected String getFirstPhoneNumber() {
    if (form.address == null) {
      return "";
    }
    if (StringUtils.isNotEmpty(form.address.getBusinessPhone()) == true) {
      return form.address.getBusinessPhone();
    } else if (StringUtils.isNotEmpty(form.address.getMobilePhone()) == true) {
      return form.address.getMobilePhone();
    } else if (StringUtils.isNotEmpty(form.address.getPrivateMobilePhone()) == true) {
      return form.address.getPrivateMobilePhone();
    } else if (StringUtils.isNotEmpty(form.address.getPrivatePhone()) == true) {
      return form.address.getPrivatePhone();
    }
    return "";
  }

  /**
   * For special phone numbers: id:# or # | name.
   *
   * @return true, if the phone number was successfully processed.
   */
  private boolean processPhoneNumber() {
    AddressDao addressDao = WicketSupport.get(AddressDao.class);
    final String phoneNumber = form.getPhoneNumber();
    if (StringUtils.isNotEmpty(phoneNumber) == true) {
      if (phoneNumber.startsWith("id:") == true && phoneNumber.length() > 3) {
        final Integer id = NumberHelper.parseInteger(phoneNumber.substring(3));
        if (id != null) {
          form.setPhoneNumber("");
          final AddressDO address = addressDao.find(id);
          if (address != null) {
            form.setAddress(address);
            final String no = getFirstPhoneNumber();
            if (StringUtils.isNotEmpty(no) == true) {
              setPhoneNumber(no, true);
            }
          }
        }
        return true;
      } else if (phoneNumber.indexOf(SEPARATOR) >= 0) {
        final int pos = phoneNumber.indexOf(SEPARATOR);
        final String rest = phoneNumber.substring(pos + SEPARATOR.length());
        final int numberPos = rest.indexOf('#');
        form.setPhoneNumber(phoneNumber.substring(0, pos));
        if (numberPos > 0) {
          final Integer id = NumberHelper.parseInteger(rest.substring(numberPos + 1));
          if (id != null) {
            final AddressDO address = addressDao.find(id);
            if (address != null) {
              form.setAddress(address);
            }
          } else {
            form.setAddress(null);
          }
        } else {
          form.setAddress(null);
        }
        return true;
      }
    }
    return false;
  }

  public void setPhoneNumber(String phoneNumber, final boolean extract) {
    if (extract == true) {
      phoneNumber = extractPhonenumber(phoneNumber);
    }
    form.setPhoneNumber(phoneNumber);
  }

  String extractPhonenumber(final String number) {
    ConfigurationService configurationService = WicketSupport.get(ConfigurationService.class);
    final String result = NumberHelper.extractPhonenumber(number,
        Configuration.getInstance().getStringValue(ConfigurationParam.DEFAULT_COUNTRY_PHONE_PREFIX));
    if (StringUtils.isNotEmpty(result) == true
        && StringUtils.isNotEmpty(configurationService.getTelephoneSystemNumber()) == true
        && result.startsWith(configurationService.getTelephoneSystemNumber()) == true) {
      return result.substring(configurationService.getTelephoneSystemNumber().length());
    }
    return result;
  }

  @Override
  protected void onBeforeRender() {
    super.onBeforeRender();
    final String rawInput = form.numberTextField.getRawInput();
    if (StringUtils.isNotEmpty(rawInput) == true) {
      form.setPhoneNumber(rawInput);
    }
    processPhoneNumber();
    form.numberTextField.modelChanged();
  }

  void backToCaller() {
    String callerPage = form.getCallerPage();
    if ("addressView".equals(callerPage)) {
      throw new RedirectToUrlException(PagesResolver.getDynamicPageUrl(AddressViewPageRest.class, null, getAddressId(), true));
    } else {
      throw new RedirectToUrlException(PagesResolver.getListPageUrl(AddressPagesRest.class, null, true));
    }
  }

  void call() {
    final boolean extracted = processPhoneNumber();
    if (extracted == true) {
      return;
    }
    String number = NumberHelper.extractPhonenumber(form.getPhoneNumber());
    if (number.length() == 0 || StringUtils.containsOnly(number, "0123456789+-/() ") == false) {
      form.addError("address.phoneCall.number.invalid");
      return;
    }
    form.setPhoneNumber(extractPhonenumber(form.getPhoneNumber()));
    callNow();
  }

  private void callNow() {
    SipgateConfiguration sipgateConfiguration = WicketSupport.get(SipgateConfiguration.class);
    SipgateDirectCallService sipgateDirectCallService = WicketSupport.get(SipgateDirectCallService.class);
    if (!sipgateConfiguration.isConfigured()) {
      log.error("Sipgate isn't configured. Phone calls not supported.");
      return;
    }
    log.info("User initiates direct call from phone with id '"
        + form.getMyCurrentPhoneId()
        + "' with caller-id '"
        + form.getMyCurrentCallerId()
        + "' to destination number: "
        + StringHelper.hideStringEnding(form.getPhoneNumber(), 'x', 3));
    result = null;
    final StringBuilder buf = new StringBuilder();
    buf.append(form.getPhoneNumber()).append(SEPARATOR);
    final AddressDO address = form.getAddress();
    if (address != null
        && StringHelper.isIn(form.getPhoneNumber(), extractPhonenumber(address.getBusinessPhone()),
        extractPhonenumber(address.getMobilePhone()), extractPhonenumber(address.getPrivatePhone()),
        extractPhonenumber(address.getPrivateMobilePhone())) == true) {
      buf.append(address.getFirstName()).append(" ").append(address.getName());
      if (form.getPhoneNumber().equals(extractPhonenumber(address.getMobilePhone())) == true) {
        buf.append(", ").append(getString("address.phoneType.mobile"));
      } else if (form.getPhoneNumber().equals(extractPhonenumber(address.getPrivatePhone())) == true) {
        buf.append(", ").append(getString("address.phoneType.private"));
      }
      buf.append(" #").append(address.getId());
    } else {
      buf.append("???");
    }
    if (sipgateDirectCallService.initCall(getUser(), form.getMyCurrentPhoneId(), form.getMyCurrentCallerId(), form.getPhoneNumber())) {
      result = DateTimeFormatter.instance().getFormattedDateTime(new Date()) + ": "
          + getString("address.phoneCall.result.successful");
      form.getRecentSearchTermsQueue().append(buf.toString());
    } else {
      form.addError("address.phoneCall.result.callingError");
    }
  }

  protected String getRecentMyPhoneId() {
    return (String) getUserPrefEntry(USER_PREF_KEY_MY_RECENT_PHONE_ID);
  }

  protected void setRecentMyPhoneId(final String myPhoneId) {
    putUserPrefEntry(USER_PREF_KEY_MY_RECENT_PHONE_ID, myPhoneId, true);
  }

  protected String getRecentMyCallerId() {
    return (String) getUserPrefEntry(USER_PREF_KEY_MY_RECENT_CALLER_ID);
  }

  protected void setRecentMyCallerId(final String myCallerId) {
    putUserPrefEntry(USER_PREF_KEY_MY_RECENT_CALLER_ID, myCallerId, true);
  }

  @Override
  protected void onAfterRender() {
    super.onAfterRender();
    result = null;
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
    return getString("address.phoneCall.title");
  }
}
