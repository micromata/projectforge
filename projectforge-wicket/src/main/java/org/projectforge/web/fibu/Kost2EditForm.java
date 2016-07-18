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

import java.math.BigDecimal;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.kost.Kost2ArtDao;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.KostentraegerStatus;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.RequiredMinMaxNumberField;
import org.projectforge.web.wicket.converter.IntegerConverter;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;

public class Kost2EditForm extends AbstractEditForm<Kost2DO, Kost2EditPage>
{
  private static final long serialVersionUID = -2138017238114715368L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Kost2EditForm.class);

  @SpringBean
  private Kost2ArtDao kost2ArtDao;

  protected TextField<Integer> nummernkreisField;

  protected TextField<Integer> bereichField;

  protected TextField<Integer> teilbereichField;

  protected TextField<Integer> kost2ArtField;

  protected NewProjektSelectPanel projektSelectPanel;

  public Kost2EditForm(final Kost2EditPage parentPage, final Kost2DO data)
  {
    super(parentPage, data);
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    gridBuilder.newGridPanel();
    {
      // Project
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.projekt"));
      projektSelectPanel = new NewProjektSelectPanel(fs.newChildId(), new PropertyModel<ProjektDO>(data,
          "projekt"), parentPage,"projektId");
      fs.add(projektSelectPanel);
      projektSelectPanel.init();
    }
    {
      // Kost 2
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
        @SuppressWarnings({ "unchecked", "rawtypes"})
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
        @SuppressWarnings({ "unchecked", "rawtypes"})
        @Override
        public IConverter getConverter(final Class type)
        {
          return new IntegerConverter(2);
        }
      };
      WicketUtils.setSize(teilbereichField, 2);
      fs.add(teilbereichField);
      fs.add(new DivTextPanel(fs.newChildId(), "."));
      kost2ArtField = new RequiredMinMaxNumberField<Integer>(InputPanel.WICKET_ID, new PropertyModel<Integer>(data, "kost2Art.id"), 0, 99) {
        @SuppressWarnings({ "unchecked", "rawtypes"})
        @Override
        public IConverter getConverter(final Class type)
        {
          return new IntegerConverter(2);
        }
      };
      kost2ArtField.setRequired(true);
      kost2ArtField.add(new AbstractValidator<Integer>() {
        @Override
        protected void onValidate(final IValidatable<Integer> validatable)
        {
          final Integer value = validatable.getValue();
          if (value == null) {
            return;
          }
          if (kost2ArtDao.getById(value) == null) { // Kost2 available but not selected.
            error(validatable);
          }
        }

        @Override
        protected String resourceKey()
        {
          return "fibu.kost2art.error.notFound";
        }
      });
      WicketUtils.setSize(kost2ArtField, 2);
      fs.add(kost2ArtField);
    }
    {
      // work fraction
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kost2.workFraction"));
      fs.add(new MinMaxNumberField<BigDecimal>(InputPanel.WICKET_ID, new PropertyModel<BigDecimal>(data, "workFraction"), BigDecimal.ZERO,
          BigDecimal.ONE));
    }
    {
      // description
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("description"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "description")));
    }
    {
      // comment
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("comment"));
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "comment")));
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
