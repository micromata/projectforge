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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.address.AddressDO;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
public class AddressCompareForm extends AbstractEditForm<AddressDO, AddressComparePage>
{

  private static final long serialVersionUID = 4990179617114170795L;
  private final AddressDO dataOld;

  public AddressCompareForm(final AddressComparePage parentPage, final AddressDO dataNew, final AddressDO dataOld)
  {
    super(parentPage, dataOld);
    this.dataOld = dataOld;
    data = dataNew;
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    feedbackPanel.error(getString("address.book.vCardImport.existingEntry"));

    // new address
    gridBuilder.newSplitPanel(GridSize.COL50);

    initFields(dataOld, false);

    // existing address
    if (data != null) {
      gridBuilder.newSplitPanel(GridSize.COL50);
      initFields(data, true);
    }
  }

  private void initFields(final AddressDO address, final boolean enabled)
  {
    final FieldsetPanel fsName = gridBuilder.newFieldset("Name");
    fsName.add(new TextField<String>(fsName.getTextFieldId(), new PropertyModel<String>(address, "name")));
    fsName.setEnabled(enabled);
    if (StringUtils.equals(dataOld.getName(), data.getName()) == true) {
      setMark(fsName, enabled);
    }

    final FieldsetPanel fsFirstName = gridBuilder.newFieldset("FirstName");
    fsFirstName.add(new TextField<String>(fsFirstName.getTextFieldId(), new PropertyModel<String>(address, "firstName")));
    fsFirstName.setEnabled(enabled);
    if (StringUtils.equals(dataOld.getFirstName(), data.getFirstName()) == true) {
      setMark(fsFirstName, enabled);
    }

    final FieldsetPanel fsEmailBusiness = gridBuilder.newFieldset("Mail gesch.");
    fsEmailBusiness.add(new TextField<String>(fsEmailBusiness.getTextFieldId(), new PropertyModel<String>(address, "email")));
    fsEmailBusiness.setEnabled(enabled);
    if (StringUtils.equals(dataOld.getEmail(), data.getEmail()) == true) {
      setMark(fsEmailBusiness, enabled);
    }

    final FieldsetPanel fsBusinessPhone = gridBuilder.newFieldset("Tele gesch.");
    fsBusinessPhone.add(new TextField<String>(fsBusinessPhone.getTextFieldId(), new PropertyModel<String>(address, "businessPhone")));
    fsBusinessPhone.setEnabled(enabled);
    if (StringUtils.equals(dataOld.getBusinessPhone(), data.getBusinessPhone()) == true) {
      setMark(fsBusinessPhone, enabled);
    }

    final FieldsetPanel fsMobilBusiness = gridBuilder.newFieldset("Mobil gesch.");
    fsMobilBusiness.add(new TextField<String>(fsMobilBusiness.getTextFieldId(), new PropertyModel<String>(address, "mobilePhone")));
    fsMobilBusiness.setEnabled(enabled);
    if (StringUtils.equals(dataOld.getMobilePhone(), data.getMobilePhone()) == true) {
      setMark(fsMobilBusiness, enabled);
    }

    final FieldsetPanel fsEmailPrivate = gridBuilder.newFieldset("Mail priv.");
    fsEmailPrivate.add(new TextField<String>(fsEmailPrivate.getTextFieldId(), new PropertyModel<String>(address, "privateEmail")));
    fsEmailPrivate.setEnabled(enabled);
    if (StringUtils.equals(dataOld.getPrivateEmail(), data.getPrivateEmail()) == true) {
      setMark(fsEmailPrivate, enabled);
    }

    final FieldsetPanel fsPrivatePhone = gridBuilder.newFieldset("Tele priv.");
    fsPrivatePhone.add(new TextField<String>(fsPrivatePhone.getTextFieldId(), new PropertyModel<String>(address, "privatePhone")));
    fsPrivatePhone.setEnabled(enabled);
    if (StringUtils.equals(dataOld.getPrivatePhone(), data.getPrivatePhone()) == true) {
      setMark(fsPrivatePhone, enabled);
    }

    final FieldsetPanel fsMobilPrivate = gridBuilder.newFieldset("Mobil priv.");
    fsMobilPrivate.add(new TextField<String>(fsMobilPrivate.getTextFieldId(), new PropertyModel<String>(address, "privateMobilePhone")));
    fsMobilPrivate.setEnabled(enabled);
    if (StringUtils.equals(dataOld.getPrivateMobilePhone(), data.getPrivateMobilePhone()) == true) {
      setMark(fsMobilPrivate, enabled);
    }
  }

  /**
   * mark panel, if content already exist.
   *
   * @param panel
   */
  private void setMark(final FieldsetPanel panel, final boolean paint)
  {
    // TODO sn migration wicket 8 use corret ids here!!
    if (paint == true) {
      panel.getFieldset().get("this will throw an exeption!").add(new AttributeModifier("style",
          new Model<String>(
              "background: #FCF8E3; border: 1px solid #FBEED5; border-radius: 4px; padding-left: 5px; padding-right: 10px; padding-bottom: 4px;")));
    } else {
      panel.getFieldset().get("this will throw an exeption!").add(new AttributeModifier("style",
          new Model<String>("border: 1px solid whiteSmoke; border-radius: 4px; padding-left: 5px; padding-right: 10px; padding-bottom: 4px;")));
    }
  }

  /**
   *
   */
  public void create()
  {
    data.setId(dataOld.getId());
    getBaseDao().save(data);
    setResponsePage(AddressListPage.class);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return null;
  }

}
