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

import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the edit formular page.
 * 
 * @author Werner Feder (werner.feder@t-online.de)
 */
public class ContactEditForm extends AbstractEditForm<ContactDO, ContactEditPage>
{

  private static final long serialVersionUID = 7930242750045989712L;

  private static final Logger log = LoggerFactory.getLogger(ContactEditForm.class);

  @SpringBean
  private ContactDao contactDao;

  @SpringBean
  private PersonalContactDao personalContactDao;

  protected ContactPageSupport contactEditSupport;

  /**
   * @param parentPage
   * @param data
   */
  public ContactEditForm(final ContactEditPage parentPage, final ContactDO data)
  {
    super(parentPage, data);
  }

  @Override
  public void init()
  {
    super.init();
    contactEditSupport = new ContactPageSupport(this, gridBuilder, (ContactDao) getBaseDao(), personalContactDao, data);

    gridBuilder.newSplitPanel(GridSize.COL75);

    contactEditSupport.addName();
    contactEditSupport.addFirstName();
    final FieldsetPanel fs = (FieldsetPanel) contactEditSupport.addFormOfAddress();
    //    final DivPanel checkBoxPanel = fs.addNewCheckBoxDiv();
    //    checkBoxPanel.addCheckBox(new PropertyModel<Boolean>(contactEditSupport.personalContact, "favoriteCard"), getString("favorite"),
    final DivPanel checkBoxPanel = fs.addNewCheckBoxButtonDiv();
    checkBoxPanel.addCheckBoxButton(new PropertyModel<Boolean>(contactEditSupport.personalContact, "favoriteCard"),
        getString("favorite"),
        getString("address.tooltip.vCardList"));
    contactEditSupport.addTitle();
    contactEditSupport.addOrganization();
    contactEditSupport.addDivision();
    contactEditSupport.addPosition();
    contactEditSupport.addWebsite();

    contactEditSupport.addBirthday();
    contactEditSupport.addLanguage();
    contactEditSupport.addContactStatus();
    contactEditSupport.addAddressStatus();

    contactEditSupport.addFingerPrint();
    contactEditSupport.addPublicKey();

    contactEditSupport.addComment();

    // Emails
    FieldsetPanel fs2 = gridBuilder.newFieldset(ContactDO.class, "emailValues").suppressLabelForWarning();
    fs2.add(new EmailsPanel(fs.newChildId(), new PropertyModel<String>(data, "emailValues")));

    // Phones
    fs2 = gridBuilder.newFieldset(ContactDO.class, "phoneValues").suppressLabelForWarning();
    fs2.add(new PhonesPanel(fs.newChildId(), new PropertyModel<String>(data, "phoneValues")));

    // Instant Messaging Entries
    fs2 = gridBuilder.newFieldset(ContactDO.class, "socialMediaValues").suppressLabelForWarning();
    fs2.add(new SocialMediaPanel(fs.newChildId(), new CompoundPropertyModel<ContactDO>(data)));

    // Contacts
    fs2 = gridBuilder.newFieldset(ContactDO.class, "contacts").suppressLabelForWarning();
    fs2.add(new ContactEntryPanel(fs.newChildId(), new CompoundPropertyModel<ContactDO>(data)));

  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

}
