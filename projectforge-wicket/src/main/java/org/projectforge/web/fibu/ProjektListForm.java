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
import org.apache.wicket.model.PropertyModel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class ProjektListForm extends AbstractListForm<ProjektListFilter, ProjektListPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ProjektListForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  public ProjektListForm(final ProjektListPage parentPage)
  {
    super(parentPage);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel, org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    // DropDownChoice listType
    final LabelValueChoiceRenderer<String> typeChoiceRenderer = new LabelValueChoiceRenderer<String>();
    typeChoiceRenderer.addValue("all", getString("filter.all"));
    typeChoiceRenderer.addValue("notEnded", getString("notEnded"));
    typeChoiceRenderer.addValue("ended", getString("ended"));
    optionsFieldsetPanel.addDropDownChoice(new PropertyModel<String>(this, "searchFilter.listType"), typeChoiceRenderer.getValues(), typeChoiceRenderer,
        true).setNullValid(false);
  }

  @Override
  protected ProjektListFilter newSearchFilterInstance()
  {
    return new ProjektListFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
