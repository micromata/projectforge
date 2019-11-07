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
 * The controller of the list page. Most functionality such as search etc. is done by the super class.
 *
 * @author Werner Feder (werner.feder@t-online.de)
 */
@ListPage(editPage = TrainingAttendeeEditPage.class)
public class TrainingAttendeeListPage
    extends AbstractListPage<TrainingAttendeeListForm, TrainingAttendeeDao, TrainingAttendeeDO> implements
    IListPageColumnsCreator<TrainingAttendeeDO>
{

  private static final long serialVersionUID = 685671613717879800L;

  public static final String I18N_KEY_PREFIX = "plugins.skillmatrix.skilltraining.attendee";

  @SpringBean
  private TrainingAttendeeDao trainingAttendeeDao;

  @SpringBean
  private UserFormatter userFormatter;

  public static final String PARAM_TRAINING_ID = "trainingId";

  public TrainingAttendeeListPage(final PageParameters parameters)
  {
    super(parameters, I18N_KEY_PREFIX);
    final Integer trainingId = WicketUtils.getAsInteger(parameters, PARAM_TRAINING_ID);
    if (NumberHelper.greaterZero(trainingId)) {
      form.getSearchFilter().setTrainingId(trainingId);
    }
  }

  /**
   * @see org.projectforge.web.wicket.IListPageColumnsCreator#createColumns(org.apache.wicket.markup.html.WebPage,
   * boolean)
   */
  @SuppressWarnings("serial")
  @Override
  public List<IColumn<TrainingAttendeeDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<TrainingAttendeeDO, String>> columns = new ArrayList<>();
    final CellItemListener<TrainingAttendeeDO> cellItemListener = new CellItemListener<TrainingAttendeeDO>()
    {
      public void populateItem(final Item<ICellPopulator<TrainingAttendeeDO>> item, final String componentId,
          final IModel<TrainingAttendeeDO> rowModel)
      {
        final TrainingAttendeeDO attendeeDO = rowModel.getObject();
        appendCssClasses(item, attendeeDO.getId(), attendeeDO.isDeleted());
      }
    };

    columns
        .add(new CellItemListenerPropertyColumn<>(getString("plugins.skillmatrix.skillrating.skill"),
            getSortable("training.skill.title", sortable), "training.skill.title", cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<>(getString("plugins.skillmatrix.skilltraining.training"),
            getSortable("training.title", sortable), "training.title", cellItemListener));

    columns.add(
        new CellItemListenerPropertyColumn<>(getString("plugins.skillmatrix.skilltraining.startDate"),
            getSortable("training.startDate", sortable), "training.startDate",
            cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<>(getString("plugins.skillmatrix.skilltraining.endDate"),
            getSortable("training.endDate", sortable), "training.endDate",
            cellItemListener));

    columns.add(new UserPropertyColumn<>(getUserGroupCache(), TrainingAttendeeDO.class,
        getSortable("attendeeId", sortable), "attendee",
        cellItemListener).withUserFormatter(userFormatter));
    columns.add(new CellItemListenerPropertyColumn<>(TrainingAttendeeDO.class,
        getSortable("rating", sortable), "rating",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(TrainingAttendeeDO.class,
        getSortable("certificate", sortable), "certificate",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(TrainingAttendeeDO.class,
        getSortable("description", sortable), "description",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<TrainingAttendeeDO>(TrainingAttendeeDO.class,
        getSortable("created", sortable), "created",
        cellItemListener)
    {
      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public void populateItem(final Item item, final String componentId, final IModel rowModel)
      {
        final TrainingAttendeeDO attendeeDO = (TrainingAttendeeDO) rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, TrainingAttendeeEditPage.class, attendeeDO.getId(),
            returnToPage,
            DateTimeFormatter.instance().getFormattedDateTime(attendeeDO.getCreated())));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });

    columns.add(new CellItemListenerPropertyColumn<>(TrainingAttendeeDO.class,
        getSortable("lastUpdate", sortable), "lastUpdate",
        cellItemListener));

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
    addExcelExport(getString("plugins.skillmatrix.skilltraining.attendee.menu"),
        getString("plugins.skillmatrix.skilltraining.attendee.menu"));
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
        if ("training".equals(field.getName())) {
          final SkillDO skill = ((TrainingAttendeeDO) entry).getTraining().getSkill();
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
  public TrainingAttendeeDao getBaseDao()
  {
    return trainingAttendeeDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("attendeeId".equals(property)) {
      form.getSearchFilter().setAttendeeId((Integer) selectedValue);
      refresh();
    } else if ("trainingId".equals(property)) {
      form.getSearchFilter().setTrainingId((Integer) selectedValue);
      refresh();
    } else {
      super.select(property, selectedValue);
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property)
  {
    if ("attendeeId".equals(property)) {
      form.getSearchFilter().setAttendeeId(null);
      refresh();
    } else if ("trainingId".equals(property)) {
      form.getSearchFilter().setTrainingId(null);
      refresh();
    } else {
      super.unselect(property);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#newListForm(org.projectforge.web.wicket.AbstractListPage)
   */
  @Override
  protected TrainingAttendeeListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new TrainingAttendeeListForm(this);
  }

}
