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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.AddressStatus;
import org.projectforge.business.address.ContactStatus;
import org.projectforge.business.address.FormOfAddress;
import org.projectforge.business.address.PersonalAddressDO;
import org.projectforge.business.address.PersonalAddressDao;
import org.projectforge.web.common.PhoneNumberValidator;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteMaxLengthTextField;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.LanguageField;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel;
import org.projectforge.web.wicket.flowlayout.AbstractGridBuilder;
import org.projectforge.web.wicket.flowlayout.FieldProperties;
import org.projectforge.web.wicket.flowlayout.FieldType;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.HtmlCommentPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;
import org.projectforge.web.wicket.mobileflowlayout.MobileGridBuilder;

/**
 * For sharing functionality between mobile and normal edit pages.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
class AddressPageSupport implements Serializable
{
  private static final long serialVersionUID = -7434267003873770614L;

  private AddressDao addressDao;

  PersonalAddressDO personalAddress;

  private final AddressDO address;

  private Form<AddressDO> form;

  @SuppressWarnings("unchecked")
  private final TextField<String>[] dependentFormComponents = new TextField[3];

  private AbstractGridBuilder<?> gridBuilder;

  private boolean mobile;

  /**
   * Constructor for mobile view page.
   * 
   * @param gridBuilder Needed for translations.
   * @param address
   */
  public AddressPageSupport(final MobileGridBuilder gridBuilder, final AddressDO address)
  {
    this.gridBuilder = gridBuilder;
    this.address = address;
    mobile = true;
  }

  /**
   * Constructor for edit pages.
   * 
   * @param form
   * @param gridBuilder
   * @param addressDao
   * @param personalAddressDao
   * @param address
   */
  @SuppressWarnings("serial")
  public AddressPageSupport(final Form<AddressDO> form, final AbstractGridBuilder<?> gridBuilder,
      final AddressDao addressDao,
      final PersonalAddressDao personalAddressDao, final AddressDO address)
  {
    this.form = form;
    this.gridBuilder = gridBuilder;
    this.addressDao = addressDao;
    this.address = address;
    if (gridBuilder instanceof MobileGridBuilder) {
      mobile = true;
    }
    personalAddress = null;
    if (isNew() == false) {
      personalAddress = personalAddressDao.getByAddressId(address.getId());
    }
    if (personalAddress == null) {
      personalAddress = new PersonalAddressDO();
    }
    form.add(new IFormValidator()
    {
      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        return dependentFormComponents;
      }

      @Override
      public void validate(final Form<?> form)
      {
        final TextField<String> name = dependentFormComponents[0];
        final TextField<String> firstName = dependentFormComponents[1];
        final TextField<String> organization = dependentFormComponents[2];
        if (StringUtils.isBlank(name.getValue()) == true
            && StringUtils.isBlank(firstName.getValue()) == true
            && StringUtils.isBlank(organization.getValue()) == true) {
          form.error(getString("address.form.error.toFewFields"));
        }
      }
    });
  }

  public AbstractFieldsetPanel<?> addName()
  {
    final FieldProperties<String> props = getNameProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    final MaxLengthTextField name = new MaxLengthTextField(fs.getTextFieldId(), props.getModel());
    name.setMarkupId("name").setOutputMarkupId(true);
    fs.add(dependentFormComponents[1] = name);
    if (isNew() == true) {
      WicketUtils.setFocus(name);
    }
    return fs;
  }

  public FieldProperties<String> getNameProperties()
  {
    return new FieldProperties<String>("name", new PropertyModel<String>(address, "name"));
  }

  public AbstractFieldsetPanel<?> addFirstName()
  {
    final FieldProperties<String> props = getFirstNameProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    MaxLengthTextField maxLengthTextField = new MaxLengthTextField(fs.getTextFieldId(), props.getModel());
    maxLengthTextField.setMarkupId("firstName").setOutputMarkupId(true);
    fs.add(dependentFormComponents[0] = maxLengthTextField);
    return fs;
  }

  public FieldProperties<String> getFirstNameProperties()
  {
    return new FieldProperties<String>("firstName", new PropertyModel<String>(address, "firstName"));
  }

  public AbstractFieldsetPanel<?> addFormOfAddress()
  {
    final FieldProperties<FormOfAddress> props = getFormOfAddressProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    final LabelValueChoiceRenderer<FormOfAddress> formChoiceRenderer = new LabelValueChoiceRenderer<FormOfAddress>(form,
        FormOfAddress.values());
    fs.addDropDownChoice(props.getModel(), formChoiceRenderer.getValues(), formChoiceRenderer).setRequired(true)
        .setNullValid(false).getDropDownChoice().setMarkupId("form").setOutputMarkupId(true);
    return fs;
  }

  public FieldProperties<FormOfAddress> getFormOfAddressProperties()
  {
    return new FieldProperties<FormOfAddress>("address.form", new PropertyModel<FormOfAddress>(address, "form"));
  }

  public AbstractFieldsetPanel<?> addTitle()
  {
    final FieldProperties<String> props = getTitleProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    MaxLengthTextField textField = new MaxLengthTextField(fs.getTextFieldId(), props.getModel());
    textField.setMarkupId("title").setOutputMarkupId(true);
    fs.add(textField);
    return fs;
  }

  public FieldProperties<String> getTitleProperties()
  {
    return new FieldProperties<String>("address.title", new PropertyModel<String>(address, "title"));
  }

  public AbstractFieldsetPanel<?> addWebsite()
  {
    final FieldProperties<String> props = getWebsiteProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    MaxLengthTextField textField = new MaxLengthTextField(fs.getTextFieldId(), props.getModel());
    textField.setMarkupId("website").setOutputMarkupId(true);
    fs.add(textField, props);
    return fs;
  }

  public FieldProperties<String> getWebsiteProperties()
  {
    return new FieldProperties<String>("address.website", new PropertyModel<String>(address, "website"))
        .setFieldType(FieldType.WEB_PAGE);
  }

  @SuppressWarnings("serial")
  public AbstractFieldsetPanel<?> addOrganization()
  {
    final FieldProperties<String> props = getOrganizationProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    PFAutoCompleteTextField organizationTextField = new PFAutoCompleteMaxLengthTextField(fs.getTextFieldId(), props.getPropertyModel())
    {

      @Override
      protected List<String> getChoices(final String input)
      {
        return addressDao.getAutocompletion("organization", input);
      }
    }.withMatchContains(true).withMinChars(2);
    organizationTextField.setMarkupId("organization").setOutputMarkupId(true);
    fs.add(
        dependentFormComponents[2] = organizationTextField);
    return fs;
  }

  public FieldProperties<String> getOrganizationProperties()
  {
    return new FieldProperties<String>("organization", new PropertyModel<String>(address, "organization"));
  }

  public AbstractFieldsetPanel<?> addDivision()
  {
    final FieldProperties<String> props = getDivisionProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    MaxLengthTextField textField = new MaxLengthTextField(fs.getTextFieldId(), props.getModel());
    textField.setMarkupId("division").setOutputMarkupId(true);
    fs.add(textField);
    return fs;
  }

  public FieldProperties<String> getDivisionProperties()
  {
    return new FieldProperties<String>("address.division", new PropertyModel<String>(address, "division"));
  }

  public AbstractFieldsetPanel<?> addPosition()
  {
    final FieldProperties<String> props = getPositionTextProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    MaxLengthTextField textField = new MaxLengthTextField(fs.getTextFieldId(), props.getPropertyModel());
    textField.setMarkupId("position").setOutputMarkupId(true);
    fs.add(textField);
    return fs;
  }

  public FieldProperties<String> getPositionTextProperties()
  {
    return new FieldProperties<String>("address.positionText", new PropertyModel<String>(address, "positionText"));
  }

  public AbstractFieldsetPanel<?> addEmail()
  {
    final FieldProperties<String> props = getEmailProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    MaxLengthTextField textField = new MaxLengthTextField(fs.getTextFieldId(), props.getModel());
    textField.setMarkupId("email").setOutputMarkupId(true);
    fs.add(textField, props);
    return fs;
  }

  public FieldProperties<String> getEmailProperties()
  {
    return new FieldProperties<String>("email", new PropertyModel<String>(address, "email"))
        .setLabelDescription("address.business")
        .setFieldType(FieldType.E_MAIL);
  }

  public AbstractFieldsetPanel<?> addPrivateEmail()
  {
    final FieldProperties<String> props = getPrivateEmailProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    MaxLengthTextField textField = new MaxLengthTextField(fs.getTextFieldId(), props.getModel());
    textField.setMarkupId("privateEmail").setOutputMarkupId(true);
    fs.add(textField, props);
    return fs;
  }

  public FieldProperties<String> getPrivateEmailProperties()
  {
    return new FieldProperties<String>("email", new PropertyModel<String>(address, "privateEmail"))
        .setLabelDescription("address.private")
        .setFieldType(FieldType.E_MAIL);
  }

  public AbstractFieldsetPanel<?> addContactStatus()
  {
    final FieldProperties<ContactStatus> props = getContactStatusProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    final LabelValueChoiceRenderer<ContactStatus> contactStatusChoiceRenderer = new LabelValueChoiceRenderer<ContactStatus>(
        form,
        ContactStatus.values());
    fs.addDropDownChoice(props.getModel(), contactStatusChoiceRenderer.getValues(), contactStatusChoiceRenderer)
        .setRequired(true)
        .setNullValid(false).getDropDownChoice().setMarkupId("contactStatus").setOutputMarkupId(true);
    return fs;
  }

  public FieldProperties<ContactStatus> getContactStatusProperties()
  {
    return new FieldProperties<ContactStatus>("address.contactStatus",
        new PropertyModel<ContactStatus>(address, "contactStatus"));
  }

  public AbstractFieldsetPanel<?> addAddressStatus()
  {
    final FieldProperties<AddressStatus> props = getAddressStatusProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    final LabelValueChoiceRenderer<AddressStatus> addressStatusChoiceRenderer = new LabelValueChoiceRenderer<AddressStatus>(
        form,
        AddressStatus.values());
    fs.addDropDownChoice(props.getModel(), addressStatusChoiceRenderer.getValues(), addressStatusChoiceRenderer)
        .setRequired(true)
        .setNullValid(false).getDropDownChoice().setMarkupId("addressStatus").setOutputMarkupId(true);
    return fs;
  }

  public FieldProperties<AddressStatus> getAddressStatusProperties()
  {
    return new FieldProperties<AddressStatus>("address.addressStatus",
        new PropertyModel<AddressStatus>(address, "addressStatus"));
  }

  @SuppressWarnings("serial")
  public AbstractFieldsetPanel<?> addBirthday()
  {
    final FieldProperties<Date> props = getBirthdayProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    DatePanel components = new DatePanel(fs.newChildId(), props.getModel(),
        DatePanelSettings.get().withTargetType(java.sql.Date.class));
    components.getFormComponent().setMarkupId("birthday").setOutputMarkupId(true);
    fs.add(
        components);
    fs.add(new HtmlCommentPanel(fs.newChildId(), new Model<String>()
    {
      @Override
      public String getObject()
      {
        return WicketUtils.getUTCDate("birthday", address.getBirthday());
      }
    }));
    return fs;
  }

  public FieldProperties<Date> getBirthdayProperties()
  {
    return new FieldProperties<Date>("address.birthday", new PropertyModel<Date>(address, "birthday"));
  }

  public AbstractFieldsetPanel<?> addLanguage()
  {
    final FieldProperties<Locale> props = getLanguageProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    final LanguageField language = new LanguageField(fs.getTextFieldId(), props.getModel());
    language.setFavoriteLanguages(addressDao.getUsedCommunicationLanguages());
    language.setMarkupId("language").setOutputMarkupId(true);
    fs.add(language);
    if (mobile == false) {
      ((FieldsetPanel) fs).addKeyboardHelpIcon(new ResourceModel("tooltip.autocompletion.title"), new ResourceModel(
          "tooltip.autocomplete.language"));
    }
    return fs;
  }

  public FieldProperties<Locale> getLanguageProperties()
  {
    return new FieldProperties<Locale>("language", new PropertyModel<Locale>(address, "communicationLanguage"));
  }

  public AbstractFieldsetPanel<?> addFingerPrint()
  {
    final FieldProperties<String> props = getFingerPringProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    MaxLengthTextField textField = new MaxLengthTextField(fs.getTextFieldId(), props.getModel());
    textField.setMarkupId("fingerprint").setOutputMarkupId(true);
    fs.add(textField);
    return fs;
  }

  public FieldProperties<String> getFingerPringProperties()
  {
    return new FieldProperties<String>("address.fingerprint", new PropertyModel<String>(address, "fingerprint"));
  }

  public AbstractFieldsetPanel<?> addPublicKey()
  {
    final FieldProperties<String> props = getPublicKeyProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    MaxLengthTextArea textArea = new MaxLengthTextArea(TextAreaPanel.WICKET_ID, props.getModel());
    textArea.setMarkupId("publicKey").setOutputMarkupId(true);
    fs.add(textArea);// .setAutogrow();
    return fs;
  }

  public FieldProperties<String> getPublicKeyProperties()
  {
    return new FieldProperties<String>("address.publicKey", new PropertyModel<String>(address, "publicKey"));
  }

  public AbstractFieldsetPanel<?> addComment()
  {
    final FieldProperties<String> props = getCommentProperties();
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    final MaxLengthTextArea comment = new MaxLengthTextArea(TextAreaPanel.WICKET_ID, props.getModel());
    comment.setMarkupId("comment").setOutputMarkupId(true);
    fs.add(comment, true);
    return fs;
  }

  public FieldProperties<String> getCommentProperties()
  {
    return new FieldProperties<String>("comment", new PropertyModel<String>(address, "comment"));
  }

  public AbstractFieldsetPanel<?> addPhoneNumber(final String property, final String labelKey,
      final String labelDescriptionKey,
      final FieldType fieldType)
  {
    final FieldProperties<String> props = getPhoneNumberProperties(property, labelKey, labelDescriptionKey, fieldType);
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    final MaxLengthTextField phoneNumber = new MaxLengthTextField(fs.getTextFieldId(), props.getModel());
    fs.add(phoneNumber, props);
    phoneNumber.setMarkupId("phoneNumber").setOutputMarkupId(true);
    phoneNumber.add(new PhoneNumberValidator());
    return fs;
  }

  public FieldProperties<String> getPhoneNumberProperties(final String property, final String labelKey,
      final String labelDescriptionKey,
      final FieldType fieldType)
  {
    return new FieldProperties<String>(labelKey, new PropertyModel<String>(address, property))
        .setLabelDescription(labelDescriptionKey)
        .setFieldType(fieldType);
  }

  @SuppressWarnings("serial")
  public AbstractFieldsetPanel<?> addAddressText(final String addressType, final String addressTextProperty)
  {
    final FieldProperties<String> props = getAddressTextProperties(addressType, addressTextProperty);
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    PFAutoCompleteTextField<String> textField = (PFAutoCompleteTextField<String>) new PFAutoCompleteMaxLengthTextField(fs.getTextFieldId(),
        props.getPropertyModel())
    {
      @Override
      protected List<String> getChoices(final String input)
      {
        return addressDao.getAutocompletion(addressTextProperty, input);
      }
    }.withMatchContains(true).withMinChars(2).setMarkupId("addressText").setOutputMarkupId(true);
    fs.add(textField);
    return fs;
  }

  public FieldProperties<String> getAddressTextProperties(final String addressType, final String addressTextProperty)
  {
    return new FieldProperties<String>("address.addressText", new PropertyModel<String>(address, addressTextProperty))
        .setLabelDescription(
            addressType, false);
  }

  public AbstractFieldsetPanel<?> addZipCode(final String zipCodeProperty)
  {
    final FieldProperties<String> props = getZipCodeProperties(zipCodeProperty);
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    MaxLengthTextField textField = new MaxLengthTextField(fs.getTextFieldId(), props.getModel());
    textField.setMarkupId("zipCode").setOutputMarkupId(true);
    fs.add(textField);
    return fs;
  }

  public FieldProperties<String> getZipCodeProperties(final String zipCodeProperty)
  {
    return new FieldProperties<String>("address.zipCode", new PropertyModel<String>(address, zipCodeProperty));
  }

  public AbstractFieldsetPanel<?> addCity(final String cityProperty)
  {
    final FieldProperties<String> props = getCityProperties(cityProperty);
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    MaxLengthTextField textField = new MaxLengthTextField(fs.getTextFieldId(), props.getModel());
    textField.setMarkupId("city").setOutputMarkupId(true);
    fs.add(textField);
    return fs;
  }

  public FieldProperties<String> getCityProperties(final String cityProperty)
  {
    return new FieldProperties<String>("address.city", new PropertyModel<String>(address, cityProperty));
  }

  public AbstractFieldsetPanel<?> addCountry(final String countryProperty)
  {
    final FieldProperties<String> props = getCountryProperties(countryProperty);
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    MaxLengthTextField textField = new MaxLengthTextField(fs.getTextFieldId(), props.getModel());
    textField.setMarkupId("country").setOutputMarkupId(true);
    fs.add(textField);
    return fs;
  }

  public FieldProperties<String> getCountryProperties(final String countryProperty)
  {
    return new FieldProperties<String>("address.country", new PropertyModel<String>(address, countryProperty));
  }

  public AbstractFieldsetPanel<?> addState(final String stateProperty)
  {
    final FieldProperties<String> props = getStateProperties(stateProperty);
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    MaxLengthTextField textField = new MaxLengthTextField(fs.getTextFieldId(), props.getModel());
    textField.setMarkupId("state").setOutputMarkupId(true);
    fs.add(textField);
    return fs;
  }

  public FieldProperties<String> getStateProperties(final String stateProperty)
  {
    return new FieldProperties<String>("address.state", new PropertyModel<String>(address, stateProperty));
  }

  public String getString(final String key)
  {
    return gridBuilder.getString(key);
  }

  /**
   * @return true, if id of address is null (id not yet exists).
   */
  public boolean isNew()
  {
    return address.getId() == null;
  }

  public AddressParameters getBusinessAddressParameters()
  {
    return new AddressParameters(getString("address.heading.businessAddress"), "addressText", "zipCode", "city",
        "country", "state");
  }

  public AddressParameters getPostalAddressParameters()
  {
    return new AddressParameters(getString("address.heading.postalAddress"), "postalAddressText", "postalZipCode",
        "postalCity",
        "postalCountry", "postalState");
  }

  public AddressParameters getPrivateAddressParameters()
  {
    return new AddressParameters(getString("address.heading.privateAddress"), "privateAddressText", "privateZipCode",
        "privateCity",
        "privateCountry", "privateState");
  }

  class AddressParameters
  {
    AddressParameters(final String addressType, final String addressTextProperty, final String zipCodeProperty,
        final String cityProperty,
        final String countryProperty, final String stateProperty)
    {
      this.addressType = addressType;
      this.addressTextProperty = addressTextProperty;
      this.zipCodeProperty = zipCodeProperty;
      this.cityProperty = cityProperty;
      this.countryProperty = countryProperty;
      this.stateProperty = stateProperty;
    }

    final String addressType, addressTextProperty, zipCodeProperty, cityProperty, countryProperty, stateProperty;
  };
}
