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

package org.projectforge.plugins.skillmatrix;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.wicket.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Billy Duong (b.duong@micromata.de)
 * 
 */
@ListPage(editPage = SkillEditPage.class)
public class SkillListPage extends AbstractListPage<SkillListForm, SkillDao, SkillDO>
    implements IListPageColumnsCreator<SkillDO>
{
  private static final long serialVersionUID = 3262800972072452074L;

  @SpringBean
  private SkillDao skillDao;

  public static final String I18N_KEY_PREFIX = "plugins.skillmatrix";

  private SkillTreePage skillTreePage;

  public SkillListPage(final PageParameters parameters)
  {
    super(parameters, I18N_KEY_PREFIX);
  }

  /**
   * @param skillTreePage2
   * @param pageParameters
   */
  public SkillListPage(final SkillTreePage skillTreePage, final PageParameters pageParameters)
  {
    super(pageParameters, I18N_KEY_PREFIX);
    this.skillTreePage = skillTreePage;
  }

  /**
   * @see org.projectforge.web.wicket.IListPageColumnsCreator#createColumns(org.apache.wicket.markup.html.WebPage,
   *      boolean)
   */
  @SuppressWarnings("serial")
  @Override
  public List<IColumn<SkillDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<SkillDO, String>> columns = new ArrayList<>();
    final CellItemListener<SkillDO> cellItemListener = new CellItemListener<SkillDO>()
    {
      private static final long serialVersionUID = 3628573642359696336L;

      public void populateItem(final Item<ICellPopulator<SkillDO>> item, final String componentId,
          final IModel<SkillDO> rowModel)
      {
        final SkillDO skill = rowModel.getObject();
        appendCssClasses(item, skill.getId(), skill.isDeleted());
      }
    };

    final CellItemListenerPropertyColumn<SkillDO> created = new CellItemListenerPropertyColumn<SkillDO>(
        new Model<>(
            getString("created")),
        getSortable("created", sortable), "created", cellItemListener)
    {
      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public void populateItem(final Item item, final String componentId, final IModel rowModel)
      {
        final SkillDO skill = (SkillDO) rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, SkillEditPage.class, skill.getId(), returnToPage,
            DateTimeFormatter
                .instance().getFormattedDateTime(skill.getCreated())));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    };

    final CellItemListenerPropertyColumn<SkillDO> lastUpdate = new CellItemListenerPropertyColumn<>(
        getString("lastUpdate"),
        getSortable("lastUpdate", sortable), "lastUpdate", cellItemListener);

    final CellItemListenerPropertyColumn<SkillDO> title = new CellItemListenerPropertyColumn<>(SkillDO.class,
        getSortable("title",
            sortable),
        "title", cellItemListener);

    // TODO: Workaround with get (hardcoded I18N), needs a better solution.
    final CellItemListenerPropertyColumn<SkillDO> parentTitle = new CellItemListenerPropertyColumn<>(
        getString("plugins.skillmatrix.skill.parent"), getSortable("parent.title", sortable), "parent.title",
        cellItemListener);

    final CellItemListenerPropertyColumn<SkillDO> description = new CellItemListenerPropertyColumn<>(
        SkillDO.class, getSortable(
        "description", sortable),
        "description", cellItemListener);

    final CellItemListenerPropertyColumn<SkillDO> comment = new CellItemListenerPropertyColumn<>(SkillDO.class,
        getSortable(
            "comment", sortable),
        "comment", cellItemListener);

    final CellItemListenerPropertyColumn<SkillDO> rateable = new CellItemListenerPropertyColumn<>(SkillDO.class,
        getSortable(
            "rateable", sortable),
        "rateable", cellItemListener);

    columns.add(title);
    columns.add(parentTitle);
    columns.add(description);
    columns.add(comment);
    columns.add(rateable);
    columns.add(created);
    columns.add(lastUpdate);

    return columns;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#init()
   */
  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "lastUpdate", SortOrder.DESCENDING);
    form.add(dataTable);
  }

  void onTreeViewSubmit()
  {
    if (skillTreePage != null) {
      setResponsePage(skillTreePage);
    } else {
      setResponsePage(new SkillTreePage(this, getPageParameters()));
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#getBaseDao()
   */
  @Override
  public SkillDao getBaseDao()
  {
    return skillDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#newListForm(org.projectforge.web.wicket.AbstractListPage)
   */
  @Override
  protected SkillListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new SkillListForm(this);
  }

  static void appendCssClasses(final Item<?> item, final SkillDO skill, final Integer preselectedSkillNode)
  {
    appendCssClasses(item, skill.getId(), preselectedSkillNode, skill.isDeleted());
  }
}
