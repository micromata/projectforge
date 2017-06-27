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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExcelExporter;
import org.projectforge.business.excel.ExportColumn;
import org.projectforge.business.excel.PropertyMapping;
import org.projectforge.business.fibu.EmployeeSalaryDO;
import org.projectforge.business.fibu.EmployeeSalaryDao;
import org.projectforge.business.fibu.datev.EmployeeSalaryExportDao;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.export.DOListExcelExporter;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.CurrencyPropertyColumn;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

@ListPage(editPage = EmployeeSalaryEditPage.class)
public class EmployeeSalaryListPage
    extends AbstractListPage<EmployeeSalaryListForm, EmployeeSalaryDao, EmployeeSalaryDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeSalaryListPage.class);

  private static final long serialVersionUID = -8406452960003792763L;

  @SpringBean
  private EmployeeSalaryDao employeeSalaryDao;

  @SpringBean
  private EmployeeSalaryExportDao employeeSalaryExportDao;

  public EmployeeSalaryListPage(final PageParameters parameters)
  {
    super(parameters, "fibu.employee.salary");
  }

  public EmployeeSalaryListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "fibu.employeeSalary");
  }

  @SuppressWarnings("serial")
  public List<IColumn<EmployeeSalaryDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<EmployeeSalaryDO, String>> columns = new ArrayList<IColumn<EmployeeSalaryDO, String>>();

    final CellItemListener<EmployeeSalaryDO> cellItemListener = new CellItemListener<EmployeeSalaryDO>()
    {
      public void populateItem(final Item<ICellPopulator<EmployeeSalaryDO>> item, final String componentId,
          final IModel<EmployeeSalaryDO> rowModel)
      {
        final EmployeeSalaryDO employeeSalary = rowModel.getObject();
        appendCssClasses(item, employeeSalary.getId(), employeeSalary.isDeleted());
      }
    };
    columns.add(new CellItemListenerPropertyColumn<EmployeeSalaryDO>(getString("calendar.month"),
        getSortable("formattedYearAndMonth",
            sortable),
        "formattedYearAndMonth", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<EmployeeSalaryDO>> item, final String componentId,
          final IModel<EmployeeSalaryDO> rowModel)
      {
        final EmployeeSalaryDO employeeSalary = rowModel.getObject();
        if (isSelectMode() == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, EmployeeSalaryEditPage.class,
              employeeSalary.getId(), returnToPage,
              employeeSalary.getFormattedYearAndMonth()));
        } else {
          item.add(new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, employeeSalary.getId(),
              employeeSalary
                  .getFormattedYearAndMonth()));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<EmployeeSalaryDO>(getString("name"),
        getSortable("employee.user.lastname", sortable),
        "employee.user.lastname", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<EmployeeSalaryDO>(getString("firstName"),
        getSortable("employee.user.firstname",
            sortable),
        "employee.user.firstname", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<EmployeeSalaryDO>(getString("fibu.employee.staffNumber"),
        getSortable("employee.staffNumber",
            sortable),
        "employee.staffNumber", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<EmployeeSalaryDO>(EmployeeSalaryDO.class,
        getSortable("type", sortable), "type",
        cellItemListener));
    columns.add(
        new CurrencyPropertyColumn<EmployeeSalaryDO>(getString("fibu.employee.salary.bruttoMitAgAnteil"), getSortable(
            "bruttoMitAgAnteil", sortable), "bruttoMitAgAnteil", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<EmployeeSalaryDO>(EmployeeSalaryDO.class,
        getSortable("comment", sortable), "comment",
        cellItemListener));
    return columns;
  }

  protected void exportExcel()
  {
    refresh();

    log.info("Exporting employee salaries as excel sheet for: "
        + DateHelper.formatMonth(form.getSearchFilter().getYear(), form.getSearchFilter().getMonth()));
    final List<EmployeeSalaryDO> list = getList();
    if (list == null || list.size() == 0) {
      // Nothing to export.
      form.addError("validation.error.nothingToExport");
      return;
    }
    final String filename = "ProjectForge-EmployeeSalaries_"
        + DateHelper.formatMonth(form.getSearchFilter().getYear(), form.getSearchFilter().getMonth())
        + "_"
        + DateHelper.getDateAsFilenameSuffix(new Date())
        + ".xls";
    final byte[] xls = employeeSalaryExportDao.export(list);
    DownloadUtils.setDownloadTarget(xls, filename);
  }

  @Override
  protected void init()
  {
    final List<IColumn<EmployeeSalaryDO, String>> columns = createColumns(this, true);
    dataTable = createDataTable(columns, "employee.user.lastname", SortOrder.ASCENDING);
    form.add(dataTable);
    addExcelExport(getString("fibu.employee.salaries"), getString("fibu.employee.salaries"));
    {
      // Excel export
      @SuppressWarnings("serial")
      final SubmitLink excelExportLink = new SubmitLink(ContentMenuEntryPanel.LINK_ID, form)
      {
        @Override
        public void onSubmit()
        {
          if (form.getSearchFilter().getMonth() < 0 || form.getSearchFilter().getMonth() > 11) {
            form.addError("fibu.employee.salary.error.monthNotGiven");
            return;
          }
          exportExcel();
        }
      };
      final ContentMenuEntryPanel excelExportButton = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          excelExportLink,
          getString("fibu.rechnung.kostExcelExport")).setTooltip(getString("fibu.employee.salary.exportXls.tooltip"));
      addContentMenuEntry(excelExportButton);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#createExcelExporter(java.lang.String)
   */
  @Override
  protected DOListExcelExporter createExcelExporter(final String filenameIdentifier)
  {
    return new DOListExcelExporter(filenameIdentifier)
    {
      /**
       * @see org.projectforge.export.MyExcelExporter#putFieldFormat(ContentProvider,
       *      java.lang.reflect.Field, org.projectforge.common.anots.PropertyInfo, ExportColumn)
       */
      @Override
      public void putFieldFormat(final ContentProvider sheetProvider, final Field field, final PropertyInfo propInfo,
          final ExportColumn exportColumn)
      {
        if ("month".equals(field.getName()) == true) {
          sheetProvider.putFormat(exportColumn, "mmm");
          exportColumn.setWidth(6);
        } else if ("year".equals(field.getName()) == true) {
          sheetProvider.putFormat(exportColumn, "#");
          exportColumn.setWidth(6);
        } else {
          super.putFieldFormat(sheetProvider, field, propInfo, exportColumn);
        }
      }

      /**
       * @see ExcelExporter#addMapping(PropertyMapping, java.lang.Object,
       *      java.lang.reflect.Field)
       */
      @Override
      public void addMapping(final PropertyMapping mapping, final Object entry, final Field field)
      {
        if ("month".equals(field.getName()) == true) {
          final EmployeeSalaryDO salary = (EmployeeSalaryDO) entry;
          // Excel month starts with 1 instead of 0:
          mapping.add(field.getName(), salary.getMonth() + 1);
        } else {
          super.addMapping(mapping, entry, field);
        }
      }
    };
  }

  @Override
  protected EmployeeSalaryListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new EmployeeSalaryListForm(this);
  }

  @Override
  public EmployeeSalaryDao getBaseDao()
  {
    return employeeSalaryDao;
  }

  protected EmployeeSalaryDao getEmployeeSalaryDao()
  {
    return employeeSalaryDao;
  }
}
