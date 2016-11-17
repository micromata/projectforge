package org.projectforge.web.orga;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.orga.VisitorbookDO;
import org.projectforge.business.orga.VisitorbookService;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;

/**
 * Created by blumenstein on 17.11.16.
 */
@ListPage(editPage = VisitorbookEditPage.class)
public class VisitorbookListPage extends AbstractListPage<VisitorbookListForm, VisitorbookService, VisitorbookDO> implements
    IListPageColumnsCreator<VisitorbookDO>
{
  private static final Logger log = Logger.getLogger(VisitorbookListPage.class);

  private static final long serialVersionUID = -8406451234003792763L;

  @SpringBean
  private VisitorbookService visitorbookService;

  @SpringBean
  private GuiAttrSchemaService guiAttrSchemaService;

  public VisitorbookListPage(final PageParameters parameters)
  {
    super(parameters, "orga.visitorbook");
  }

  public VisitorbookListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "orga.visitorbook");
  }

  @Override
  @SuppressWarnings("serial")
  public List<IColumn<VisitorbookDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<VisitorbookDO, String>> columns = new ArrayList<>();

    final CellItemListener<VisitorbookDO> cellItemListener = new CellItemListener<VisitorbookDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<VisitorbookDO>> item, final String componentId,
          final IModel<VisitorbookDO> rowModel)
      {
        final VisitorbookDO visitorbook = rowModel.getObject();
        appendCssClasses(item, visitorbook.getId(), visitorbook.isDeleted());
      }
    };

    columns.add(new CellItemListenerPropertyColumn<VisitorbookDO>(new ResourceModel("orga.visitorbook.lastname"),
        getSortable("lastname", sortable),
        "lastname", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<VisitorbookDO>> item, final String componentId,
          final IModel<VisitorbookDO> rowModel)
      {
        final VisitorbookDO visitor = rowModel.getObject();
        if (isSelectMode() == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, VisitorbookEditPage.class, visitor.getId(),
              returnToPage, visitor.getLastname()));
        } else {
          item.add(
              new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, visitor.getId(), visitor.getLastname()));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<VisitorbookDO>(new ResourceModel("orga.visitorbook.firstname"),
        getSortable("firstname", sortable),
        "firstname", cellItemListener));

    columns.add(new CellItemListenerPropertyColumn<VisitorbookDO>(new ResourceModel("orga.visitorbook.company"),
        getSortable("company", sortable),
        "company", cellItemListener));

    columns.add(new CellItemListenerPropertyColumn<VisitorbookDO>(new ResourceModel("orga.visitorbook.visitortype"),
        getSortable("visitortype", sortable),
        "visitortype", cellItemListener));

    return columns;
  }

  @Override
  protected void init()
  {
    final List<IColumn<VisitorbookDO, String>> columns = createColumns(this, true);
    dataTable = createDataTable(columns, "orga.visitorbook.lastname", SortOrder.ASCENDING);
    form.add(dataTable);
    addExcelExport(getString("orga.visitorbook.title.heading"), "visitors");
  }

  @Override
  protected VisitorbookListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new VisitorbookListForm(this);
  }

  @Override
  public VisitorbookService getBaseDao()
  {
    return visitorbookService;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if (property.startsWith("quickSelect.") == true) { // month".equals(property) == true) {
      final Date date = (Date) selectedValue;
      form.getSearchFilter().setStartTime(date);
      final DateHolder dateHolder = new DateHolder(date);
      if (property.endsWith(".month") == true) {
        dateHolder.setEndOfMonth();
      } else if (property.endsWith(".week") == true) {
        dateHolder.setEndOfWeek();
      } else {
        log.error("Property '" + property + "' not supported for selection.");
      }
      form.getSearchFilter().setStopTime(dateHolder.getDate());
      form.startDate.markModelAsChanged();
      form.stopDate.markModelAsChanged();
      refresh();
    } else {
      super.select(property, selectedValue);
    }
  }

}

