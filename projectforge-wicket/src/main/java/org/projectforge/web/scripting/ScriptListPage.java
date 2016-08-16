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

package org.projectforge.web.scripting;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.scripting.ScriptDO;
import org.projectforge.business.scripting.ScriptDao;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;

@ListPage(editPage = ScriptEditPage.class)
public class ScriptListPage extends AbstractListPage<ScriptListForm, ScriptDao, ScriptDO>
{
  private static final long serialVersionUID = -8406452960003792763L;

  @SpringBean
  private ScriptDao scriptDao;

  public ScriptListPage(final PageParameters parameters)
  {
    super(parameters, "scripting");
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    final List<IColumn<ScriptDO, String>> columns = new ArrayList<IColumn<ScriptDO, String>>();
    final CellItemListener<ScriptDO> cellItemListener = new CellItemListener<ScriptDO>()
    {
      public void populateItem(final Item<ICellPopulator<ScriptDO>> item, final String componentId,
          final IModel<ScriptDO> rowModel)
      {
        final ScriptDO script = rowModel.getObject();
        appendCssClasses(item, script.getId(), script.isDeleted());
      }
    };
    columns.add(new CellItemListenerPropertyColumn<ScriptDO>(new Model<String>(getString("scripting.script.name")),
        "name", "name",
        cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<ScriptDO>> item, final String componentId,
          final IModel<ScriptDO> rowModel)
      {
        final ScriptDO script = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, ScriptExecutePage.class, script.getId(),
            ScriptListPage.this, script
                .getName()));
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<ScriptDO>(new Model<String>(getString("description")), "description",
        "description",
        cellItemListener));
    columns.add(new AbstractColumn<ScriptDO, String>(new Model<String>(getString("scripting.script.parameter")))
    {
      public void populateItem(final Item<ICellPopulator<ScriptDO>> cellItem, final String componentId,
          final IModel<ScriptDO> rowModel)
      {
        final ScriptDO script = rowModel.getObject();
        final Label label = new Label(componentId, new Model<String>(script.getParameterNames(true)));
        cellItem.add(label);
        cellItemListener.populateItem(cellItem, componentId, rowModel);
      }
    });
    dataTable = createDataTable(columns, "name", SortOrder.ASCENDING);
    form.add(dataTable);
  }

  @Override
  protected ScriptListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new ScriptListForm(this);
  }

  @Override
  public ScriptDao getBaseDao()
  {
    return scriptDao;
  }
}
