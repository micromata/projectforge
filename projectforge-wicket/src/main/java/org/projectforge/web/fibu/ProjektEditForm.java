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

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.fibu.KontoCache;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektStatus;
import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.business.fibu.kost.KostCache;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.reporting.Kost2Art;
import org.projectforge.web.task.TaskSelectPanel;
import org.projectforge.web.user.NewGroupSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.converter.IntegerConverter;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;

public class ProjektEditForm extends AbstractEditForm<ProjektDO, ProjektEditPage>
{
  private static final long serialVersionUID = -6018131069720611834L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ProjektEditForm.class);

  List<Kost2Art> kost2Arts;

  protected NewCustomerSelectPanel kundeSelectPanel;

  protected NewGroupSelectPanel groupSelectPanel;

  @SpringBean
  KontoCache kontoCache;

  @SpringBean
  KostCache kostCache;

  public ProjektEditForm(final ProjektEditPage parentPage, final ProjektDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newGridPanel();
    {
      // Number
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.projekt.nummer"));
      final MinMaxNumberField<Integer> field = new MinMaxNumberField<Integer>(InputPanel.WICKET_ID,
          new PropertyModel<Integer>(data,
              "nummer"),
          0, 99)
      {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public IConverter getConverter(final Class type)
        {
          return new IntegerConverter(2);
        }
      };
      WicketUtils.setSize(field, 2);
      fs.add(field);
    }
    {
      // Customer
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kunde")).suppressLabelForWarning();
      kundeSelectPanel = new NewCustomerSelectPanel(fs.newChildId(),
          new PropertyModel<KundeDO>(data, "kunde"), null, parentPage, "kundeId");
      fs.add(kundeSelectPanel);
      kundeSelectPanel.init();
    }
    {
      // Internal cost (digit 2-4)
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.projekt.internKost2_4"));
      fs.add(new DivTextPanel(fs.newChildId(), "4."));
      final MinMaxNumberField<Integer> field = new MinMaxNumberField<Integer>(InputPanel.WICKET_ID,
          new PropertyModel<Integer>(data,
              "internKost2_4"),
          0, 999)
      {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public IConverter getConverter(final Class type)
        {
          return new IntegerConverter(3);
        }
      };
      WicketUtils.setSize(field, 3);
      fs.add(field);
      fs.add(new DivTextPanel(fs.newChildId(), ".##.##"));
    }
    if (kontoCache.isEmpty() == false) {
      // Show this field only if DATEV accounts does exist.
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.konto"));
      final KontoSelectPanel kontoSelectPanel = new KontoSelectPanel(fs.newChildId(),
          new PropertyModel<KontoDO>(data, "konto"), null,
          "kontoId");
      kontoSelectPanel.setKontoNumberRanges(AccountingConfig.getInstance().getDebitorsAccountNumberRanges()).init();
      fs.addHelpIcon(getString("fibu.projekt.konto.tooltip"));
      fs.add(kontoSelectPanel);
    }
    {
      // Name
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.projekt.name"));
      final MaxLengthTextField field = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "name"));
      fs.add(field);
      WicketUtils.setFocus(field);
      WicketUtils.setStrong(field);
    }
    {
      // Identifier
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.projekt.identifier"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "identifier")));
    }
    {
      // task
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task"));
      final TaskSelectPanel taskSelectPanel = new TaskSelectPanel(fs, new PropertyModel<TaskDO>(data, "task"),
          parentPage, "taskId");
      fs.add(taskSelectPanel);
      taskSelectPanel.init();
    }
    {
      // DropDownChoice status
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("status"));
      final LabelValueChoiceRenderer<ProjektStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<ProjektStatus>(
          this,
          ProjektStatus.values());
      final DropDownChoice<ProjektStatus> statusChoice = new DropDownChoice<ProjektStatus>(fs.getDropDownChoiceId(),
          new PropertyModel<ProjektStatus>(data, "status"), statusChoiceRenderer.getValues(), statusChoiceRenderer);
      statusChoice.setNullValid(false).setRequired(true);
      fs.add(statusChoice);
    }
    {
      // project manager group
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.projekt.projektManagerGroup"))
          .suppressLabelForWarning();
      groupSelectPanel = new NewGroupSelectPanel(fs.newChildId(), new PropertyModel<GroupDO>(data,
          "projektManagerGroup"), parentPage, "projektManagerGroupId");
      fs.add(groupSelectPanel);
      groupSelectPanel.init();
    }
    {
      // description
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("description"));
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "description")));
    }
    if (isNew() == true) {
      kost2Arts = kostCache.getAllKostArts();
    } else {
      kost2Arts = kostCache.getAllKost2Arts(getData().getId());
    }
    {
      // cost 2 types
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kost2art.kost2arten")).suppressLabelForWarning();
      final ProjectEditCost2TypeTablePanel table = new ProjectEditCost2TypeTablePanel(fs.newChildId());
      fs.add(table);
      table.init(kost2Arts);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
