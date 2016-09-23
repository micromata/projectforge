package org.projectforge.web.vacation.helper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.web.vacation.VacationViewPageSortableDataProvider;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.Heading1Panel;
import org.projectforge.web.wicket.flowlayout.Heading3Panel;
import org.projectforge.web.wicket.flowlayout.TablePanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VacationViewHelper
{
  @Autowired
  private VacationService vacationService;

  public void createVacationView(GridBuilder gridBuilder, EmployeeDO currentEmployee)
  {
    final Calendar now = new GregorianCalendar();
    DivPanel section = gridBuilder.getPanel();
    section.add(new Heading1Panel(section.newChildId(), I18nHelper.getLocalizedMessage("menu.vacation.leaveaccount")));
    appendFieldset(gridBuilder, "vacation.annualleave",
        currentEmployee.getUrlaubstage() != null ? currentEmployee.getUrlaubstage().toString() : "0");
    appendFieldset(gridBuilder, "vacation.previousyearleave",
        currentEmployee.getAttribute("previousyearleave", BigDecimal.class) != null
            ? currentEmployee.getAttribute("previousyearleave", BigDecimal.class).toString() : "0");
    appendFieldset(gridBuilder, "vacation.usedvacation",
        vacationService.getUsedVacationdays(currentEmployee).toString());
    appendFieldset(gridBuilder, "vacation.planedvacation",
        vacationService.getPlanedVacationdays(currentEmployee).toString());
    appendFieldset(gridBuilder, "vacation.availablevacation",
        vacationService.getAvailableVacationdays(currentEmployee).toString());
    section.add(new Heading3Panel(section.newChildId(),
        I18nHelper.getLocalizedMessage("vacation.title.list") + " " + now.get(Calendar.YEAR)));
    TablePanel tablePanel = new TablePanel(section.newChildId());
    section.add(tablePanel);
    final DataTable<VacationDO, String> dataTable = createDataTable(createColumns(), "startDate", SortOrder.ASCENDING,
        currentEmployee);
    tablePanel.add(dataTable);
  }

  private DataTable<VacationDO, String> createDataTable(final List<IColumn<VacationDO, String>> columns,
      final String sortProperty, final SortOrder sortOrder, final EmployeeDO employee)
  {
    final SortParam<String> sortParam = sortProperty != null
        ? new SortParam<String>(sortProperty, sortOrder == SortOrder.ASCENDING) : null;
    return new DefaultDataTable<VacationDO, String>(TablePanel.TABLE_ID, columns,
        createSortableDataProvider(sortParam, employee), 50);
  }

  private ISortableDataProvider<VacationDO, String> createSortableDataProvider(final SortParam<String> sortParam,
      EmployeeDO employee)
  {
    return new VacationViewPageSortableDataProvider<VacationDO>(sortParam, vacationService, employee);
  }

  private List<IColumn<VacationDO, String>> createColumns()
  {
    final List<IColumn<VacationDO, String>> columns = new ArrayList<IColumn<VacationDO, String>>();

    final CellItemListener<VacationDO> cellItemListener = new CellItemListener<VacationDO>()
    {
      private static final long serialVersionUID = 1L;

      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
          final IModel<VacationDO> rowModel)
      {
        //Nothing to do here
      }
    };
    columns.add(
        new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, "startDate", "startDate", cellItemListener));
    columns
        .add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, "endDate", "endDate", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, "status", "status", cellItemListener));
    columns
        .add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, "workingdays", "workingdays",
            cellItemListener));
    return columns;
  }

  private boolean appendFieldset(GridBuilder gridBuilder, final String label, final String value)
  {
    if (StringUtils.isBlank(value) == true) {
      return false;
    }
    final FieldsetPanel fs = gridBuilder.newFieldset(I18nHelper.getLocalizedMessage(label)).suppressLabelForWarning();
    fs.add(new DivTextPanel(fs.newChildId(), value));
    return true;
  }

}
