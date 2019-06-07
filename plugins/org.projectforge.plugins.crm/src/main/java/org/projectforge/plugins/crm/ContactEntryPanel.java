/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.crm;

import java.util.Iterator;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.web.wicket.components.AjaxMaxLengthEditableLabel;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.AjaxIconLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ContactEntryPanel extends Panel
{
  private static final long serialVersionUID = -7234382706624510638L;

  private RepeatingView entrysRepeater;

  private WebMarkupContainer mainContainer, addNewEntryContainer;

  private LabelValueChoiceRenderer<ContactType> formChoiceRenderer;

  private ContactEntryDO newEntryValue;

  private final String DEFAULT_ENTRY_VALUE = "Neue Adresse";
  private final String DEFAULT_STREET_VALUE = "Strasse";
  private final String DEFAULT_ZIPCODE_VALUE = "Plz";
  private final String DEFAULT_CITY_VALUE = "Stadt";
  private final String DEFAULT_COUNTRY_VALUE = "Land";
  private final String DEFAULT_STATE_VALUE = "Bundesland";

  private Component city;
  private Component zipCode;
  private Component country;
  private Component state;
  private Component delete;

  private final IModel<ContactDO> model;

  /**
   * @param id
   */
  public ContactEntryPanel(final String id, final IModel<ContactDO> model)
  {
    super(id);
    this.model = model;
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    newEntryValue = new ContactEntryDO().setStreet(DEFAULT_ENTRY_VALUE).setCity(DEFAULT_CITY_VALUE) //
        .setZipCode(DEFAULT_ZIPCODE_VALUE).setCountry(DEFAULT_COUNTRY_VALUE).setState(DEFAULT_STATE_VALUE)
        .setContactType(ContactType.PRIVATE) //
        .setContact(model.getObject());
    formChoiceRenderer = new LabelValueChoiceRenderer<ContactType>(this, ContactType.values());
    mainContainer = new WebMarkupContainer("main");
    add(mainContainer.setOutputMarkupId(true));
    entrysRepeater = new RepeatingView("liRepeater");
    mainContainer.add(entrysRepeater);

    rebuildEntrys();
    addNewEntryContainer = new WebMarkupContainer("liAddNewEntry");
    mainContainer.add(addNewEntryContainer);

    init(addNewEntryContainer);
    entrysRepeater.setVisible(true);
  }

  /********************************** init ** ********************************* */
  @SuppressWarnings("serial")
  void init(final WebMarkupContainer item)
  {
    final DropDownChoice<ContactType> dropdownChoice = new DropDownChoice<ContactType>("choice",
        new PropertyModel<ContactType>(
            newEntryValue, "contactType"),
        formChoiceRenderer.getValues(), formChoiceRenderer);
    item.add(dropdownChoice);
    dropdownChoice.add(new AjaxFormComponentUpdatingBehavior("change")
    {
      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        newEntryValue.setContactType(dropdownChoice.getModelObject());
      }
    });

    final WebMarkupContainer streetCodeDiv = new WebMarkupContainer("streetCodeDiv");
    streetCodeDiv.setOutputMarkupId(true);
    streetCodeDiv.add(new AjaxMaxLengthEditableLabel("street", new PropertyModel<String>(newEntryValue, "street"))
    {
      /**
       * @see org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel#onEdit(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      public void onEdit(final AjaxRequestTarget target)
      {
        super.onEdit(target);
        if (newEntryValue.getStreet().equals(DEFAULT_ENTRY_VALUE) == true)
          newEntryValue.setStreet(DEFAULT_STREET_VALUE);
      }

      /**
       * @see org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel#onSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected void onSubmit(final AjaxRequestTarget target)
      {
        super.onSubmit(target);
        zipCode.setVisible(true);
        target.add(mainContainer);
      }
    }).setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setVisible(true);
    item.add(streetCodeDiv);

    final WebMarkupContainer zipCodeDiv = new WebMarkupContainer("zipCodeDiv");
    zipCodeDiv.setOutputMarkupId(true);
    zipCodeDiv
        .add(zipCode = new AjaxMaxLengthEditableLabel("zipCode", new PropertyModel<String>(newEntryValue, "zipCode"))
        {
          /**
           * @see org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel#onSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
           */
          @Override
          protected void onSubmit(final AjaxRequestTarget target)
          {
            super.onSubmit(target);
            city.setVisible(true);
            target.add(mainContainer);
          }
        }.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setVisible(false));
    item.add(zipCodeDiv);

    final WebMarkupContainer cityDiv = new WebMarkupContainer("cityDiv");
    cityDiv.setOutputMarkupId(true);
    cityDiv.add(city = new AjaxMaxLengthEditableLabel("city", new PropertyModel<String>(newEntryValue, "city"))
    {
      /**
       * @see org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel#onSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected void onSubmit(final AjaxRequestTarget target)
      {
        super.onSubmit(target);
        country.setVisible(true);
        target.add(mainContainer);
      }
    }.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setVisible(false));
    item.add(cityDiv);

    final WebMarkupContainer countryDiv = new WebMarkupContainer("countryDiv");
    countryDiv.setOutputMarkupId(true);
    countryDiv
        .add(country = new AjaxMaxLengthEditableLabel("country", new PropertyModel<String>(newEntryValue, "country"))
        {
          /**
           * @see org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel#onSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
           */
          @Override
          protected void onSubmit(final AjaxRequestTarget target)
          {
            super.onSubmit(target);
            state.setVisible(true);
            target.add(mainContainer);
          }
        }.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setVisible(false));
    item.add(countryDiv);

    final WebMarkupContainer stateDiv = new WebMarkupContainer("stateDiv");
    stateDiv.setOutputMarkupId(true);
    stateDiv.add(state = new AjaxMaxLengthEditableLabel("state", new PropertyModel<String>(newEntryValue, "state"))
    {
      /**
       * @see org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel#onSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected void onSubmit(final AjaxRequestTarget target)
      {
        super.onSubmit(target);
        model.getObject()
            .addContactEntry(new ContactEntryDO().setStreet(newEntryValue.getStreet()).setCity(newEntryValue.getCity()) //
                .setZipCode(newEntryValue.getZipCode()).setCountry(newEntryValue.getCountry()) //
                .setState(newEntryValue.getState()).setContactType(newEntryValue.getContactType()));
        rebuildEntrys();
        newEntryValue.setStreet(DEFAULT_ENTRY_VALUE).setCity(DEFAULT_CITY_VALUE).setZipCode(DEFAULT_ZIPCODE_VALUE) //
            .setCountry(DEFAULT_COUNTRY_VALUE).setState(DEFAULT_STATE_VALUE).setContactType(ContactType.PRIVATE);
        target.add(mainContainer);
        zipCode.setVisible(false);
        city.setVisible(false);
        country.setVisible(false);
        state.setVisible(false);
      }
    }.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setVisible(false));
    item.add(stateDiv);

    final WebMarkupContainer deleteDiv = new WebMarkupContainer("deleteDiv");
    deleteDiv.setOutputMarkupId(true);
    deleteDiv.add(
        delete = new AjaxIconLinkPanel("delete", IconType.REMOVE, new PropertyModel<String>(newEntryValue, "street"))
        {
          /**
           * @see org.projectforge.web.wicket.flowlayout.AjaxIconLinkPanel#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
           */
          @Override
          protected void onClick(final AjaxRequestTarget target)
          {
            super.onClick(target);
            final Iterator<ContactEntryDO> it = model.getObject().getContactEntries().iterator();
            while (it.hasNext() == true) {
              if (it.next() == newEntryValue) {
                it.remove();
              }
            }
            rebuildEntrys();
            target.add(mainContainer);
          }
        });
    item.add(deleteDiv);
    delete.setVisible(false);
  }

  /********************************** rebuild ** ********************************* */
  @SuppressWarnings("serial")
  private void rebuildEntrys()
  {

    final Set<ContactEntryDO> entries = model.getObject().getContactEntries();
    if (entries != null) {
      entrysRepeater.removeAll();

      for (final ContactEntryDO entry : entries) {

        final WebMarkupContainer item = new WebMarkupContainer(entrysRepeater.newChildId());
        entrysRepeater.add(item);
        final DropDownChoice<ContactType> dropdownChoice = new DropDownChoice<ContactType>("choice",
            new PropertyModel<ContactType>(entry,
                "contactType"),
            formChoiceRenderer.getValues(), formChoiceRenderer);
        item.add(dropdownChoice);
        dropdownChoice.add(new AjaxFormComponentUpdatingBehavior("change")
        {
          @Override
          protected void onUpdate(final AjaxRequestTarget target)
          {
            entry.setContactType(dropdownChoice.getModelObject());
          }
        });

        final WebMarkupContainer streetCodeDiv = new WebMarkupContainer("streetCodeDiv");
        streetCodeDiv.setOutputMarkupId(true);
        streetCodeDiv.add(new AjaxMaxLengthEditableLabel("street", new PropertyModel<String>(entry, "street"))
        {
          /**
           * @see org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel#onEdit(org.apache.wicket.ajax.AjaxRequestTarget)
           */
          @Override
          public void onEdit(final AjaxRequestTarget target)
          {
            super.onEdit(target);
            if (newEntryValue.getStreet().equals(DEFAULT_ENTRY_VALUE) == true)
              newEntryValue.setStreet(DEFAULT_STREET_VALUE);
          }
        }).setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setVisible(true);
        item.add(streetCodeDiv);

        final WebMarkupContainer zipCodeDiv = new WebMarkupContainer("zipCodeDiv");
        zipCodeDiv.setOutputMarkupId(true);
        zipCodeDiv.add(new AjaxMaxLengthEditableLabel("zipCode", new PropertyModel<String>(entry, "zipCode"))
            .setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setVisible(true));
        item.add(zipCodeDiv);

        final WebMarkupContainer cityDiv = new WebMarkupContainer("cityDiv");
        cityDiv.setOutputMarkupId(true);
        cityDiv.add(new AjaxMaxLengthEditableLabel("city", new PropertyModel<String>(entry, "city"))
            .setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setVisible(true));
        item.add(cityDiv);

        final WebMarkupContainer countryDiv = new WebMarkupContainer("countryDiv");
        countryDiv.setOutputMarkupId(true);
        countryDiv.add(new AjaxMaxLengthEditableLabel("country", new PropertyModel<String>(entry, "country"))
            .setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setVisible(true));
        item.add(countryDiv);

        final WebMarkupContainer stateDiv = new WebMarkupContainer("stateDiv");
        stateDiv.setOutputMarkupId(true);
        stateDiv.add(new AjaxMaxLengthEditableLabel("state", new PropertyModel<String>(entry, "state"))
        {
          /**
           * @see org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel#onSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
           */
          @Override
          protected void onSubmit(final AjaxRequestTarget target)
          {
            super.onSubmit(target);
            rebuildEntrys();
            target.add(mainContainer);
          }
        }.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setVisible(true));
        item.add(stateDiv);

        final WebMarkupContainer deleteDiv = new WebMarkupContainer("deleteDiv");
        deleteDiv.setOutputMarkupId(true);
        deleteDiv.add(new AjaxIconLinkPanel("delete", IconType.REMOVE, new PropertyModel<String>(entry, "street"))
        {
          /**
           * @see org.projectforge.web.wicket.flowlayout.AjaxIconLinkPanel#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
           */
          @Override
          protected void onClick(final AjaxRequestTarget target)
          {
            super.onClick(target);
            final Iterator<ContactEntryDO> it = model.getObject().getContactEntries().iterator();
            while (it.hasNext() == true) {
              if (it.next() == entry) {
                it.remove();
              }
            }
            rebuildEntrys();
            target.add(mainContainer);
          }
        });
        item.add(deleteDiv);
      }
    }
  }
}
