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

package org.projectforge.plugins.crm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.PhoneType;
import org.projectforge.web.wicket.components.AjaxMaxLengthEditableLabel;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.AjaxIconLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 */
public class PhonesPanel extends Panel
{

  private static final long serialVersionUID = -5390479088481704778L;

  @SpringBean
  private ContactDao contactDao;

  private List<PhoneValue> phones = null;

  private RepeatingView phonesRepeater;

  private WebMarkupContainer mainContainer, addNewPhoneContainer;

  private LabelValueChoiceRenderer<PhoneType> phoneChoiceRenderer;

  private PhoneValue newPhoneValue;

  private final String DEFAULT_PHONE_VALUE = "Telefon";

  private Component delete;

  private final PropertyModel<String> model;

  /**
   * @param id
   */
  public PhonesPanel(final String id, final PropertyModel<String> model)
  {
    super(id);
    this.model = model;
    if (StringUtils.isNotBlank(model.getObject()) == true) {
      phones = contactDao.readPhoneValues(model.getObject());
    }
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();

    if (phones == null) {
      phones = new ArrayList<PhoneValue>();
    }
    newPhoneValue = new PhoneValue().setNumber(DEFAULT_PHONE_VALUE).setPhoneType(PhoneType.PRIVATE);
    phoneChoiceRenderer = new LabelValueChoiceRenderer<PhoneType>(this, PhoneType.values());
    mainContainer = new WebMarkupContainer("main");
    add(mainContainer.setOutputMarkupId(true));
    phonesRepeater = new RepeatingView("liRepeater");
    mainContainer.add(phonesRepeater);

    rebuildPhones();
    addNewPhoneContainer = new WebMarkupContainer("liAddNewPhone");
    mainContainer.add(addNewPhoneContainer);

    init(addNewPhoneContainer);
    phonesRepeater.setVisible(true);
  }

  @SuppressWarnings("serial")
  void init(final WebMarkupContainer item)
  {
    final DropDownChoice<PhoneType> dropdownChoice = new DropDownChoice<PhoneType>("choice",
        new PropertyModel<PhoneType>(
            newPhoneValue, "phoneType"),
        phoneChoiceRenderer.getValues(), phoneChoiceRenderer);
    item.add(dropdownChoice);
    dropdownChoice.add(new AjaxFormComponentUpdatingBehavior("change")
    {
      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        newPhoneValue.setPhoneType(dropdownChoice.getModelObject());
        model.setObject(contactDao.getPhoneValuesAsXml(phones));
      }
    });

    item.add(new AjaxMaxLengthEditableLabel("editableLabel", new PropertyModel<String>(newPhoneValue, "number"))
    {
      @Override
      protected void onSubmit(final AjaxRequestTarget target)
      {
        super.onSubmit(target);
        if (StringUtils.isNotBlank(newPhoneValue.getNumber()) == true
            && newPhoneValue.getNumber().equals(DEFAULT_PHONE_VALUE) == false) {
          phones.add(new PhoneValue().setNumber(newPhoneValue.getNumber()).setPhoneType(newPhoneValue.getPhoneType()));
          model.setObject(contactDao.getPhoneValuesAsXml(phones));
        }
        newPhoneValue.setNumber(DEFAULT_PHONE_VALUE);
        rebuildPhones();
        target.add(mainContainer);
      }
    });

    final WebMarkupContainer deleteDiv = new WebMarkupContainer("deleteDiv");
    deleteDiv.setOutputMarkupId(true);
    deleteDiv.add(
        delete = new AjaxIconLinkPanel("delete", IconType.REMOVE, new PropertyModel<String>(newPhoneValue, "number"))
        {
          /**
           * @see org.projectforge.web.wicket.flowlayout.AjaxIconLinkPanel#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
           */
          @Override
          protected void onClick(final AjaxRequestTarget target)
          {
            super.onClick(target);
            final Iterator<PhoneValue> it = phones.iterator();
            while (it.hasNext() == true) {
              if (it.next() == newPhoneValue) {
                it.remove();
              }
            }
            rebuildPhones();
            model.setObject(contactDao.getPhoneValuesAsXml(phones));
            target.add(mainContainer);
          }
        });
    item.add(deleteDiv);
    delete.setVisible(false);
  }

  @SuppressWarnings("serial")
  private void rebuildPhones()
  {
    phonesRepeater.removeAll();
    for (final PhoneValue phone : phones) {

      final WebMarkupContainer item = new WebMarkupContainer(phonesRepeater.newChildId());
      phonesRepeater.add(item);
      final DropDownChoice<PhoneType> dropdownChoice = new DropDownChoice<PhoneType>("choice",
          new PropertyModel<PhoneType>(phone,
              "phoneType"),
          phoneChoiceRenderer.getValues(), phoneChoiceRenderer);
      item.add(dropdownChoice);
      dropdownChoice.add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          phone.setPhoneType(dropdownChoice.getModelObject());
          model.setObject(contactDao.getPhoneValuesAsXml(phones));
        }
      });

      item.add(new AjaxMaxLengthEditableLabel("editableLabel", new PropertyModel<String>(phone, "number"))
      {
        @Override
        protected void onSubmit(final AjaxRequestTarget target)
        {
          super.onSubmit(target);
          model.setObject(contactDao.getPhoneValuesAsXml(phones));
          rebuildPhones();
          target.add(mainContainer);
        }
      });

      final WebMarkupContainer deleteDiv = new WebMarkupContainer("deleteDiv");
      deleteDiv.setOutputMarkupId(true);
      deleteDiv.add(new AjaxIconLinkPanel("delete", IconType.REMOVE, new PropertyModel<String>(phone, "number"))
      {
        /**
         * @see org.projectforge.web.wicket.flowlayout.AjaxIconLinkPanel#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
         */
        @Override
        protected void onClick(final AjaxRequestTarget target)
        {
          super.onClick(target);
          final Iterator<PhoneValue> it = phones.iterator();
          while (it.hasNext() == true) {
            if (it.next() == phone) {
              it.remove();
            }
          }
          rebuildPhones();
          model.setObject(contactDao.getPhoneValuesAsXml(phones));
          target.add(mainContainer);
        }
      });
      item.add(deleteDiv);
    }
  }
}
