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

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.AddressFilter;
import org.projectforge.business.address.PhoneType;
import org.projectforge.business.user.service.UserService;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.common.BeanHelper;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.framework.utils.RecentQueue;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextLinkPanel;
import org.projectforge.web.wicket.flowlayout.TextPanel;

public class PhoneCallForm extends AbstractStandardForm<Object, PhoneCallPage>
{
  private static final long serialVersionUID = -2138017238114715368L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PhoneCallForm.class);

  private static final String USER_PREF_KEY_RECENTS = "phoneCalls";

  @SpringBean
  private AddressDao addressDao;

  @SpringBean
  private UserService userService;

  protected AddressDO address;

  protected PFAutoCompleteTextField<AddressDO> numberTextField;

  private DivPanel addressPanel;

  protected String phoneNumber;

  private String myCurrentPhoneId;

  Date lastSuccessfulPhoneCall;

  private RecentQueue<String> recentSearchTermsQueue;

  public PhoneCallForm(final PhoneCallPage parentPage)
  {
    super(parentPage);
  }

  public String getPhoneNumber()
  {
    return phoneNumber;
  }

  public void setPhoneNumber(final String phoneNumber)
  {
    this.phoneNumber = phoneNumber;
  }

  public String getMyCurrentPhoneId()
  {
    if (myCurrentPhoneId == null) {
      myCurrentPhoneId = parentPage.getRecentMyPhoneId();
    }
    return myCurrentPhoneId;
  }

  public void setMyCurrentPhoneId(final String myCurrentPhoneId)
  {
    this.myCurrentPhoneId = myCurrentPhoneId;
    if (this.myCurrentPhoneId != null) {
      parentPage.setRecentMyPhoneId(this.myCurrentPhoneId);
    }
  }

  public AddressDO getAddress()
  {
    return address;
  }

  public void setAddress(final AddressDO address)
  {
    this.address = address;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractStandardForm#createMessageComponent()
   */
  @SuppressWarnings("serial")
  @Override
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

  @Override
  @SuppressWarnings({ "serial", "unchecked", "rawtypes" })
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL50);
    FieldsetPanel fs = gridBuilder.newFieldset(getString("address.phoneCall.number.label"),
        getString("address.phoneCall.number.labeldescription"));
    numberTextField = new PFAutoCompleteTextField<AddressDO>(InputPanel.WICKET_ID, new Model()
    {
      @Override
      public Serializable getObject()
      {
        // Pseudo object for storing search string (title field is used for this foreign purpose).
        return new AddressDO().setName(phoneNumber);
      }

      @Override
      public void setObject(final Serializable object)
      {
        if (object != null) {
          if (object instanceof String) {
            phoneNumber = (String) object;
          }
        } else {
          phoneNumber = "";
        }
      }
    })
    {
      @Override
      protected List<AddressDO> getChoices(final String input)
      {
        final AddressFilter addressFilter = new AddressFilter();
        addressFilter.setSearchString(input);
        addressFilter.setSearchFields("name", "firstName", "organization");
        return addressDao.getList(addressFilter);
      }

      @Override
      protected List<String> getRecentUserInputs()
      {
        return getRecentSearchTermsQueue().getRecents();
      }

      @Override
      protected String formatLabel(final AddressDO address)
      {
        return StringHelper.listToString(", ", address.getName(), address.getFirstName(), address.getOrganization());
      }

      @Override
      protected String formatValue(final AddressDO address)
      {
        return "id:" + address.getId();
      }

      /**
       * @see org.apache.wicket.Component#getConverter(java.lang.Class)
       */
      @Override
      public <C> IConverter<C> getConverter(final Class<C> type)
      {
        return new IConverter()
        {
          @Override
          public Object convertToObject(final String value, final Locale locale)
          {
            phoneNumber = value;
            return new AddressDO().setName(phoneNumber);
          }

          @Override
          public String convertToString(final Object value, final Locale locale)
          {
            return phoneNumber;
          }
        };
      }
    };
    numberTextField.withLabelValue(true).withMatchContains(true).withMinChars(2).withFocus(true).withAutoSubmit(true);
    if (StringUtils.isBlank(phoneNumber) == true) {
      if (address != null) {
        final String no = parentPage.getFirstPhoneNumber();
        if (StringUtils.isNotBlank(no) == true) {
          phoneNumber = parentPage.extractPhonenumber(no);
        }
      } else {
        final String recentNumber = getRecentSearchTermsQueue().get(0);
        if (StringUtils.isNotBlank(recentNumber) == true) {
          phoneNumber = recentNumber;
        }
      }
    }
    fs.add(numberTextField);
    fs.addKeyboardHelpIcon(new ResourceModel("tooltip.autocompletion.title"),
        new ResourceModel("address.directCall.number.tooltip"));

    {
      // DropDownChoice myCurrentPhoneId
      fs = gridBuilder.newFieldset(getString("address.myCurrentPhoneId"));
      final LabelValueChoiceRenderer<String> myCurrentPhoneIdChoiceRenderer = new LabelValueChoiceRenderer<String>();
      final String[] ids = userService.getPersonalPhoneIdentifiers(ThreadLocalUserContext.getUser());
      if (ids == null) {
        myCurrentPhoneIdChoiceRenderer.addValue("--", getString("user.personalPhoneIdentifiers.pleaseDefine"));
      } else {
        for (final String id : ids) {
          myCurrentPhoneIdChoiceRenderer.addValue(id, id);
        }
      }
      final DropDownChoice myCurrentPhoneIdChoice = new DropDownChoice(fs.getDropDownChoiceId(),
          new PropertyModel(this, "myCurrentPhoneId"), myCurrentPhoneIdChoiceRenderer.getValues(),
          myCurrentPhoneIdChoiceRenderer);
      myCurrentPhoneIdChoice.setNullValid(false).setRequired(true);
      fs.add(myCurrentPhoneIdChoice);
      fs.addHelpIcon(new ResourceModel("address.myCurrentPhoneId.tooltip.title"),
          new ResourceModel("address.myCurrentPhoneId.tooltip.content"));
    }
    addressPanel = gridBuilder.newSplitPanel(GridSize.COL50).getPanel();
    {
      final Link<String> addressViewLink = new Link<String>(TextLinkPanel.LINK_ID)
      {
        @Override
        public void onClick()
        {
          if (address == null) {
            log.error("Oups should not occur: AddressViewLink is shown without a given address. Ignoring link.");
            return;
          }
          final PageParameters params = new PageParameters();
          params.add(AbstractEditPage.PARAMETER_KEY_ID, address.getId());
          setResponsePage(new AddressViewPage(params, parentPage));
        }
      };
      final TextLinkPanel addressLinkPanel = new TextLinkPanel(addressPanel.newChildId(), addressViewLink,
          new Model<String>()
          {
            @Override
            public String getObject()
            {
              if (address == null) {
                return "";
              }
              final StringBuffer buf = new StringBuffer();
              if (address.getForm() != null) {
                buf.append(getString(address.getForm().getI18nKey())).append(" ");
              }
              if (StringUtils.isNotBlank(address.getTitle()) == true) {
                buf.append(address.getTitle()).append(" ");
              }
              if (StringUtils.isNotBlank(address.getFirstName()) == true) {
                buf.append(address.getFirstName()).append(" ");
              }
              if (StringUtils.isNotBlank(address.getName()) == true) {
                buf.append(address.getName());
              }
              return buf.toString();
            }
          });
      addressPanel.add(addressLinkPanel);
      addLineBreak();
    }
    {
      addPhoneNumber("businessPhone", getString(PhoneType.BUSINESS.getI18nKey()));
      addPhoneNumber("mobilePhone", getString(PhoneType.MOBILE.getI18nKey()));
      addPhoneNumber("privatePhone", getString(PhoneType.PRIVATE.getI18nKey()));
      addPhoneNumber("privateMobilePhone", getString(PhoneType.PRIVATE_MOBILE.getI18nKey()));
    }
    {
      final Button callButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("call"))
      {
        @Override
        public final void onSubmit()
        {
          parentPage.call();
        }
      };
      final SingleButtonPanel callButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), callButton,
          getString("address.directCall.call"), SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(callButtonPanel);
      setDefaultButton(callButton);
    }
    final String url = ConfigXml.getInstance().getTelephoneSystemOperatorPanelUrl();
    if (url != null) {
      final DivPanel section = gridBuilder.newGridPanel().getPanel();
      final TextPanel showOperatorPanel = new TextPanel(section.newChildId(), url);
      showOperatorPanel.getLabel().setEscapeModelStrings(false);
      section.add(showOperatorPanel);
    }
  }

  private void addLineBreak()
  {
    final TextPanel lineBreak = new TextPanel(addressPanel.newChildId(), "<br/>");
    lineBreak.getLabel().setEscapeModelStrings(false);
    addressPanel.add(lineBreak);
  }

  @SuppressWarnings("serial")
  private void addPhoneNumber(final String property, final String label)
  {
    final SubmitLink numberLink = new SubmitLink(TextLinkPanel.LINK_ID)
    {
      @Override
      public void onSubmit()
      {
        final String number = (String) BeanHelper.getProperty(address, property);
        setPhoneNumber(parentPage.extractPhonenumber(number));
        numberTextField.setModelObject(new AddressDO().setName(getPhoneNumber()));
        numberTextField.modelChanged();
        parentPage.call();
      }
    };
    final TextLinkPanel numberLinkPanel = new TextLinkPanel(addressPanel.newChildId(), numberLink, new Model<String>()
    {
      @Override
      public String getObject()
      {
        final String number = (String) BeanHelper.getProperty(address, property);
        return HtmlHelper.escapeHtml(number + " (" + label + ")\n", true);
      }
    })
    {
      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        if (address == null) {
          return false;
        }
        final String number = (String) BeanHelper.getProperty(address, property);
        return (StringUtils.isNotBlank(number) == true);
      }
    };
    numberLinkPanel.getLabel().setEscapeModelStrings(false);
    addressPanel.add(numberLinkPanel);
  }

  protected String getPhoneNumberAndPerson(final AddressDO address, final PhoneType phoneType, final String number,
      final String countryPrefix)
  {
    return StringHelper.listToString(", ",
        NumberHelper.extractPhonenumber(number, countryPrefix) + ": " + address.getName(),
        address.getFirstName(), getString(phoneType.getI18nKey()), address.getOrganization());
  }

  @SuppressWarnings("unchecked")
  protected RecentQueue<String> getRecentSearchTermsQueue()
  {
    if (recentSearchTermsQueue == null) {
      recentSearchTermsQueue = (RecentQueue<String>) parentPage.getUserPrefEntry(USER_PREF_KEY_RECENTS);
    }
    if (recentSearchTermsQueue == null) {
      recentSearchTermsQueue = (RecentQueue<String>) parentPage
          .getUserPrefEntry("org.projectforge.web.address.PhoneCallAction:recentSearchTerms");
      if (recentSearchTermsQueue != null) {
        // Old entries:
        parentPage.putUserPrefEntry(USER_PREF_KEY_RECENTS, recentSearchTermsQueue, true);
        parentPage.removeUserPrefEntry("org.projectforge.web.address.PhoneCallAction:recentSearchTerms");
      }
    }
    if (recentSearchTermsQueue == null) {
      recentSearchTermsQueue = new RecentQueue<String>();
      parentPage.putUserPrefEntry(USER_PREF_KEY_RECENTS, recentSearchTermsQueue, true);
    }
    return recentSearchTermsQueue;
  }
}
