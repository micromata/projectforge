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

package org.projectforge.plugins.marketing;

import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;
import org.slf4j.Logger;

/**
 * This is the edit formular page.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class AddressCampaignValueEditForm extends AbstractEditForm<AddressCampaignValueDO, AddressCampaignValueEditPage>
{
  private static final long serialVersionUID = -6208809585214296635L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(AddressCampaignValueEditForm.class);

  @SpringBean
  private AddressCampaignValueDao addressCampaignValueDao;

  public AddressCampaignValueEditForm(final AddressCampaignValueEditPage parentPage, final AddressCampaignValueDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected void updateButtonVisibility()
  {
    super.updateButtonVisibility();
    updateAndNextButtonPanel
        .setVisible(addressCampaignValueDao.hasLoggedInUserUpdateAccess(getData(), getData(), false));
  }

  @Override
  protected void init()
  {
    super.init();
    {
      // Heading
      gridBuilder.newFormHeading(
          getString("plugins.marketing.addressCampaignValue") + ": " + data.getAddressCampaign().getTitle());
    }
    {
      // Name
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("name")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), data.getAddress().getFullName()));
    }
    {
      // Organization
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("organization")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), data.getAddress().getOrganization()));
    }
    {
      // Value
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("value"));
      final AddressCampaignDO addressCampaign = data.getAddressCampaign();
      final LabelValueChoiceRenderer<String> valueChoiceRenderer = new LabelValueChoiceRenderer<>(
          addressCampaign.getValuesArray());
      fs.addDropDownChoice(new PropertyModel<>(data, "value"), valueChoiceRenderer.getValues(),
          valueChoiceRenderer).setNullValid(
              true);
    }
    {
      // Comment
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("comment"));
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<>(data, "comment"))).setAutogrow();
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
