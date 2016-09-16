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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.fibu.EmployeeEditPage;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;

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
    final List<IColumn<VacationDO, String>> columns = new ArrayList<IColumn<VacationDO, String>>();

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
    columns.add(new CellItemListenerPropertyColumn<VacationDO>(new ResourceModel("name"),
        getSortable("user.lastname", sortable),
        "user.lastname", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
          final IModel<VacationDO> rowModel)
      {
        final VacationDO employee = rowModel.getObject();
        final PFUserDO user = employee.getUser();
        final String lastname = user != null ? user.getLastname() : null;
        if (isSelectMode() == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, EmployeeEditPage.class, employee.getId(),
              returnToPage, lastname));
        } else {
          item.add(
              new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, employee.getId(), lastname));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<VacationDO>(new ResourceModel("firstName"),
        getSortable("user.firstname", sortable),
        "user.firstname", cellItemListener));
    columns
        .add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, getSortable("status", sortable), "status",
            cellItemListener));
    columns
        .add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, getSortable("staffNumber", sortable),
            "staffNumber",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<VacationDO>(new ResourceModel("fibu.kost1"),
        getSortable("kost1.shortDisplayName",
            sortable),
        "kost1.shortDisplayName", cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, getSortable("position", sortable), "position",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, getSortable("abteilung", sortable),
        "abteilung",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class,
        getSortable("eintrittsDatum", sortable), "eintrittsDatum",
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
          final IModel<VacationDO> rowModel)
      {
        final VacationDO employee = rowModel.getObject();
        item.add(new Label(componentId, DateTimeFormatter.instance().getFormattedDate(employee.getEintrittsDatum())));
      }
    });
    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class,
        getSortable("austrittsDatum", sortable), "austrittsDatum",
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
          final IModel<VacationDO> rowModel)
      {
        final VacationDO employee = rowModel.getObject();
        item.add(new Label(componentId, DateTimeFormatter.instance().getFormattedDate(employee.getAustrittsDatum())));
      }
    });
    columns.add(
        new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, getSortable("comment", sortable), "comment",
            cellItemListener));
    return columns;
  }

  @Override
  protected void init()
  {
    final List<IColumn<VacationDO, String>> columns = createColumns(this, true);
    dataTable = createDataTable(columns, "user.lastname", SortOrder.ASCENDING);
    form.add(dataTable);
    addExcelExport(getString("fibu.employee.title.heading"), "employees");
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
