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
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KontoStatus;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;

public class KontoEditForm extends AbstractEditForm<KontoDO, KontoEditPage>
{
  private static final long serialVersionUID = -6018131069720611834L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(KontoEditForm.class);

  public KontoEditForm(final KontoEditPage parentPage, final KontoDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected void init()
  {
    super.init();
    /* GRID16 - BLOCK */
    gridBuilder.newGridPanel();
    {
      // Number
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.konto.nummer"));
      final MinMaxNumberField<Integer> nummerField = new MinMaxNumberField<Integer>(InputPanel.WICKET_ID, new PropertyModel<Integer>(data,
          "nummer"), 0, 99999999);
      fs.add(nummerField);
      if (isNew() == true) {
        WicketUtils.setFocus(nummerField);
      }
    }
    {
      // DropDownChoice status
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("status"));
      final LabelValueChoiceRenderer<KontoStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<KontoStatus>(this,
          KontoStatus.values());
      final DropDownChoice<KontoStatus> statusChoice = new DropDownChoice<KontoStatus>(fs.getDropDownChoiceId(),
          new PropertyModel<KontoStatus>(data, "status"), statusChoiceRenderer.getValues(), statusChoiceRenderer);
      statusChoice.setNullValid(true);
      statusChoice.setRequired(false);
      fs.add(statusChoice);
    }
    {
      // Identifier
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.konto.bezeichnung"));
      final RequiredMaxLengthTextField identifier = new RequiredMaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data,
          "bezeichnung"));
      fs.add(identifier);
      if (isNew() == false) {
        WicketUtils.setFocus(identifier);
      }
    }
    {
      // Description
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("description"));
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "description")));
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
