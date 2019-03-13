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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.AddressFilter;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.framework.utils.RecentQueue;
import org.projectforge.sms.SmsSenderConfig;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;
import org.projectforge.web.wicket.flowlayout.TextPanel;

public class SendSmsForm extends AbstractStandardForm<SendSmsData, SendSmsPage>
{
  private static final long serialVersionUID = -2138017238114715368L;

  private static final String USER_PREF_KEY_RECENTS = "messagingReceivers";

  @SpringBean
  private ConfigurationService configurationService;

  @SpringBean
  private SmsSenderConfig smsSenderConfig;

  @SpringBean
  private AddressDao addressDao;

  protected SendSmsData data;

  private RecentQueue<String> recentSearchTermsQueue;

  public SendSmsForm(final SendSmsPage parentPage)
  {
    super(parentPage);
    data = new SendSmsData();
  }

  protected static String getPhoneNumberAndPerson(final AddressDO address, final String number,
      final String countryPrefix)
  {
    return StringHelper.listToString(", ",
        NumberHelper.extractPhonenumber(number, countryPrefix) + ": " + address.getName(),
        address.getFirstName(), address.getOrganization());
  }

  private void buildAutocompleteEntry(final List<String> list, final AddressDO address, final String number)
  {
    if (StringUtils.isBlank(number) == true) {
      return;
    }
    list.add(getPhoneNumberAndPerson(address, number,
        Configuration.getInstance().getStringValue(ConfigurationParam.DEFAULT_COUNTRY_PHONE_PREFIX)));
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    FieldsetPanel fs = gridBuilder.newFieldset(getString("address.sendSms.phoneNumber"));
    final PFAutoCompleteTextField<String> numberTextField = new PFAutoCompleteTextField<String>(InputPanel.WICKET_ID,
        new PropertyModel<String>(data, "phoneNumber"))
    {
      @Override
      protected List<String> getChoices(final String input)
      {
        final AddressFilter addressFilter = new AddressFilter();
        addressFilter.setSearchString(input);
        final List<String> list = new ArrayList<String>();
        for (final AddressDO address : addressDao.getList(addressFilter)) {
          buildAutocompleteEntry(list, address, address.getMobilePhone());
          buildAutocompleteEntry(list, address, address.getPrivateMobilePhone());
        }
        return list;
      }

      @Override
      protected List<String> getFavorites()
      {
        return getRecentSearchTermsQueue().getRecents();
      }
    };
    numberTextField.withMatchContains(true).withMinChars(2).withFocus(true);
    numberTextField.setRequired(true);
    fs.add(numberTextField);
    data.setMessage(getInitalMessageText());
    fs = gridBuilder.newFieldset(getString("address.sendSms.message"));
    final MaxLengthTextArea messageTextArea = new MaxLengthTextArea(TextAreaPanel.WICKET_ID,
        new PropertyModel<String>(data, "message"),
            smsSenderConfig.getSmsMaxMessageLength());
    // messageTextArea.add(AttributeModifier.append("onKeyDown", "limitText(this.form.limitedtextarea,this.form.countdown,"
    // + MAX_MESSAGE_LENGTH
    // + ")"));
    // messageTextArea.add(AttributeModifier.append("onKeyUp", "limitText(this.form.limitedtextarea,this.form.countdown,"
    // + MAX_MESSAGE_LENGTH
    // + ")"));
    messageTextArea.add(AttributeModifier.append("maxlength", smsSenderConfig.getSmsMaxMessageLength()));
    fs.add(messageTextArea);
    fs = gridBuilder.newFieldset("");
    final DivTextPanel charsRemaining = new DivTextPanel(fs.newChildId(), "");
    charsRemaining.setMarkupId("charsRemaining");
    fs.add(charsRemaining);

    {
      final Button resetButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("reset"))
      {
        @Override
        public final void onSubmit()
        {
          data.setMessage(getInitalMessageText());
          data.setPhoneNumber("");
          numberTextField.modelChanged();
          messageTextArea.modelChanged();
        }
      };
      resetButton.setDefaultFormProcessing(false);
      final SingleButtonPanel resetButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), resetButton,
          getString("reset"),
          SingleButtonPanel.RESET);
      actionButtons.add(resetButtonPanel);

      final Button sendButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("send"))
      {
        @Override
        public final void onSubmit()
        {
          parentPage.send();
        }
      };
      sendButton.add(AttributeModifier.replace("onclick", "return showSendQuestionDialog();"));
      final SingleButtonPanel sendButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), sendButton,
          getString("send"),
          SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(sendButtonPanel);
      setDefaultButton(sendButton);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractStandardForm#createMessageComponent()
   */
  @Override
  @SuppressWarnings("serial")
  protected Component createMessageComponent()
  {
    final DivPanel messagePanel = new DivPanel("message")
    {
      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        return StringUtils.isNotBlank(parentPage.result);
      }

      @Override
      public void onAfterRender()
      {
        super.onAfterRender();
        parentPage.result = null;
      }
    };
    messagePanel.add(new TextPanel(messagePanel.newChildId(), new Model<String>()
    {
      @Override
      public String getObject()
      {
        return parentPage.result;
      }
    }));
    return messagePanel;
  }

  @SuppressWarnings("unchecked")
  protected RecentQueue<String> getRecentSearchTermsQueue()
  {
    if (recentSearchTermsQueue == null) {
      recentSearchTermsQueue = (RecentQueue<String>) parentPage.getUserPrefEntry(USER_PREF_KEY_RECENTS);
    }
    if (recentSearchTermsQueue == null) {
      recentSearchTermsQueue = (RecentQueue<String>) parentPage
          .getUserPrefEntry(this.getClass().getName() + ":recentSearchTerms");
      if (recentSearchTermsQueue != null) {
        // Old entries:
        parentPage.putUserPrefEntry(USER_PREF_KEY_RECENTS, recentSearchTermsQueue, true);
        parentPage.removeUserPrefEntry(this.getClass().getName() + ":recentSearchTerms");
      }
    }
    if (recentSearchTermsQueue == null) {
      recentSearchTermsQueue = new RecentQueue<String>();
      parentPage.putUserPrefEntry(USER_PREF_KEY_RECENTS, recentSearchTermsQueue, true);
    }
    return recentSearchTermsQueue;
  }

  public String getInitalMessageText()
  {
    return getUser().getFullname() + ". " + getString("address.sendSms.doNotReply");
  }
}
