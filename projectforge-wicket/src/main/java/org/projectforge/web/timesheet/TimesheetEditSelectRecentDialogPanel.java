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

package org.projectforge.web.timesheet;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.business.common.OutputType;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.task.formatter.WicketTaskFormatter;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.dialog.ModalDialog;
import org.projectforge.web.task.TaskPropertyColumn;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.RowCssClass;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.TablePanel;

public class TimesheetEditSelectRecentDialogPanel extends ModalDialog
{
  private static final long serialVersionUID = -9175062586210446142L;

  private final boolean showCost2Column;

  private final UserFormatter userFormatter;

  private final TimesheetDao timesheetDao;

  private final TaskTree taskTree;

  private final TimesheetEditPage parentPage;

  private final TimesheetEditForm form;

  /**
   * @param modalWindow
   * @param title
   */
  public TimesheetEditSelectRecentDialogPanel(final String id, final String title, final TimesheetEditPage parentPage,
      final TimesheetEditForm form, final boolean showCost2Column, final TimesheetDao timesheetDao,
      final TaskTree taskTree,
      final UserFormatter userFormatter)
  {
    super(id);
    setTitle(title);
    this.parentPage = parentPage;
    this.form = form;
    this.showCost2Column = showCost2Column;
    this.timesheetDao = timesheetDao;
    this.taskTree = taskTree;
    this.userFormatter = userFormatter;
    setBigWindow();
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#init()
   */
  @Override
  public void init()
  {
    init(new Form<String>(getFormId()));
    addRecentSheetsTable();
  }

  @SuppressWarnings({ "serial" })
  private void addRecentSheetsTable()
  {
    final List<IColumn<TimesheetDO, String>> columns = new ArrayList<IColumn<TimesheetDO, String>>();
    final CellItemListener<TimesheetDO> cellItemListener = new CellItemListener<TimesheetDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
          final IModel<TimesheetDO> rowModel)
      {
        final TimesheetDO timesheet = rowModel.getObject();
        final int rowIndex = ((Item<?>) item.findParent(Item.class)).getIndex();
        String cssClasses = null;
        if (timesheet.isDeleted() == true) {
          cssClasses = RowCssClass.MARKED_AS_DELETED.getCssClass();
        } else if (rowIndex < TimesheetEditPage.SIZE_OF_FIRST_RECENT_BLOCK) {
          cssClasses = RowCssClass.IMPORTANT_ROW.getCssClass();
        }
        if (cssClasses != null) {
          item.add(AttributeModifier.append("class", cssClasses));
        }
      }
    };
    if (showCost2Column == true) { // Is maybe invisible but does always exist if cost2 entries does exist in the system.
      columns
          .add(new CellItemListenerPropertyColumn<TimesheetDO>(getString("fibu.kost2"), null, "kost2.shortDisplayName",
              cellItemListener)
          {
            @Override
            public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
                final IModel<TimesheetDO> rowModel)
            {
              final TimesheetDO timesheet = rowModel.getObject();
              final ListSelectActionPanel actionPanel = new ListSelectActionPanel(componentId,
                  createRecentTimeSheetSelectionLink(timesheet), new Model<String>()
              {
                @Override
                public String getObject()
                {
                  final StringBuffer buf = new StringBuffer();
                  if (timesheet.getKost2() != null) {
                    buf.append(timesheet.getKost2().getShortDisplayName());
                  }
                  if (timesheet.getUserId() != null
                      && timesheet.getUserId().equals(ThreadLocalUserContext.getUserId()) == false) {
                    if (timesheet.getKost2() != null) {
                      buf.append(", ");
                    }
                    buf.append(userFormatter.getFormattedUser(timesheet.getUserId()));
                  }
                  return buf.toString();
                }
              });
              item.add(actionPanel);
              item.add(AttributeModifier.append("style", new Model<String>("white-space: nowrap;")));
              final Item<?> row = item.findParent(Item.class);
              WicketUtils.addRowClick(row);
              cellItemListener.populateItem(item, componentId, rowModel);
            }
          });
      columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(new Model<String>(getString("fibu.kunde")), null,
          "kost2.projekt.kunde.name", cellItemListener));
      columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(new Model<String>(getString("fibu.projekt")), null,
          "kost2.projekt.name",
          cellItemListener));
      columns.add(new TaskPropertyColumn<TimesheetDO>(getString("task"), null, "task", cellItemListener)
          .withTaskTree(taskTree));
    } else {
      columns
          .add(new CellItemListenerPropertyColumn<TimesheetDO>(new Model<String>(getString("task")), null, "task.title",
              cellItemListener)
          {
            @Override
            public void populateItem(final Item<ICellPopulator<TimesheetDO>> item, final String componentId,
                final IModel<TimesheetDO> rowModel)
            {
              final TimesheetDO timesheet = rowModel.getObject();
              final TaskDO task = rowModel.getObject().getTask();
              final Label label = new Label("label", task != null ? task.getTitle() : "");
              final ListSelectActionPanel actionPanel = new ListSelectActionPanel(componentId,
                  createRecentTimeSheetSelectionLink(timesheet),
                  label);
              WicketUtils.addTooltip(label, WicketTaskFormatter.getTaskPath(task.getId(), false, OutputType.HTML));
              item.add(actionPanel);
              final Item<?> row = item.findParent(Item.class);
              WicketUtils.addRowClick(row);
              cellItemListener.populateItem(item, componentId, rowModel);
            }
          });
    }
    columns.add(new CellItemListenerPropertyColumn<TimesheetDO>(getString("timesheet.location"), null, "location",
        cellItemListener)
    {

    });
    columns.add(
        new CellItemListenerPropertyColumn<TimesheetDO>(getString("timesheet.description"), null, "shortDescription",
            cellItemListener));
    final DivPanel panel = gridBuilder.getPanel();
    final TablePanel table = new TablePanel(panel.newChildId());
    panel.add(table);
    final IDataProvider<TimesheetDO> dataProvider = new ListDataProvider<TimesheetDO>(parentPage.getRecentTimesheets());
    final DataTable<TimesheetDO, String> dataTable = new DataTable<TimesheetDO, String>(TablePanel.TABLE_ID, columns,
        dataProvider, 100)
    {
      @Override
      protected Item<TimesheetDO> newRowItem(final String id, final int index, final IModel<TimesheetDO> model)
      {
        return new OddEvenItem<TimesheetDO>(id, index, model);
      }
    };
    final HeadersToolbar headersToolbar = new HeadersToolbar(dataTable, null);
    dataTable.addTopToolbar(headersToolbar);
    table.add(dataTable);
  }

  /**
   * Submit link is needed to submit former changed input fields on selection.
   *
   * @param timesheet
   * @return
   */
  @SuppressWarnings("serial")
  private AjaxLink<Void> createRecentTimeSheetSelectionLink(final TimesheetDO timesheet)
  {
    return new AjaxLink<Void>(ListSelectActionPanel.LINK_ID)
    {
      @Override
      public void onClick(final AjaxRequestTarget target)
      {
        if (target != null) {
          form.getData().setLocation(timesheet.getLocation());
          form.getData().setDescription(timesheet.getDescription());
          timesheetDao.setTask(form.getData(), timesheet.getTaskId());
          timesheetDao.setUser(form.getData(), timesheet.getUserId());
          timesheetDao.setKost2(form.getData(), timesheet.getKost2Id());
          if (form.cost2ChoicePanel != null && form.cost2ChoicePanel.getDropDownChoice() != null) {
            form.cost2ChoicePanel.getDropDownChoice().modelChanged();
          }
          form.locationTextField.modelChanged();
          form.descriptionArea.modelChanged();
          form.userSelectPanel.markTextFieldModelAsChanged();
          // updateStopDate();
          form.refresh();
          // modalWindow.close(target);
          parentPage.setResponsePage(parentPage);
        }
      }
    };
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#renderHead(org.apache.wicket.markup.head.IHeaderResponse)
   */
  @Override
  public void renderHead(final IHeaderResponse response)
  {
    super.renderHead(response);
    final String initJS = // Mache alle Zeilen von recentSheets klickbar\n"
        "  $(\".dataview td\").click( function() {\n" //
            + "    $(this).parent().find(\"a:first\").click();\n"
            + "  });\n";
    response.render(OnDomReadyHeaderItem.forScript(initJS));
  }
}
