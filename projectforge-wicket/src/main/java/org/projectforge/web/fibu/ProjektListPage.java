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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.KontoCache;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.kost.KostCache;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.reporting.Kost2Art;
import org.projectforge.reporting.impl.ProjektImpl;
import org.projectforge.web.task.TaskPropertyColumn;
import org.projectforge.web.user.UserPrefListPage;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

@ListPage(editPage = ProjektEditPage.class)
public class ProjektListPage extends AbstractListPage<ProjektListForm, ProjektDao, ProjektDO>
    implements IListPageColumnsCreator<ProjektDO>
{
  private static final long serialVersionUID = -8406452960003792763L;

  @SpringBean
  private ProjektDao projektDao;

  @SpringBean
  private KontoCache kontoCache;

  @SpringBean
  private KostCache kostCache;

  @SpringBean
  private GroupService groupService;

  public ProjektListPage(final PageParameters parameters)
  {
    super(parameters, "fibu.projekt");
  }

  public ProjektListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "fibu.projekt");
  }

  @SuppressWarnings("serial")
  @Override
  public List<IColumn<ProjektDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<ProjektDO, String>> columns = new ArrayList<IColumn<ProjektDO, String>>();
    final CellItemListener<ProjektDO> cellItemListener = new CellItemListener<ProjektDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<ProjektDO>> item, final String componentId,
          final IModel<ProjektDO> rowModel)
      {
        final ProjektDO projekt = rowModel.getObject();
        if (projekt.getStatus() == null) {
          // Should not occur:
          return;
        }
        appendCssClasses(item, projekt.getId(), projekt.isDeleted());
      }
    };
    columns.add(new CellItemListenerPropertyColumn<ProjektDO>(new Model<String>(getString("fibu.projekt.nummer")),
        getSortable("kost",
            sortable),
        "kost", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<ProjektDO>> item, final String componentId,
          final IModel<ProjektDO> rowModel)
      {
        final ProjektDO projekt = rowModel.getObject();
        if (isSelectMode() == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, ProjektEditPage.class, projekt.getId(),
              returnToPage, String
                  .valueOf(projekt.getKost())));
        } else {
          item.add(new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, projekt.getId(),
              String.valueOf(projekt
                  .getKost())));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<ProjektDO>(new Model<String>(getString("fibu.projekt.identifier")),
        getSortable(
            "identifier", sortable),
        "identifier", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<ProjektDO>(new Model<String>(getString("fibu.kunde.name")),
        getSortable("kunde.name",
            sortable),
        "kunde.name", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<ProjektDO>(new Model<String>(getString("fibu.projekt.name")),
        getSortable("name",
            sortable),
        "name", cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<ProjektDO>(new Model<String>(getString("fibu.kunde.division")), getSortable(
            "kunde.division", sortable), "kunde.division", cellItemListener));
    columns.add(
        new TaskPropertyColumn<ProjektDO>(getString("task"), getSortable("task.title", sortable), "task",
            cellItemListener));
    if (kontoCache.isEmpty() == false) {
      columns
          .add(new CellItemListenerPropertyColumn<ProjektDO>(new Model<String>(getString("fibu.konto")), null, "konto",
              cellItemListener)
          {
            /**
             * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
             *      java.lang.String, org.apache.wicket.model.IModel)
             */
            @Override
            public void populateItem(final Item<ICellPopulator<ProjektDO>> item, final String componentId,
                final IModel<ProjektDO> rowModel)
            {
              final ProjektDO projekt = rowModel.getObject();
              final KontoDO konto = kontoCache.getKonto(projekt);
              item.add(new Label(componentId, konto != null ? konto.formatKonto() : ""));
              cellItemListener.populateItem(item, componentId, rowModel);
            }
          });
    }
    columns.add(new CellItemListenerPropertyColumn<ProjektDO>(new Model<String>(getString("status")),
        getSortable("status", sortable),
        "status", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<ProjektDO>(
        new Model<String>(getString("fibu.projekt.projektManagerGroup")), null,
        "projektManagerGroup", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<ProjektDO>> item, final String componentId,
          final IModel<ProjektDO> rowModel)
      {
        final ProjektDO projektDO = rowModel.getObject();
        String groupName = "";
        if (projektDO.getProjektManagerGroupId() != null) {
          final GroupDO group = groupService.getGroup(projektDO.getProjektManagerGroupId());
          if (group != null) {
            groupName = group.getName();
          }
        }
        final Label label = new Label(componentId, groupName);
        item.add(label);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(
        new CellItemListenerPropertyColumn<ProjektDO>(new Model<String>(getString("fibu.kost2art.kost2arten")), null,
            "kost2ArtsAsHtml", cellItemListener)
        {
          /**
           * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
           *      java.lang.String, org.apache.wicket.model.IModel)
           */
          @Override
          public void populateItem(final Item<ICellPopulator<ProjektDO>> item, final String componentId,
              final IModel<ProjektDO> rowModel)
          {
            final ProjektDO projektDO = rowModel.getObject();
            final ProjektImpl projekt = new ProjektImpl(projektDO);
            final List<Kost2Art> kost2Arts = kostCache.getAllKost2Arts(projektDO.getId());
            projekt.setKost2Arts(kost2Arts);
            final Label label = new Label(componentId, new Model<String>(projekt.getKost2ArtsAsHtml()));
            label.setEscapeModelStrings(false);
            item.add(label);
            cellItemListener.populateItem(item, componentId, rowModel);
          }
        });
    columns.add(new CellItemListenerPropertyColumn<ProjektDO>(new Model<String>(getString("description")),
        getSortable("description",
            sortable),
        "description", cellItemListener));
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "kost", SortOrder.ASCENDING);
    form.add(dataTable);
    final BookmarkablePageLink<Void> addTemplatesLink = UserPrefListPage.createLink("link",
        UserPrefArea.PROJEKT_FAVORITE);
    final ContentMenuEntryPanel menuEntry = new ContentMenuEntryPanel(getNewContentMenuChildId(), addTemplatesLink,
        getString("favorites"));
    addContentMenuEntry(menuEntry);
  }

  @Override
  protected ProjektListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new ProjektListForm(this);
  }

  @Override
  public ProjektDao getBaseDao()
  {
    return projektDao;
  }

  protected ProjektDao getProjektDao()
  {
    return projektDao;
  }
}
