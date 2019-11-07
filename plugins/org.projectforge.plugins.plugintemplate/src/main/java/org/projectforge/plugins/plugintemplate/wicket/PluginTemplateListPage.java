/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.plugintemplate.wicket;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.plugins.plugintemplate.model.PluginTemplateDO;
import org.projectforge.plugins.plugintemplate.repository.PluginTemplateDao;
import org.projectforge.plugins.plugintemplate.service.PluginTemplateService;
import org.projectforge.web.wicket.*;

import java.util.ArrayList;
import java.util.List;

@ListPage(editPage = PluginTemplateEditPage.class)
public class PluginTemplateListPage extends AbstractListPage<PluginTemplateListForm, PluginTemplateDao, PluginTemplateDO> implements
    IListPageColumnsCreator<PluginTemplateDO>
{
  private static final long serialVersionUID = -8406452960003792763L;

  @SpringBean
  private PluginTemplateService pluginTemplateService;

  public PluginTemplateListPage(final PageParameters parameters)
  {
    super(parameters, "plugins.plugintemplate");
  }

  @Override
  @SuppressWarnings("serial")
  public List<IColumn<PluginTemplateDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<PluginTemplateDO, String>> columns = new ArrayList<>();

    final CellItemListener<PluginTemplateDO> cellItemListener = new CellItemListener<PluginTemplateDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<PluginTemplateDO>> item, final String componentId,
          final IModel<PluginTemplateDO> rowModel)
      {
        final PluginTemplateDO event = rowModel.getObject();
        appendCssClasses(item, event.getId(), event.isDeleted());
      }
    };

    columns
        .add(new CellItemListenerPropertyColumn<PluginTemplateDO>(PluginTemplateDO.class, getSortable("key", sortable),
            "key", cellItemListener)
        {
          /**
           * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
           *      java.lang.String, org.apache.wicket.model.IModel)
           */
          @Override
          public void populateItem(final Item<ICellPopulator<PluginTemplateDO>> item, final String componentId,
              final IModel<PluginTemplateDO> rowModel)
          {
            final PluginTemplateDO event = rowModel.getObject();
            if (!isSelectMode()) {
              item.add(new ListSelectActionPanel(componentId, rowModel, PluginTemplateEditPage.class, event.getId(),
                  returnToPage, event.getKey()));
            } else {
              item.add(
                  new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, event.getId(),
                      event.getKey()));
            }
            cellItemListener.populateItem(item, componentId, rowModel);
            item.add(AttributeModifier.replace("style", "width: 50%"));
            addRowClick(item);
          }
        });

    columns
        .add(new CellItemListenerPropertyColumn<>(PluginTemplateDO.class, getSortable("value", sortable),
            "value", cellItemListener));

    columns
        .add(new CellItemListenerPropertyColumn<>(PluginTemplateDO.class, getSortable("created", sortable),
            "created", cellItemListener));

    columns
        .add(new CellItemListenerPropertyColumn<>(PluginTemplateDO.class, getSortable("lastUpdate", sortable),
            "lastUpdate", cellItemListener));

    return columns;
  }

  @Override
  protected void init()
  {
    final List<IColumn<PluginTemplateDO, String>> columns = createColumns(this, true);
    dataTable = createDataTable(columns, "created", SortOrder.DESCENDING);
    form.add(dataTable);
    //To add excel export for list page:
    //addExcelExport(getString("plugins.plugintemplate.title.list"), "pluginTamplete");
  }

  @Override
  protected PluginTemplateListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new PluginTemplateListForm(this);
  }

  @Override
  public PluginTemplateDao getBaseDao()
  {
    return pluginTemplateService.getPluginTemplateDao();
  }

}
