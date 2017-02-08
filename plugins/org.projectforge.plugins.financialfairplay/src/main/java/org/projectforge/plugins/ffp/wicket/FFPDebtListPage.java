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

package org.projectforge.plugins.ffp.wicket;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.plugins.ffp.model.FFPDebtDO;
import org.projectforge.plugins.ffp.repository.FFPDebtDao;
import org.projectforge.plugins.ffp.repository.FFPEventService;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.flowlayout.ButtonPanel;
import org.projectforge.web.wicket.flowlayout.ButtonType;

public class FFPDebtListPage extends AbstractListPage<FFPDebtListForm, FFPDebtDao, FFPDebtDO> implements
    IListPageColumnsCreator<FFPDebtDO>
{
  private static final long serialVersionUID = -8406452960003792763L;

  @SpringBean
  private FFPEventService eventService;

  @SpringBean
  private EmployeeService employeeService;

  protected EmployeeDO currentEmployee;

  public FFPDebtListPage(final PageParameters parameters)
  {
    super(parameters, "plugins.ffp");
  }

  @Override
  public List<IColumn<FFPDebtDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<FFPDebtDO, String>> columns = new ArrayList<>();
    columns.add(new PropertyColumn<FFPDebtDO, String>(new ResourceModel("plugins.ffp.eventDate"), "event.eventDate"));
    columns.add(new PropertyColumn<FFPDebtDO, String>(new ResourceModel("plugins.ffp.title"), "event.title"));
    columns.add(new PropertyColumn<FFPDebtDO, String>(new ResourceModel("plugins.ffp.from"), "from.user.fullname"));
    columns.add(new PropertyColumn<FFPDebtDO, String>(new ResourceModel("plugins.ffp.to"), "to.user.fullname"));
    columns.add(new CellItemListenerPropertyColumn<FFPDebtDO>(FFPDebtDO.class, "plugins.ffp.value", "value", null));

    columns.add(new CellItemListenerPropertyColumn<FFPDebtDO>(FFPDebtDO.class, "plugins.ffp.approvedByFrom", "approvedByFrom", null)
    {
      private static final long serialVersionUID = 3672950740712610620L;

      @Override
      public void populateItem(Item<ICellPopulator<FFPDebtDO>> item, String componentId,
          IModel<FFPDebtDO> rowModel)
      {
        FFPDebtDO debt = rowModel.getObject();
        Button button = new Button(ButtonPanel.BUTTON_ID);
        if (debt.getFrom().equals(currentEmployee)) {
          button.setOutputMarkupId(true);
          button.add(new AjaxEventBehavior("click")
          {
            @Override
            protected void onEvent(AjaxRequestTarget target)
            {
              if (debt.isApprovedByFrom() == false) {
                eventService.updateDebtFrom(debt);
                button.add(AttributeModifier.append("class", ButtonType.GREEN.getClassAttrValue()));
                button.addOrReplace(new Label("title", I18nHelper.getLocalizedMessage("plugins.ffp.approved")));
                target.add(button);
              }
            }
          });
        }
        String label = debt.isApprovedByFrom() ?
            I18nHelper.getLocalizedMessage("plugins.ffp.approved") :
            I18nHelper.getLocalizedMessage("plugins.ffp.notApproved");
        ButtonType bt = debt.isApprovedByFrom() ? ButtonType.GREEN : ButtonType.RED;
        ButtonPanel buttonPanel = new ButtonPanel(componentId, label, button, bt);
        item.add(buttonPanel);
      }

    });
    columns.add(new CellItemListenerPropertyColumn<FFPDebtDO>(FFPDebtDO.class, I18nHelper.getLocalizedMessage("plugins.ffp.approvedByTo"), "approvedByTo", null)
    {
      private static final long serialVersionUID = 367295074123610620L;

      @Override
      public void populateItem(Item<ICellPopulator<FFPDebtDO>> item, String componentId,
          IModel<FFPDebtDO> rowModel)
      {
        FFPDebtDO debt = rowModel.getObject();
        Button button = new Button(ButtonPanel.BUTTON_ID);
        if (debt.isApprovedByFrom() && debt.getTo().equals(currentEmployee)) {
          button.setOutputMarkupId(true);
          button.add(new AjaxEventBehavior("click")
          {
            @Override
            protected void onEvent(AjaxRequestTarget target)
            {
              if (debt.isApprovedByTo() == false) {
                eventService.updateDebtTo(debt);
                button.add(AttributeModifier.append("class", ButtonType.GREEN.getClassAttrValue()));
                button.addOrReplace(new Label("title", I18nHelper.getLocalizedMessage("plugins.ffp.approved")));
                target.add(button);
              }
            }
          });
        }
        String label = debt.isApprovedByTo() ?
            I18nHelper.getLocalizedMessage("plugins.ffp.approved") :
            I18nHelper.getLocalizedMessage("plugins.ffp.notApproved");
        ButtonType bt = debt.isApprovedByTo() ? ButtonType.GREEN : ButtonType.RED;
        ButtonPanel buttonPanel = new ButtonPanel(componentId, label, button, bt);
        item.add(buttonPanel);
      }

    });
    return columns;
  }

  @Override
  protected void init()
  {
    newItemMenuEntry.setVisible(false);
    currentEmployee = employeeService.getEmployeeByUserId(ThreadLocalUserContext.getUserId());
    if (currentEmployee == null) {
      throw new AccessException("access.exception.noEmployeeToUser");
    }
    final List<IColumn<FFPDebtDO, String>> columns = createColumns(this, true);
    dataTable = createDataTable(columns, "title", SortOrder.ASCENDING);
    form.add(dataTable);
  }

  @Override
  protected FFPDebtListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new FFPDebtListForm(this);
  }

  @Override
  public FFPDebtDao getBaseDao()
  {
    return eventService.getDebtDao();
  }

}
