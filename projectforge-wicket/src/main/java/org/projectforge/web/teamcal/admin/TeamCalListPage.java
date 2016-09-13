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

package org.projectforge.web.teamcal.admin;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.TeamCalFilter;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.admin.right.TeamCalRight;
import org.projectforge.business.teamcal.service.CalendarFeedService;
import org.projectforge.web.calendar.AbstractICSExportDialog;
import org.projectforge.web.teamcal.dialog.TeamCalICSExportDialog;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.projectforge.web.wicket.flowlayout.AjaxIconLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
@ListPage(editPage = TeamCalEditPage.class)
public class TeamCalListPage extends AbstractListPage<TeamCalListForm, TeamCalDao, TeamCalDO>
    implements IListPageColumnsCreator<TeamCalDO>
{
  private static final long serialVersionUID = 1749480610890950450L;

  @SpringBean
  private TeamCalDao teamCalDao;

  @SpringBean
  CalendarFeedService calendarFeedService;

  private TeamCalICSExportDialog icsExportDialog;

  private final boolean isAdminUser;

  /**
   * 
   */
  public TeamCalListPage(final PageParameters parameters)
  {
    super(parameters, "plugins.teamcal");
    isAdminUser = accessChecker.isLoggedInUserMemberOfAdminGroup();
  }

  /**
   * @see org.projectforge.web.wicket.IListPageColumnsCreator#createColumns(org.apache.wicket.markup.html.WebPage,
   *      boolean)
   */
  @SuppressWarnings("serial")
  @Override
  public List<IColumn<TeamCalDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<TeamCalDO, String>> columns = new ArrayList<IColumn<TeamCalDO, String>>();

    final CellItemListener<TeamCalDO> cellItemListener = new CellItemListener<TeamCalDO>()
    {
      public void populateItem(final Item<ICellPopulator<TeamCalDO>> item, final String componentId,
          final IModel<TeamCalDO> rowModel)
      {
        final TeamCalDO teamCal = rowModel.getObject();
        appendCssClasses(item, teamCal.getId(), teamCal.isDeleted());
      }
    };

    columns.add(new CellItemListenerPropertyColumn<TeamCalDO>(getString("plugins.teamcal.title"),
        getSortable("title", sortable), "title",
        cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<TeamCalDO>> item, final String componentId,
          final IModel<TeamCalDO> rowModel)
      {
        final TeamCalDO teamCal = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, TeamCalEditPage.class, teamCal.getId(), returnToPage,
            teamCal.getTitle()));
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<TeamCalDO>(getString("plugins.teamcal.externalsubscription.label"),
        getSortable(
            "externalSubscriptionUrlAnonymized", sortable),
        "externalSubscriptionUrlAnonymized", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<TeamCalDO>> item, final String componentId,
          final IModel<TeamCalDO> rowModel)
      {
        final TeamCalDO teamCal = rowModel.getObject();
        item.add(new Label(componentId, StringUtils.defaultString(teamCal.getExternalSubscriptionUrlAnonymized())));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });

    columns.add(new CellItemListenerPropertyColumn<TeamCalDO>(getString("plugins.teamcal.description"),
        getSortable("description", sortable), "description", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<TeamCalDO>(getString("plugins.teamcal.owner"),
        getSortable("owner", sortable),
        "owner.username", cellItemListener));
    columns.add(new AbstractColumn<TeamCalDO, String>(new Model<String>(getString("plugins.teamcal.access")))
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<TeamCalDO>> item, final String componentId,
          final IModel<TeamCalDO> rowModel)
      {
        final TeamCalDO teamCal = rowModel.getObject();
        final TeamCalRight right = (TeamCalRight) teamCalDao.getUserRight();
        String label;
        if (right.isOwner(getUser(), teamCal) == true) {
          label = getString("plugins.teamcal.owner");
        } else if (right.hasFullAccess(teamCal, getUserId()) == true) {
          label = getString("plugins.teamcal.fullAccess");
        } else if (right.hasReadonlyAccess(teamCal, getUserId()) == true) {
          label = getString("plugins.teamcal.readonlyAccess");
        } else if (right.hasMinimalAccess(teamCal, getUserId()) == true) {
          label = getString("plugins.teamcal.minimalAccess");
        } else if (isAdminUser == true) {
          label = getString("plugins.teamcal.adminAccess");
        } else {
          label = "???";
        }
        item.add(new Label(componentId, label));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<TeamCalDO>(getString("lastUpdate"),
        getSortable("lastUpdate", sortable), "lastUpdate",
        cellItemListener));
    if (isCalledBySearchPage() == false) {
      // Don't call by search page, because there is no form to show the popup-dialog.
      // ics export buttons
      columns.add(new AbstractColumn<TeamCalDO, String>(new Model<String>(getString("plugins.teamcal.subscription")))
      {
        /**
         * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
         *      java.lang.String, org.apache.wicket.model.IModel)
         */
        @Override
        public void populateItem(final Item<ICellPopulator<TeamCalDO>> item, final String componentId,
            final IModel<TeamCalDO> rowModel)
        {
          if (accessChecker.isRestrictedUser() == false) {
            final TeamCalDO teamCal = rowModel.getObject();
            item.add(new AjaxIconLinkPanel(componentId, IconType.SUBSCRIPTION,
                new ResourceModel("plugins.teamcal.subscription.tooltip"))
            {
              @Override
              public void onClick(final AjaxRequestTarget target)
              {
                icsExportDialog.redraw(teamCal);
                icsExportDialog.addContent(target);
                icsExportDialog.setCalendarTitle(target, teamCal.getTitle());
                icsExportDialog.open(target);
              };
            });
          }
        }
      });
    }
    return columns;
  }

  protected TeamCalFilter getFilter()
  {
    return form.getFilter();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#getBaseDao()
   */
  @Override
  public TeamCalDao getBaseDao()
  {
    return teamCalDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#newListForm(org.projectforge.web.wicket.AbstractListPage)
   */
  @Override
  protected TeamCalListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new TeamCalListForm(this);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#init()
   */
  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    final MyICSExportDialog exportDialog = new MyICSExportDialog(newModalDialogId());
    TeamCalListPage.this.add(exportDialog);
    exportDialog.init();
    {
      final ContentMenuEntryPanel menuEntry = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new AjaxLink<Void>("link")
          {
            @Override
            public void onClick(final AjaxRequestTarget target)
            {
              openExportICSDialog(exportDialog, target, "plugins.teamcal.export.timesheets",
                  calendarFeedService.getUrl4Timesheets(getUserId()));
            };
          }, getString("plugins.teamcal.export.timesheets"));
      menuEntry.setMarkupId("exportTimesheets").setOutputMarkupId(true);
      addContentMenuEntry(menuEntry);
    }
    {
      final ContentMenuEntryPanel menuEntry = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new AjaxLink<Void>("link")
          {
            @Override
            public void onClick(final AjaxRequestTarget target)
            {
              openExportICSDialog(exportDialog, target, "plugins.teamcal.export.holidays",
                  calendarFeedService.getUrl4Holidays());
            };
          }, getString("plugins.teamcal.export.holidays"))
              .setTooltip(getString("plugins.teamcal.export.holidays.tooltip"));
      menuEntry.setMarkupId("exportHolidays").setOutputMarkupId(true);
      addContentMenuEntry(menuEntry);
    }
    {
      final ContentMenuEntryPanel menuEntry = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new AjaxLink<Void>("link")
          {
            @Override
            public void onClick(final AjaxRequestTarget target)
            {
              openExportICSDialog(exportDialog, target, "plugins.teamcal.export.weekOfYears",
                  calendarFeedService.getUrl4WeekOfYears());
            };
          }, getString("plugins.teamcal.export.weekOfYears"))
              .setTooltip(getString("plugins.teamcal.export.weekOfYears.tooltip"));
      menuEntry.setMarkupId("exportWeekOfYears").setOutputMarkupId(true);
      addContentMenuEntry(menuEntry);
    }

    dataTable = createDataTable(createColumns(this, true), "title", SortOrder.ASCENDING);
    form.add(dataTable);
    icsExportDialog = new TeamCalICSExportDialog(newModalDialogId());
    add(icsExportDialog);
    icsExportDialog.init();
  }

  private void openExportICSDialog(final MyICSExportDialog exportDialog, final AjaxRequestTarget target,
      final String titleKey,
      final String url)
  {
    exportDialog.title = getString(titleKey);
    exportDialog.addContent(target).addTitleLabel(target);
    exportDialog.redraw();
    exportDialog.url = url;
    exportDialog.open(target);
  }

  class MyICSExportDialog extends AbstractICSExportDialog
  {
    private static final long serialVersionUID = -1829962601576743330L;

    String url;

    String title;

    @SuppressWarnings("serial")
    public MyICSExportDialog(final String id)
    {
      super(id, null);
      setTitle(new Model<String>()
      {
        /**
         * @see org.apache.wicket.model.Model#getObject()
         */
        @Override
        public String getObject()
        {
          return title;
        }
      });
    }

    /**
     * @see org.projectforge.web.calendar.AbstractICSExportDialog#getUrl()
     */
    @Override
    protected String getUrl()
    {
      return url;
    }
  };
}
