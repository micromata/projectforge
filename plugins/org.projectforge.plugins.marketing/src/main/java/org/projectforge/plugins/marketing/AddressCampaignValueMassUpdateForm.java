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
import org.projectforge.web.wicket.AbstractMassEditForm;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class AddressCampaignValueMassUpdateForm
    extends AbstractMassEditForm<AddressCampaignValueDO, AddressCampaignValueMassUpdatePage>
{
  private static final long serialVersionUID = -6785832818308468337L;

  protected AddressCampaignValueDO data;

  public AddressCampaignValueMassUpdateForm(final AddressCampaignValueMassUpdatePage parentPage,
      final AddressCampaignDO addressCampaign)
  {
    super(parentPage);
    data = new AddressCampaignValueDO();
    data.setAddressCampaign(addressCampaign);
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
      // Value
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("value"));
      final AddressCampaignDO addressCampaign = data.getAddressCampaign();
      final LabelValueChoiceRenderer<String> valueChoiceRenderer = new LabelValueChoiceRenderer<>(
          addressCampaign.getValuesArray());
      fs.addDropDownChoice(new PropertyModel<>(data, "value"), valueChoiceRenderer.getValues(),
          valueChoiceRenderer).setNullValid(
              false);
    }
    {
      // Comment
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("comment"));
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<>(data, "comment"))).setAutogrow();
    }
  }
}
