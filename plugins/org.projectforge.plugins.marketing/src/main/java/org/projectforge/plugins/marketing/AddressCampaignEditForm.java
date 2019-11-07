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

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidator;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;

/**
 * This is the edit formular page.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class AddressCampaignEditForm extends AbstractEditForm<AddressCampaignDO, AddressCampaignEditPage>
{
  private static final long serialVersionUID = -6208809585214296635L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddressCampaignEditForm.class);

  private TextField<String> valuesField;

  public AddressCampaignEditForm(final AddressCampaignEditPage parentPage, final AddressCampaignDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    {
      // Title
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("title"));
      fs.add(new RequiredMaxLengthTextField(fs.getTextFieldId(), new PropertyModel<>(data, "title")));
    }
    {
      // Values
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("values"));
      valuesField = new RequiredMaxLengthTextField(fs.getTextFieldId(), new PropertyModel<>(data, "values"));
      fs.addHelpIcon(getString("plugins.marketing.addressCampaign.values.format"));
      fs.add(valuesField);
      fs.addAlertIcon(getString("plugins.marketing.addressCampaign.edit.warning.doNotChangeValues"));
      valuesField.add((IValidator<String>) validatable -> {
        if (AddressCampaignDO.Companion.getValuesArray(validatable.getValue()) == null) {
          valuesField.error(getString("plugins.marketing.addressCampaign.values.invalidFormat"));
        }
      });
    }
    {
      // Text description
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("comment"));
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<>(data, "comment")));
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
