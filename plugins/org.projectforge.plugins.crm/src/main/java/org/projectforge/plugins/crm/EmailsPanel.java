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
import org.projectforge.web.wicket.components.AjaxMaxLengthEditableLabel;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.AjaxIconLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class EmailsPanel extends Panel
{
  private static final long serialVersionUID = -7950224503861575606L;

  @SpringBean
  private ContactDao contactDao;

  private List<EmailValue> emails = null;

  private RepeatingView emailsRepeater;

  private WebMarkupContainer mainContainer, addNewEMailContainer;

  private LabelValueChoiceRenderer<ContactType> emailChoiceRenderer;

  private EmailValue newEmailValue;

  private final String DEFAULT_EMAIL_VALUE = "E-Mail";

  private Component delete;

  private final PropertyModel<String> model;

  /**
   * @param id
   */
  public EmailsPanel(final String id, final PropertyModel<String> model)
  {
    super(id);
    this.model = model;
    if (StringUtils.isNotBlank(model.getObject()) == true) {
      emails = contactDao.readEmailValues(model.getObject());
    }
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    if (emails == null) {
      emails = new ArrayList<EmailValue>();
    }
    newEmailValue = new EmailValue().setEmail(DEFAULT_EMAIL_VALUE).setContactType(ContactType.PRIVATE);
    emailChoiceRenderer = new LabelValueChoiceRenderer<ContactType>(this, ContactType.values());
    mainContainer = new WebMarkupContainer("main");
    add(mainContainer.setOutputMarkupId(true));
    emailsRepeater = new RepeatingView("liRepeater");
    mainContainer.add(emailsRepeater);

    rebuildEmails();
    addNewEMailContainer = new WebMarkupContainer("liAddNewEmail");
    mainContainer.add(addNewEMailContainer);

    init(addNewEMailContainer);
    emailsRepeater.setVisible(true);
  }

  @SuppressWarnings("serial")
  void init(final WebMarkupContainer item)
  {
    final DropDownChoice<ContactType> dropdownChoice = new DropDownChoice<ContactType>("choice",
        new PropertyModel<ContactType>(
            newEmailValue, "contactType"),
        emailChoiceRenderer.getValues(), emailChoiceRenderer);
    item.add(dropdownChoice);
    dropdownChoice.add(new AjaxFormComponentUpdatingBehavior("change")
    {
      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        newEmailValue.setContactType(dropdownChoice.getModelObject());
        model.setObject(contactDao.getEmailValuesAsXml(emails));
      }
    });

    item.add(new AjaxMaxLengthEditableLabel("editableLabel", new PropertyModel<String>(newEmailValue, "email"))
    {
      @Override
      protected void onSubmit(final AjaxRequestTarget target)
      {
        super.onSubmit(target);
        if (StringUtils.isNotBlank(newEmailValue.getEmail()) == true
            && newEmailValue.getEmail().equals(DEFAULT_EMAIL_VALUE) == false) {
          emails
              .add(new EmailValue().setEmail(newEmailValue.getEmail()).setContactType(newEmailValue.getContactType()));
          model.setObject(contactDao.getEmailValuesAsXml(emails));
        }
        newEmailValue.setEmail(DEFAULT_EMAIL_VALUE);
        rebuildEmails();
        target.add(mainContainer);
      }
    });

    final WebMarkupContainer deleteDiv = new WebMarkupContainer("deleteDiv");
    deleteDiv.setOutputMarkupId(true);
    deleteDiv.add(
        delete = new AjaxIconLinkPanel("delete", IconType.REMOVE, new PropertyModel<String>(newEmailValue, "email"))
        {
          /**
           * @see org.projectforge.web.wicket.flowlayout.AjaxIconLinkPanel#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
           */
          @Override
          protected void onClick(final AjaxRequestTarget target)
          {
            super.onClick(target);
            final Iterator<EmailValue> it = emails.iterator();
            while (it.hasNext() == true) {
              if (it.next() == newEmailValue) {
                it.remove();
              }
            }
            rebuildEmails();
            model.setObject(contactDao.getEmailValuesAsXml(emails));
            target.add(mainContainer);
          }
        });
    item.add(deleteDiv);
    delete.setVisible(false);
  }

  @SuppressWarnings("serial")
  private void rebuildEmails()
  {
    emailsRepeater.removeAll();
    for (final EmailValue email : emails) {

      final WebMarkupContainer item = new WebMarkupContainer(emailsRepeater.newChildId());
      emailsRepeater.add(item);
      final DropDownChoice<ContactType> dropdownChoice = new DropDownChoice<ContactType>("choice",
          new PropertyModel<ContactType>(email,
              "contactType"),
          emailChoiceRenderer.getValues(), emailChoiceRenderer);
      item.add(dropdownChoice);
      dropdownChoice.add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          email.setContactType(dropdownChoice.getModelObject());
          model.setObject(contactDao.getEmailValuesAsXml(emails));
        }
      });

      item.add(new AjaxMaxLengthEditableLabel("editableLabel", new PropertyModel<String>(email, "email"))
      {
        @Override
        protected void onSubmit(final AjaxRequestTarget target)
        {
          super.onSubmit(target);
          model.setObject(contactDao.getEmailValuesAsXml(emails));
          rebuildEmails();
          target.add(mainContainer);
        }
      });

      final WebMarkupContainer deleteDiv = new WebMarkupContainer("deleteDiv");
      deleteDiv.setOutputMarkupId(true);
      deleteDiv.add(new AjaxIconLinkPanel("delete", IconType.REMOVE, new PropertyModel<String>(email, "email"))
      {
        /**
         * @see org.projectforge.web.wicket.flowlayout.AjaxIconLinkPanel#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
         */
        @Override
        protected void onClick(final AjaxRequestTarget target)
        {
          super.onClick(target);
          final Iterator<EmailValue> it = emails.iterator();
          while (it.hasNext() == true) {
            if (it.next() == email) {
              it.remove();
            }
          }
          rebuildEmails();
          model.setObject(contactDao.getEmailValuesAsXml(emails));
          target.add(mainContainer);
        }
      });
      item.add(deleteDiv);
    }
  }
}
