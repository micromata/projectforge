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

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.KostentraegerStatus;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.RequiredMinMaxNumberField;
import org.projectforge.web.wicket.converter.IntegerConverter;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;
import org.slf4j.Logger;

public class Kost1EditForm extends AbstractEditForm<Kost1DO, Kost1EditPage>
{
  private static final long serialVersionUID = 7867840580460197749L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Kost1EditForm.class);

  protected TextField<Integer> nummernkreisField, bereichField, teilbereichField, endzifferField;

  public Kost1EditForm(final Kost1EditPage parentPage, final Kost1DO data)
  {
    super(parentPage, data);
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    /* GRID16 - BLOCK */
    gridBuilder.newGridPanel();
    {
      // Number range
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kost.kostentraeger"));
      nummernkreisField = new RequiredMinMaxNumberField<Integer>(InputPanel.WICKET_ID, new PropertyModel<Integer>(data, "nummernkreis"), 0,
          9);
      if (isNew() == true) {
        WicketUtils.setFocus(nummernkreisField);
      }
      WicketUtils.setSize(nummernkreisField, 1);
      fs.add(nummernkreisField);
      fs.add(new DivTextPanel(fs.newChildId(), "."));
      bereichField = new RequiredMinMaxNumberField<Integer>(InputPanel.WICKET_ID, new PropertyModel<Integer>(data, "bereich"), 0, 999) {
        @SuppressWarnings({ "rawtypes", "unchecked"})
        @Override
        public IConverter getConverter(final Class type)
        {
          return new IntegerConverter(3);
        }
      };
      WicketUtils.setSize(bereichField, 3);
      fs.add(bereichField);
      fs.add(new DivTextPanel(fs.newChildId(), "."));
      teilbereichField = new RequiredMinMaxNumberField<Integer>(InputPanel.WICKET_ID, new PropertyModel<Integer>(data, "teilbereich"), 0,
          99) {
        @SuppressWarnings({ "rawtypes", "unchecked"})
        @Override
        public IConverter getConverter(final Class type)
        {
          return new IntegerConverter(2);
        }
      };
      WicketUtils.setSize(teilbereichField, 2);
      fs.add(teilbereichField);
      fs.add(new DivTextPanel(fs.newChildId(), "."));
      endzifferField = new RequiredMinMaxNumberField<Integer>(InputPanel.WICKET_ID, new PropertyModel<Integer>(data, "endziffer"), 0, 99) {
        @SuppressWarnings({ "rawtypes", "unchecked"})
        @Override
        public IConverter getConverter(final Class type)
        {
          return new IntegerConverter(2);
        }
      };
      endzifferField.setRequired(true);
      WicketUtils.setSize(endzifferField, 2);
      fs.add(endzifferField);
    }
    {
      // Description
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("description"));
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "description")));
    }
    {
      // DropDownChoice status
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("status"));
      final LabelValueChoiceRenderer<KostentraegerStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<KostentraegerStatus>(this,
          KostentraegerStatus.values());
      final DropDownChoice<KostentraegerStatus> statusChoice = new DropDownChoice<KostentraegerStatus>(fs.getDropDownChoiceId(),
          new PropertyModel<KostentraegerStatus>(data, "kostentraegerStatus"), statusChoiceRenderer.getValues(), statusChoiceRenderer);
      statusChoice.setNullValid(false);
      statusChoice.setRequired(true);
      fs.add(statusChoice);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
