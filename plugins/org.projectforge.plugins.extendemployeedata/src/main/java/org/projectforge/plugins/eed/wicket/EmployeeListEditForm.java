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

package org.projectforge.plugins.eed.wicket;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeFilter;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.plugins.eed.ExtendEmployeeDataEnum;
import org.projectforge.plugins.eed.service.EEDHelper;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class EmployeeListEditForm extends AbstractListForm<EmployeeFilter, EmployeeListEditPage>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeListEditForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  @SpringBean
  private EEDHelper eedHelper;

  protected Integer selectedMonth;

  protected Integer selectedYear;

  protected ExtendEmployeeDataEnum selectedOption;

  protected boolean showOnlyActiveEntries = true;

  @Override
  protected void init()
  {
    super.init();

    //Top Buttons
    //Disable default buttons
    resetButtonPanel.setVisible(false);
    searchButtonPanel.setVisible(false);
    //Create custom search button
    final Button searchButton = new Button(SingleButtonPanel.WICKET_ID, new Model<>("search"))
    {
      @Override
      public final void onSubmit()
      {
        parentPage.refreshDataTable();
      }
    };
    WicketUtils.addTooltip(searchButton, getString("search"));
    final SingleButtonPanel customizedSearchButtonPanel = new SingleButtonPanel(actionButtons.newChildId(),
        searchButton,
        getString("search"), SingleButtonPanel.DEFAULT_SUBMIT);
    actionButtons.add(customizedSearchButtonPanel);

    //    setDefaultButton(searchButton);

    // Customized Filter
    remove(gridBuilder.getMainContainer());
    gridBuilder = newGridBuilder(this, "filter");
    //Filter
    //Fieldset for Date DropDown
    final FieldsetPanel fsMonthYear = gridBuilder
        .newFieldset(
            I18nHelper.getLocalizedMessage("plugins.eed.listcare.yearmonth"));
    //Get actual Month as preselected
    selectedMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
    //Month DropDown
    DropDownChoicePanel<Integer> ddcMonth = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedMonth"),
            EEDHelper.MONTH_INTEGERS));
    fsMonthYear.add(ddcMonth);
    //Get actual year for pre select
    selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    //Year DropDown
    DropDownChoicePanel<Integer> ddcYear = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedYear"),
            eedHelper.getDropDownYears()));
    fsMonthYear.add(ddcYear);

    //Fieldset for option DropDown
    final FieldsetPanel fsOption = gridBuilder
        .newFieldset(I18nHelper.getLocalizedMessage("plugins.eed.listcare.option"));
    //Option DropDown
    final DropDownChoicePanel<ExtendEmployeeDataEnum> ddcOption = new DropDownChoicePanel<>(
        gridBuilder.getPanel().newChildId(),
        new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedOption"),
            Arrays.asList(ExtendEmployeeDataEnum.values()), new IChoiceRenderer<ExtendEmployeeDataEnum>()
        {
          @Override
          public Object getDisplayValue(ExtendEmployeeDataEnum eede)
          {
            return I18nHelper.getLocalizedMessage(eede.getI18nKeyDropDown());
          }

          @Override
          public String getIdValue(ExtendEmployeeDataEnum eede, int index)
          {
            return String.valueOf(index);
          }

          @Override
          public ExtendEmployeeDataEnum getObject(final String s, final IModel<? extends List<? extends ExtendEmployeeDataEnum>> iModel)
          {
            if (s == null) {
              return null;
            }

            for (int i = iModel.getObject().size() - 1; i >= 0; --i) {
              // TODO sn migration
              if (s.equals(String.valueOf(i))) {
                return iModel.getObject().get(i);
              }
            }

            return null;
          }
        }));
    fsOption.add(ddcOption);

    //Filter for active employees
    gridBuilder.newSplitPanel(GridSize.COL66);
    FieldsetPanel optionsFieldsetPanel = gridBuilder.newFieldset(getOptionsLabel()).suppressLabelForWarning();
    final DivPanel optionsCheckBoxesPanel = optionsFieldsetPanel.addNewCheckBoxButtonDiv();
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<>(this, "showOnlyActiveEntries"), getString("label.onlyActiveEntries")));
  }

  public EmployeeListEditForm(final EmployeeListEditPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  public Panel getSaveButtonPanel(String id)
  {
    //Bottom Buttons
    final Button saveButton = new Button(SingleButtonPanel.WICKET_ID, new Model<>("save"))
    {
      @Override
      public final void onSubmit()
      {
        parentPage.saveList();
      }
    };
    WicketUtils.addTooltip(saveButton, getString("save"));
    return new SingleButtonPanel(id, saveButton, getString("save"), SingleButtonPanel.DEFAULT_SUBMIT);
  }

  @Override
  protected EmployeeFilter newSearchFilterInstance()
  {
    return new EmployeeFilter();
  }

}
