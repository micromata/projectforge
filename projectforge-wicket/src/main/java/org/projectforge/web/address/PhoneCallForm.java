/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.AddressFilter;
import org.projectforge.business.address.PhoneType;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.common.BeanHelper;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.framework.utils.RecentQueue;
import org.projectforge.rest.AddressViewPageRest;
import org.projectforge.rest.sipgate.SipgateDirectCallService;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.*;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhoneCallForm extends AbstractStandardForm<Object, PhoneCallPage> {
    private static final long serialVersionUID = -2138017238114715368L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PhoneCallForm.class);

    private static final String USER_PREF_KEY_RECENTS = "phoneCalls";

    private static final String[] SEARCH_FIELDS = {"name", "firstName", "organization"};

    protected AddressDO address;

    protected PFAutoCompleteTextField<AddressDO> numberTextField;

    private DivPanel addressPanel;

    protected String phoneNumber;

    protected String callerPage;

    private String myCurrentPhoneId;

    private String myCurrentCallerId;

    Date lastSuccessfulPhoneCall;

    private RecentQueue<String> recentSearchTermsQueue;

    public PhoneCallForm(final PhoneCallPage parentPage) {
        super(parentPage);
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCallerPage() {
        return callerPage;
    }

    public void setCallerPage(String callerPage) {
        this.callerPage = callerPage;
    }

    public String getMyCurrentPhoneId() {
        if (myCurrentPhoneId == null) {
            myCurrentPhoneId = parentPage.getRecentMyPhoneId();
        }
        return myCurrentPhoneId;
    }

    public void setMyCurrentPhoneId(final String myCurrentPhoneId) {
        this.myCurrentPhoneId = myCurrentPhoneId;
        if (this.myCurrentPhoneId != null) {
            parentPage.setRecentMyPhoneId(this.myCurrentPhoneId);
        }
    }

    public String getMyCurrentCallerId() {
        if (myCurrentCallerId == null) {
            myCurrentCallerId = parentPage.getRecentMyCallerId();
        }
        return myCurrentCallerId;
    }

    public void setMyCurrentCallerId(final String myCurrentCallerId) {
        this.myCurrentCallerId = myCurrentCallerId;
        if (this.myCurrentCallerId != null) {
            parentPage.setRecentMyCallerId(this.myCurrentCallerId);
        }
    }


    public AddressDO getAddress() {
        return address;
    }

    public void setAddress(final AddressDO address) {
        this.address = address;
    }

    /**
     * @see org.projectforge.web.wicket.AbstractStandardForm#createMessageComponent()
     */
    @SuppressWarnings("serial")
    @Override
    protected Component createMessageComponent() {
        final DivPanel messagePanel = new DivPanel("message") {
            /**
             * @see org.apache.wicket.Component#isVisible()
             */
            @Override
            public boolean isVisible() {
                return StringUtils.isNotBlank(parentPage.result);
            }

            @Override
            public void onAfterRender() {
                super.onAfterRender();
                parentPage.result = null;
            }
        };
        messagePanel.add(new TextPanel(messagePanel.newChildId(), new Model<String>() {
            @Override
            public String getObject() {
                return parentPage.result;
            }
        }));
        return messagePanel;
    }

    @Override
    @SuppressWarnings({"serial", "unchecked", "rawtypes"})
    protected void init() {
        super.init();
        gridBuilder.newSplitPanel(GridSize.COL50);
        FieldsetPanel fs = gridBuilder.newFieldset(getString("address.phoneCall.number.label"),
                getString("address.phoneCall.number.labeldescription"));
        numberTextField = new PFAutoCompleteTextField<AddressDO>(InputPanel.WICKET_ID, new Model() {
            @Override
            public Serializable getObject() {
                // Pseudo object for storing search string (title field is used for this foreign purpose).
                AddressDO addr = new AddressDO();
                addr.setName(phoneNumber);
                return addr;
            }

            @Override
            public void setObject(final Serializable object) {
                if (object != null) {
                    if (object instanceof String) {
                        phoneNumber = (String) object;
                    }
                } else {
                    phoneNumber = "";
                }
            }
        }) {
            @Override
            protected List<AddressDO> getChoices(final String input) {
                final AddressFilter addressFilter = new AddressFilter();
                addressFilter.setSearchString(input);
                addressFilter.setSearchFields(SEARCH_FIELDS);
                return WicketSupport.get(AddressDao.class).select(addressFilter);
            }

            @Override
            protected List<String> getRecentUserInputs() {
                return getRecentSearchTermsQueue().getRecentList();
            }

            @Override
            protected String formatLabel(final AddressDO address) {
                return StringHelper.listToString(", ", address.getName(), address.getFirstName(), address.getOrganization());
            }

            @Override
            protected String formatValue(final AddressDO address) {
                return "id:" + address.getId();
            }

            /**
             * @see org.apache.wicket.Component#getConverter(java.lang.Class)
             */
            @Override
            public <C> IConverter<C> getConverter(final Class<C> type) {
                return new IConverter() {
                    @Override
                    public Object convertToObject(final String value, final Locale locale) {
                        phoneNumber = value;
                        AddressDO addr = new AddressDO();
                        addr.setName(phoneNumber);
                        return addr;
                    }

                    @Override
                    public String convertToString(final Object value, final Locale locale) {
                        return phoneNumber;
                    }
                };
            }
        };
        numberTextField.withLabelValue(true).withMatchContains(true).withMinChars(2).withFocus(true).withAutoSubmit(true);
        if (StringUtils.isBlank(phoneNumber)) {
            if (address != null) {
                final String no = parentPage.getFirstPhoneNumber();
                if (StringUtils.isNotBlank(no)) {
                    phoneNumber = parentPage.extractPhonenumber(no);
                }
            } else {
                final String recentNumber = getRecentSearchTermsQueue().get(0);
                if (StringUtils.isNotBlank(recentNumber)) {
                    phoneNumber = recentNumber;
                }
            }
        }
        fs.add(numberTextField);
        fs.addKeyboardHelpIcon(new ResourceModel("tooltip.autocompletion.title"),
                new ResourceModel("address.directCall.number.tooltip"));

        var sipgateDirectCallService = WicketSupport.get(SipgateDirectCallService.class);
        if (sipgateDirectCallService.isAvailable()) {
            // DropDownChoice myCurrentPhoneId
            fs = gridBuilder.newFieldset(getString("address.myCurrentPhoneId"));
            final LabelValueChoiceRenderer<String> myCurrentPhoneIdChoiceRenderer = new LabelValueChoiceRenderer<String>();
            List<String> ids = sipgateDirectCallService.getCallerNumbers(ThreadLocalUserContext.getLoggedInUser());
            if (CollectionUtils.isEmpty(ids)) {
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

            // DropDownChoice myCurrentCallerId
            fs = gridBuilder.newFieldset(getString("address.myCurrentCallerId"));
            final LabelValueChoiceRenderer<String> myCurrentCalerIdChoiceRenderer = new LabelValueChoiceRenderer<String>();
            ids = sipgateDirectCallService.getCallerIds(ThreadLocalUserContext.getLoggedInUser());
            for (final String id : ids) {
                myCurrentCalerIdChoiceRenderer.addValue(id, id);
            }
            final DropDownChoice myCurrentCallerIdChoice = new DropDownChoice(fs.getDropDownChoiceId(),
                    new PropertyModel(this, "myCurrentCallerId"), myCurrentCalerIdChoiceRenderer.getValues(),
                    myCurrentCalerIdChoiceRenderer);
            myCurrentCallerIdChoice.setNullValid(false).setRequired(true);
            fs.add(myCurrentCallerIdChoice);
            fs.addHelpIcon(new ResourceModel("address.myCurrentCallerId.tooltip.title"),
                    new ResourceModel("address.myCurrentCallerId.tooltip.content"));
        }
        addressPanel = gridBuilder.newSplitPanel(GridSize.COL50).getPanel();
        {
            final Link<String> addressViewLink = new Link<String>(TextLinkPanel.LINK_ID) {
                @Override
                public void onClick() {
                    if (address == null) {
                        log.error("Oups should not occur: AddressViewLink is shown without a given address. Ignoring link.");
                        return;
                    }
                    throw new RedirectToUrlException(AddressViewPageRest.getPageUrl(address.getId(), "/wa/phoneCall"));
                }
            };
            final TextLinkPanel addressLinkPanel = new TextLinkPanel(addressPanel.newChildId(), addressViewLink,
                    new Model<String>() {
                        @Override
                        public String getObject() {
                            if (address == null) {
                                return "";
                            }
                            final StringBuilder buf = new StringBuilder();
                            if (address.getForm() != null) {
                                buf.append(getString(address.getForm().getI18nKey())).append(" ");
                            }
                            if (StringUtils.isNotBlank(address.getTitle())) {
                                buf.append(address.getTitle()).append(" ");
                            }
                            if (StringUtils.isNotBlank(address.getFirstName())) {
                                buf.append(address.getFirstName()).append(" ");
                            }
                            if (StringUtils.isNotBlank(address.getName())) {
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
            final Button backButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("back")) {
                @Override
                public void onSubmit() {
                    parentPage.backToCaller();
                }
            };
            final SingleButtonPanel backButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), backButton,
                    getString("back"), SingleButtonPanel.INFO);
            actionButtons.add(backButtonPanel);
        }
        {
            final Button callButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("call")) {
                @Override
                public void onSubmit() {
                    parentPage.call();
                }
            };
            final SingleButtonPanel callButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), callButton,
                    getString("address.directCall.call"), SingleButtonPanel.DEFAULT_SUBMIT);
            actionButtons.add(callButtonPanel);
            setDefaultButton(callButton);
        }
    }

    private void addLineBreak() {
        final TextPanel lineBreak = new TextPanel(addressPanel.newChildId(), "<br/>");
        lineBreak.getLabel().setEscapeModelStrings(false);
        addressPanel.add(lineBreak);
    }

    @SuppressWarnings("serial")
    private void addPhoneNumber(final String property, final String label) {
        final SubmitLink numberLink = new SubmitLink(TextLinkPanel.LINK_ID) {
            @Override
            public void onSubmit() {
                final String number = (String) BeanHelper.getProperty(address, property);
                setPhoneNumber(parentPage.extractPhonenumber(number));
                AddressDO addr = new AddressDO();
                addr.setName(getPhoneNumber());
                numberTextField.setModelObject(addr);
                numberTextField.modelChanged();
                parentPage.call();
            }
        };
        final TextLinkPanel numberLinkPanel = new TextLinkPanel(addressPanel.newChildId(), numberLink, new Model<String>() {
            @Override
            public String getObject() {
                final String number = (String) BeanHelper.getProperty(address, property);
                return HtmlHelper.escapeHtml(number + " (" + label + ")\n", true);
            }
        }) {
            /**
             * @see org.apache.wicket.Component#isVisible()
             */
            @Override
            public boolean isVisible() {
                if (address == null) {
                    return false;
                }
                final String number = (String) BeanHelper.getProperty(address, property);
                return (StringUtils.isNotBlank(number));
            }
        };
        numberLinkPanel.getLabel().setEscapeModelStrings(false);
        addressPanel.add(numberLinkPanel);
    }

    protected String getPhoneNumberAndPerson(final AddressDO address, final PhoneType phoneType, final String number,
                                             final String countryPrefix) {
        return StringHelper.listToString(", ",
                NumberHelper.extractPhonenumber(number, countryPrefix) + ": " + address.getName(),
                address.getFirstName(), getString(phoneType.getI18nKey()), address.getOrganization());
    }

    @SuppressWarnings("unchecked")
    protected RecentQueue<String> getRecentSearchTermsQueue() {
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
