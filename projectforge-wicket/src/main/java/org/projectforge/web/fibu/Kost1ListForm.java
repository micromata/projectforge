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

package org.projectforge.web.fibu;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class Kost1ListForm extends AbstractListForm<Kost1ListFilter, Kost1ListPage>
{
  private static final long serialVersionUID = -1065033270563801353L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Kost1ListForm.class);

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel, org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    // DropDownChoice listType
    final LabelValueChoiceRenderer<String> typeChoiceRenderer = new LabelValueChoiceRenderer<String>();
    typeChoiceRenderer.addValue("all", getString("filter.all"));
    typeChoiceRenderer.addValue("active", getString("fibu.kost.status.active"));
    typeChoiceRenderer.addValue("nonactive", getString("fibu.kost.status.nonactive"));
    typeChoiceRenderer.addValue("notEnded", getString("notEnded"));
    typeChoiceRenderer.addValue("ended", getString("ended"));
    final DropDownChoice<String> typeChoice = new DropDownChoice<String>(optionsFieldsetPanel.getDropDownChoiceId(), new PropertyModel<String>(this,
        "searchFilter.listType"), typeChoiceRenderer.getValues(), typeChoiceRenderer);
    typeChoice.setNullValid(false);
    optionsFieldsetPanel.add(typeChoice, true);
  }

  public Kost1ListForm(final Kost1ListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected Kost1ListFilter newSearchFilterInstance()
  {
    return new Kost1ListFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
