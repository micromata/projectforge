package org.projectforge.plugins.eed.wicket;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.export.AttrColumnDescription;
import org.projectforge.export.DOListExcelExporter;
import org.projectforge.export.DOWithAttrListExcelExporter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.plugins.eed.wicket.EmployeeListEditForm.SelectOption;
import org.projectforge.web.core.MenuBarPanel;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

public class EmployeeListEditPage extends AbstractListPage<EmployeeListEditForm, EmployeeService, EmployeeDO> implements
    IListPageColumnsCreator<EmployeeDO>
{
  private static final long serialVersionUID = -9117648731994041528L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeListEditPage.class);

  @SpringBean
  private EmployeeService employeeService;

  @SpringBean
  private TimeableService<Integer, EmployeeTimedDO> timeableService;

  public EmployeeListEditPage(final PageParameters parameters)
  {
    super(parameters, "fibu.employee");
  }

  @Override
  public List<IColumn<EmployeeDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<EmployeeDO, String>> columns = new ArrayList<>();

    final CellItemListener<EmployeeDO> cellItemListener = (CellItemListener<EmployeeDO>) (item, componentId,
        rowModel) -> {
      final EmployeeDO employee = rowModel.getObject();
      appendCssClasses(item, employee.getId(), employee.isDeleted());
    };

    columns.add(new CellItemListenerPropertyColumn<>(new ResourceModel("name"),
        getSortable("user.lastname", sortable),
        "user.lastname", cellItemListener));

    columns.add(new CellItemListenerPropertyColumn<>(new ResourceModel("firstName"),
        getSortable("user.firstname", sortable),
        "user.firstname", cellItemListener));

    SelectOption so = SelectOption.findByAttrXMLKey(form.getSelectedOption());
    switch (so) {
      case WEEKENDWORK:
        createWeekendworkColumns(columns, sortable, cellItemListener);
        break;
      case NONE:
        log.info("No option filter is selected!");
        break;
      default:
        error(I18nHelper.getLocalizedString(ThreadLocalUserContext.getLocale(), "not.found"));
        log.warn("Selected option not found: " + form.getSelectedOption());
    }

    return columns;
  }

  private void createWeekendworkColumns(List<IColumn<EmployeeDO, String>> columns, boolean sortable,
      CellItemListener<EmployeeDO> cellItemListener)
  {
    columns.add(new AttrInputCellItemListenerPropertyColumn<>(
        new ResourceModel("fibu.employee.weekendwork.saturday"),
        getSortable("fibu.employee.weekendwork.saturday", sortable),
        "weekendwork", "workinghourssaturday", cellItemListener, timeableService, employeeService,
        form.getSelectedMonth(), form.getSelectedYear()));

    columns.add(new AttrInputCellItemListenerPropertyColumn<>(
        new ResourceModel("fibu.employee.weekendwork.sunday"),
        getSortable("fibu.employee.weekendwork.sunday", sortable),
        "weekendwork", "workinghourssunday", cellItemListener, timeableService, employeeService,
        form.getSelectedMonth(), form.getSelectedYear()));

    columns.add(new AttrInputCellItemListenerPropertyColumn<>(
        new ResourceModel("fibu.employee.weekendwork.holiday"),
        getSortable("fibu.employee.weekendwork.holiday", sortable),
        "weekendwork", "workinghoursholiday", cellItemListener, timeableService, employeeService,
        form.getSelectedMonth(), form.getSelectedYear()));

  }

  @Override
  protected DOListExcelExporter createExcelExporter(final String filenameIdentifier)
  {
    final String[] fieldsToExport = { "id", "user", "kost1", "staffNumber" };

    final AttrColumnDescription[] attrFieldsToExport = {
        new AttrColumnDescription("mobilecheck", "mobilecheck", "fibu.employee.mobilecheck.title"),
        new AttrColumnDescription("ebikeleasing", "ebikeleasing", "fibu.employee.ebikeleasing.title")
    };

    final Date dateToSelectAttrRow = new GregorianCalendar(form.getSelectedYear(), form.getSelectedMonth() - 1, 1, 0, 0)
        .getTime();
    return new DOWithAttrListExcelExporter<>(filenameIdentifier, timeableService, fieldsToExport, attrFieldsToExport,
        dateToSelectAttrRow);
  }

  @Override
  protected void init()
  {
    final List<IColumn<EmployeeDO, String>> columns = createColumns(this, true);
    dataTable = createDataTable(columns, "user.lastname", SortOrder.ASCENDING);
    form.add(dataTable);

    // remove add and reindex buttons from context menu
    contentMenuBarPanel = new MenuBarPanel("menuBar");
    addExcelExport(getString("fibu.employee.title.heading"), "employees");
  }

  @Override
  protected EmployeeListEditForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new EmployeeListEditForm(this);
  }

  @Override
  public EmployeeService getBaseDao()
  {
    return employeeService;
  }

  @Override
  protected void addBottomPanel(final String id)
  {
    form.add(form.getSaveButtonPanel(id));
  }

  public void refreshDataTable()
  {
    final List<IColumn<EmployeeDO, String>> columns = createColumns(this, true);
    dataTable = createDataTable(columns, "user.lastname", SortOrder.ASCENDING);
    form.addOrReplace(dataTable);
  }

}
