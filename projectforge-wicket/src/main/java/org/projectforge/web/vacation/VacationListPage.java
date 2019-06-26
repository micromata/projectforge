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

package org.projectforge.web.vacation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.excel.PropertyMapping;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.common.BeanHelper;
import org.projectforge.export.DOGetterListExcelExporter;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerLambdaColumn;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.flowlayout.TextPanel;

@ListPage(editPage = VacationEditPage.class)
public class VacationListPage extends AbstractListPage<VacationListForm, VacationService, VacationDO> implements
    IListPageColumnsCreator<VacationDO>
{
  private static final long serialVersionUID = -8406452960003712363L;

  @SpringBean
  private VacationService vacationService;

  public VacationListPage(final PageParameters parameters)
  {
    super(parameters, "vacation");
  }

  public VacationListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "vacation");
  }

  @Override
  @SuppressWarnings("serial")
  public List<IColumn<VacationDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<VacationDO, String>> columns = new ArrayList<>();

    final CellItemListener<VacationDO> cellItemListener = new CellItemListener<VacationDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
          final IModel<VacationDO> rowModel)
      {
        final VacationDO vacation = rowModel.getObject();
        appendCssClasses(item, vacation.getId(), vacation.isDeleted());
      }
    };

    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class,
        getSortable("employee", sortable),
        "employee", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
          final IModel<VacationDO> rowModel)
      {
        final VacationDO vacation = rowModel.getObject();
        final EmployeeDO employee = vacation.getEmployee();
        final String fullname = employee != null && employee.getUser() != null ? employee.getUser().getFullname()
            : null;
        if (isSelectMode() == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, VacationEditPage.class, vacation.getId(),
              returnToPage, fullname));
        } else {
          item.add(
              new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, vacation.getId(), fullname));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns
        .add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, getSortable("startDate", sortable),
            "startDate",
            cellItemListener));
    columns
        .add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, getSortable("endDate", sortable),
            "endDate",
            cellItemListener));

    columns
        .add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, getSortable("vacationmode", sortable),
            "vacationmode",
            cellItemListener));

    columns
        .add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, getSortable("status", sortable),
            "status",
            cellItemListener));

    columns.add(new CellItemListenerLambdaColumn<>(new ResourceModel("vacation.workingdays"),
        rowModel -> vacationService.getVacationDays(rowModel.getObject().getStartDate(), rowModel.getObject().getEndDate(), rowModel.getObject().getHalfDay()),
        cellItemListener)
    );

    columns
        .add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, "isSpecial", "isSpecial",
            cellItemListener)
        {
          @Override
          public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
              final IModel<VacationDO> rowModel)
          {
            final VacationDO vacation = rowModel.getObject();
            if (vacation.isSpecial() != null && vacation.isSpecial() == Boolean.TRUE) {
              item.add(new TextPanel(componentId, I18nHelper.getLocalizedMessage("yes")));
            } else {
              item.add(new TextPanel(componentId, I18nHelper.getLocalizedMessage("no")));
            }
            cellItemListener.populateItem(item, componentId, rowModel);
          }
        });

    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class,
        getSortable("manager", sortable),
        "manager", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
          final IModel<VacationDO> rowModel)
      {
        final VacationDO vacation = rowModel.getObject();
        final EmployeeDO manager = vacation.getManager();
        final String fullname = manager != null && manager.getUser() != null ? manager.getUser().getFullname()
            : null;
        if (isSelectMode() == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, VacationEditPage.class, vacation.getId(),
              returnToPage, fullname));
        } else {
          item.add(
              new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, vacation.getId(), fullname));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });

    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class,
        getSortable("substitutions", sortable),
        "substitutions", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
          final IModel<VacationDO> rowModel)
      {
        final VacationDO vacation = rowModel.getObject();
        final Set<EmployeeDO> substitutions = vacation.getSubstitutions();
        final List<String> substitutionNames = new ArrayList<>();

        for (EmployeeDO substitution : substitutions) {
          if (substitution != null && substitution.getUser() != null) {
            substitutionNames.add(substitution.getUser().getFullname());
          }
        }

        if (isSelectMode() == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, VacationEditPage.class, vacation.getId(),
              returnToPage, String.join(", ", substitutionNames)));
        } else {
          item.add(
              new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, vacation.getId(), String.join(" ,", substitutionNames)));
        }

        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });

    return columns;
  }

  @Override
  protected void init()
  {
    newItemMenuEntry.setVisible(vacationService.couldUserUseVacationService(ThreadLocalUserContext.getUser(), false));
    final List<IColumn<VacationDO, String>> columns = createColumns(this, true);
    dataTable = createDataTable(columns, "startDate", SortOrder.DESCENDING);
    form.add(dataTable);
    addExcelExport(getString("vacation.title.heading"), "vacation");
  }

  @Override
  protected DOGetterListExcelExporter createExcelExporter(final String filenameIdentifier)
  {
    return new DOGetterListExcelExporter(filenameIdentifier)
    {
      @Override
      public void addMapping(final PropertyMapping mapping, final Object entry, final Field field)
      {
        if (field.getName().equals("substitutions")) {
          Set<EmployeeDO> substitutions = (Set<EmployeeDO>) BeanHelper.getFieldValue(entry, field);
          StringBuilder sb = new StringBuilder();

          for (EmployeeDO sub : substitutions) {
            sb.append(", ");
            sb.append(sub.getUser().getFullname());
          }

          if (sb.length() > 2) {
            mapping.add(field.getName(), sb.substring(2));
          } else {
            mapping.add(field.getName(), "");
          }
        } else {
          super.addMapping(mapping, entry, field);
        }
      }
    };
  }

  @Override
  protected VacationListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new VacationListForm(this);
  }

  @Override
  public VacationService getBaseDao()
  {
    return vacationService;
  }

  protected VacationService getEmployeeDao()
  {
    return vacationService;
  }

}
