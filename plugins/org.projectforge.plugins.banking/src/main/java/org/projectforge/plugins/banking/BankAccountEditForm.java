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

package org.projectforge.plugins.banking;

import org.apache.wicket.model.PropertyModel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;

/**
 * This is the edit formular page.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class BankAccountEditForm extends AbstractEditForm<BankAccountDO, BankAccountEditPage>
{
  private static final long serialVersionUID = -6208809585214296635L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BankAccountEditForm.class);

  public BankAccountEditForm(final BankAccountEditPage parentPage, final BankAccountDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Name
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.banking.account.name"));
      fs.add(new RequiredMaxLengthTextField(fs.getTextFieldId(), new PropertyModel<>(data, "name")));
    }
    {
      // Account number
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.banking.account.number"));
      fs.add(new RequiredMaxLengthTextField(fs.getTextFieldId(), new PropertyModel<>(data, "accountNumber")));
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Bank
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.banking.bank"));
      fs.add(new MaxLengthTextField(fs.getTextFieldId(), new PropertyModel<>(data, "bank")));
    }
    {
      // Bank identification code
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.banking.bankIdentificationCode"));
      fs.add(new MaxLengthTextField(fs.getTextFieldId(), new PropertyModel<>(data, "bankIdentificationCode")));
    }
    gridBuilder.newGridPanel();
    {
      // Text description
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("description"));
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<>(data, "description"))).setAutogrow();
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
