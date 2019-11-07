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
import org.projectforge.business.excel.ExcelExporter;
import org.projectforge.business.excel.PropertyMapping;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.export.DOListExcelExporter;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.user.UserPropertyColumn;
import org.projectforge.web.wicket.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Billy Duong (b.duong@micromata.de)
 */
@ListPage(editPage = SkillRatingEditPage.class)
public class SkillRatingListPage extends AbstractListPage<SkillRatingListForm, SkillRatingDao, SkillRatingDO> implements
    IListPageColumnsCreator<SkillRatingDO>
{

  private static final long serialVersionUID = 3262800972072452074L;

  @SpringBean
  private SkillRatingDao skillRatingDao;

  @SpringBean
  private UserFormatter userFormatter;

  public static final String PARAM_SKILL_ID = "skillId";

  public SkillRatingListPage(final PageParameters parameters)
  {
    super(parameters, "plugins.skillmatrix.rating");
    final Integer skillId = WicketUtils.getAsInteger(parameters, PARAM_SKILL_ID);
    if (NumberHelper.greaterZero(skillId)) {
      form.getSearchFilter().setSkillId(skillId);
    }
  }

  /**
   * @see org.projectforge.web.wicket.IListPageColumnsCreator#createColumns(org.apache.wicket.markup.html.WebPage,
   * boolean)
   */
  @SuppressWarnings("serial")
  @Override
  public List<IColumn<SkillRatingDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<SkillRatingDO, String>> columns = new ArrayList<>();
    final CellItemListener<SkillRatingDO> cellItemListener = new CellItemListener<SkillRatingDO>()
    {
      public void populateItem(final Item<ICellPopulator<SkillRatingDO>> item, final String componentId,
          final IModel<SkillRatingDO> rowModel)
      {
        final SkillRatingDO skillRating = rowModel.getObject();
        appendCssClasses(item, skillRating.getId(), skillRating.isDeleted());
      }
    };

    final CellItemListenerPropertyColumn<SkillRatingDO> created = new CellItemListenerPropertyColumn<SkillRatingDO>(
        new Model<>(
            getString("created")),
        getSortable("created", sortable), "created", cellItemListener)
    {
      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public void populateItem(final Item item, final String componentId, final IModel rowModel)
      {
        final SkillRatingDO skillRating = (SkillRatingDO) rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, SkillRatingEditPage.class, skillRating.getId(),
            returnToPage,
            DateTimeFormatter.instance().getFormattedDateTime(skillRating.getCreated())));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }

    };

    final CellItemListenerPropertyColumn<SkillRatingDO> modified = new CellItemListenerPropertyColumn<>(
        getString("modified"),
        getSortable("lastUpdate", sortable), "lastUpdate", cellItemListener);

    final CellItemListenerPropertyColumn<SkillRatingDO> user = new UserPropertyColumn<>(
        getUserGroupCache(),
        SkillRatingDO.class, getSortable(
        "userId", sortable),
        "user", cellItemListener).withUserFormatter(userFormatter);

    // TODO: Workaround with get (hardcoded I18N), needs a better solution.
    // Commented lines don't work!
    // final CellItemListenerPropertyColumn<SkillRatingDO> skillTitle = new
    // CellItemListenerPropertyColumn<SkillRatingDO>(SkillRatingDO.class,
    // getSortable("skill.title", sortable), "skill.title", cellItemListener);
    final CellItemListenerPropertyColumn<SkillRatingDO> skillTitle = new CellItemListenerPropertyColumn<>(
        getString("plugins.skillmatrix.skillrating.skill"), getSortable("skill.title", sortable), "skill.title",
        cellItemListener);

    final CellItemListenerPropertyColumn<SkillRatingDO> experience = new CellItemListenerPropertyColumn<>(
        SkillRatingDO.class,
        getSortable("skillRating", sortable), "skillRating", cellItemListener);

    final CellItemListenerPropertyColumn<SkillRatingDO> sinceYear = new CellItemListenerPropertyColumn<>(
        SkillRatingDO.class,
        getSortable("sinceYear", sortable), "sinceYear", cellItemListener);

    final CellItemListenerPropertyColumn<SkillRatingDO> certificates = new CellItemListenerPropertyColumn<>(
        SkillRatingDO.class, getSortable("certificates", sortable), "certificates", cellItemListener);

    final CellItemListenerPropertyColumn<SkillRatingDO> trainingCourses = new CellItemListenerPropertyColumn<>(
        SkillRatingDO.class, getSortable("trainingCourses", sortable), "trainingCourses", cellItemListener);

    final CellItemListenerPropertyColumn<SkillRatingDO> description = new CellItemListenerPropertyColumn<>(
        SkillRatingDO.class, getSortable("description", sortable), "description", cellItemListener);

    final CellItemListenerPropertyColumn<SkillRatingDO> comment = new CellItemListenerPropertyColumn<>(
        SkillRatingDO.class,
        getSortable("comment", sortable), "comment", cellItemListener);

    columns.add(created);
    columns.add(modified);
    columns.add(user);
    columns.add(skillTitle);
    columns.add(experience);
    columns.add(sinceYear);
    columns.add(certificates);
    columns.add(trainingCourses);
    columns.add(description);
    columns.add(comment);

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
    addExcelExport(getString("plugins.skillmatrix.skillrating.menu"),
        getString("plugins.skillmatrix.skillrating.menu"));
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
       * @see ExcelExporter#addMapping(PropertyMapping, java.lang.Object,
       *      java.lang.reflect.Field)
       */
      @Override
      public void addMapping(final PropertyMapping mapping, final Object entry, final Field field)
      {
        if ("skill".equals(field.getName())) {
          final SkillDO skill = ((SkillRatingDO) entry).getSkill();
          mapping.add(field.getName(), skill != null ? skill.getTitle() : "");
        } else {
          super.addMapping(mapping, entry, field);
        }
      }
    };
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#getBaseDao()
   */
  @Override
  public SkillRatingDao getBaseDao()
  {
    return skillRatingDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#newListForm(org.projectforge.web.wicket.AbstractListPage)
   */
  @Override
  protected SkillRatingListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new SkillRatingListForm(this);
  }

  /*
   * @see org.projectforge.web.wicket.AbstractListPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("skillId".equals(property)) {
      form.getSearchFilter().setSkillId((Integer) selectedValue);
      refresh();
    } else if ("userId".equals(property)) {
      form.getSearchFilter().setUserId((Integer) selectedValue);
      refresh();
    } else
      super.select(property, selectedValue);
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property)
  {
    if ("skillId".equals(property)) {
      form.getSearchFilter().setSkillId(null);
      refresh();
    } else if ("userId".equals(property)) {
      form.getSearchFilter().setUserId(null);
      refresh();
    }
    super.unselect(property);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#redirectToEditPage(org.apache.wicket.request.mapper.parameter.PageParameters)
   */
  @Override
  protected AbstractEditPage<?, ?, ?> redirectToEditPage(final PageParameters params)
  {
    if (params == null && form.getSearchFilter().getSkillId() != null) {
      final PageParameters newParams = new PageParameters();
      newParams.set(SkillRatingEditForm.PARAM_SKILL_ID, form.getSearchFilter().getSkillId());
      return super.redirectToEditPage(newParams);
    } else {
      return super.redirectToEditPage(params);
    }
  }

}
