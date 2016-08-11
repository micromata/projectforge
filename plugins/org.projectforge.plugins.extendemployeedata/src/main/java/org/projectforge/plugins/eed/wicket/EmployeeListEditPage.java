package org.projectforge.plugins.eed.wicket;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

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
import org.projectforge.plugins.eed.ExtendedEmployeeDataEnum;
import org.projectforge.web.core.MenuBarPanel;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;

public class EmployeeListEditPage extends AbstractListPage<EmployeeListEditForm, EmployeeService, EmployeeDO> implements
    IListPageColumnsCreator<EmployeeDO>
{
  private static final long serialVersionUID = -9117648731994041528L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeListEditPage.class);

  @SpringBean
  private EmployeeService employeeService;

  @SpringBean
  private TimeableService<Integer, EmployeeTimedDO> timeableService;

  private List<EmployeeDO> dataList;

  public EmployeeListEditPage(final PageParameters parameters)
  {
    super(parameters, "fibu.employee");
  }

  @Override
  protected String getTitle()
  {
    return getString("plugins.eed.listcare.title");
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

    ExtendedEmployeeDataEnum eede = ExtendedEmployeeDataEnum.findByAttrXMLKey(form.getSelectedOption());
    createWeekendworkColumns(eede, columns, sortable, cellItemListener);

    return columns;
  }

  private void createWeekendworkColumns(ExtendedEmployeeDataEnum eede, List<IColumn<EmployeeDO, String>> columns,
      boolean sortable,
      CellItemListener<EmployeeDO> cellItemListener)
  {
    for (AttrColumnDescription desc : eede.getAttrColumnDescriptions()) {
      columns.add(new AttrInputCellItemListenerPropertyColumn<>(
          new ResourceModel(desc.getI18nKey()),
          getSortable(desc.getI18nKey(), sortable),
          desc.getGroupName(), desc.getPropertyName(), cellItemListener, timeableService, employeeService,
          form.getSelectedMonth(), form.getSelectedYear()));
    }
  }

  @Override
  protected DOListExcelExporter createExcelExporter(final String filenameIdentifier)
  {
    final String[] fieldsToExport = { "id", "user", "staffNumber" };
    final List<AttrColumnDescription> attrFieldsToExport = ExtendedEmployeeDataEnum
        .findByAttrXMLKey(form.getSelectedOption())
        .getAttrColumnDescriptions();

    final Date dateToSelectAttrRow = new GregorianCalendar(form.getSelectedYear(), form.getSelectedMonth() - 1, 1, 0, 0).getTime();
    return new DOWithAttrListExcelExporter<>(filenameIdentifier, timeableService, fieldsToExport, attrFieldsToExport, dateToSelectAttrRow);
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

  public void saveList()
  {
    for (EmployeeDO e : this.dataList) {
      List<EmployeeTimedDO> unusedTimeableAttributes = new ArrayList<>();
      for (EmployeeTimedDO timed : e.getTimeableAttributes()) {
        if (log.isDebugEnabled()) {
          log.debug("Timed attributes for employee: " + e.getUser().getFullname() + " Attribute group: "
              + timed.getGroupName() + " Start-Date: " + timed.getStartTime());
        }
        Map<String, JpaTabAttrBaseDO<EmployeeTimedDO, Integer>> attributes = timed.getAttributes();
        if (attributes.size() < 1) {
          unusedTimeableAttributes.add(timed);
        }
      }
      //Remove unused timeable attribute
      e.getTimeableAttributes().removeAll(unusedTimeableAttributes);
      employeeService.update(e);
    }
    info(I18nHelper.getLocalizedString("plugins.eed.listcare.savesucces"));
  }

  @Override
  public List<EmployeeDO> getList()
  {
    this.dataList = super.getList();
    return this.dataList;
  }

}
