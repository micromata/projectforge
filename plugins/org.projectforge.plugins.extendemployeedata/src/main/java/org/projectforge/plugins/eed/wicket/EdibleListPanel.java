package org.projectforge.plugins.eed.wicket;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPage;
import org.projectforge.web.wicket.MyListPageSortableDataProvider;
import org.projectforge.web.wicket.RowCssClass;
import org.projectforge.web.wicket.WicketUtils;

public class EdibleListPanel extends Panel
{
  private static final long serialVersionUID = -1002345493490211625L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EdibleListPanel.class);

  private DataTable<EmployeeDO, String> dataTable;

  private MyListPageSortableDataProvider<EmployeeDO> listPageSortableDataProvider;

  private Serializable highlightedRowId;

  private IListPage<?, ?> parentPage;

  private static final List<Integer> MONTH_INTEGERS = Arrays
      .asList(new Integer[] { new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5),
          new Integer(6), new Integer(7), new Integer(8), new Integer(9), new Integer(10), new Integer(11),
          new Integer(12) });

  public EdibleListPanel(String id, IListPage parentPage, List<EmployeeDO> employeeList)
  {
    super(id);
    this.parentPage = parentPage;
    //Filter
    DropDownChoice<Integer> ddcMonth = new DropDownChoice<Integer>("monthDropDownChoise", MONTH_INTEGERS);
    add(ddcMonth);
    DropDownChoice<Integer> ddcYear = new DropDownChoice<Integer>("yearDropDownChoise", getDropDownYears());
    add(ddcYear);
    DropDownChoice<String> ddcOption = new DropDownChoice<String>("typeDropDownChoise", getDropDownOptions());
    add(ddcOption);

    //TopButtons
    add(new Button("displayButton"));
    add(new Button("exportButton"));
    add(new Button("importButton"));

    //List
    List<IColumn<EmployeeDO, String>> columns = createColumns(true);
    dataTable = createDataTable(columns, "user.lastname", SortOrder.ASCENDING);
    add(dataTable);
  }

  private List<String> getDropDownOptions()
  {
    return Arrays
        .asList(new String[] { "[TODO] Abzug Mobilfunk", "[TODO] Abzug Mobilgerät", "[TODO] Abzug Reisekosten",
            "[TODO] Auslagen", "[TODO] Überstunden", "[TODO] Prämie/Bonus", "[TODO] Sonderzahlungen",
            "[TODO] Zielvereinbarungen", "[TODO] Abzug Shop", "[TODO] Wochenendarbeit", "[TODO] Bemerkung/Sonstiges" });
  }

  private List<? extends Integer> getDropDownYears()
  {
    return Arrays
        .asList(new Integer[] { new Integer(2016) });
  }

  private DataTable<EmployeeDO, String> createDataTable(final List<IColumn<EmployeeDO, String>> columns,
      final String sortProperty,
      final SortOrder sortOrder)
  {
    final int pageSize = 100;
    final SortParam<String> sortParam = sortProperty != null
        ? new SortParam<String>(sortProperty, sortOrder == SortOrder.ASCENDING) : null;
    return new DefaultDataTable<EmployeeDO, String>("table", columns, createSortableDataProvider(sortParam), pageSize);
    // return new AjaxFallbackDefaultDataTable<O>("table", columns, createSortableDataProvider(sortProperty, ascending), pageSize);
  }

  private List<IColumn<EmployeeDO, String>> createColumns(final boolean sortable)
  {
    final List<IColumn<EmployeeDO, String>> columns = new ArrayList<IColumn<EmployeeDO, String>>();

    final CellItemListener<EmployeeDO> cellItemListener = new CellItemListener<EmployeeDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<EmployeeDO>> item, final String componentId,
          final IModel<EmployeeDO> rowModel)
      {
        final EmployeeDO employee = rowModel.getObject();
        appendCssClasses(item, employee.getId(), employee.isDeleted());
      }
    };

    columns.add(new CellItemListenerPropertyColumn<EmployeeDO>(new ResourceModel("name"),
        getSortable("user.lastname", sortable),
        "user.lastname", cellItemListener));

    columns.add(new CellItemListenerPropertyColumn<EmployeeDO>(new ResourceModel("firstName"),
        getSortable("user.firstname", sortable),
        "user.firstname", cellItemListener));

    return columns;
  }

  /**
   * At default a new SortableDOProvider is returned. Overload this method e. g. for avoiding
   * LazyInitializationExceptions due to sorting.
   * 
   * @param sortProperty
   * @param ascending
   */
  private ISortableDataProvider<EmployeeDO, String> createSortableDataProvider(final SortParam<String> sortParam)
  {
    return createSortableDataProvider(sortParam, null);
  }

  /**
   * At default a new SortableDOProvider is returned. Overload this method e. g. for avoiding
   * LazyInitializationExceptions due to sorting.
   * 
   * @param sortProperty
   * @param ascending
   */
  private ISortableDataProvider<EmployeeDO, String> createSortableDataProvider(final SortParam<String> sortParam,
      final SortParam<String> secondSortParam)
  {
    if (listPageSortableDataProvider == null) {
      listPageSortableDataProvider = new MyListPageSortableDataProvider<EmployeeDO>(sortParam, secondSortParam,
          parentPage);
    }
    return listPageSortableDataProvider;
  }

  /**
   * @param item The item where to add the css classes.
   * @param rowDataId If the current row data is equals to the hightlightedRow then the style will contain highlighting.
   * @param highlightedRowId The current row to highlight (id of the data object behind the row).
   * @param isDeleted Is this entry deleted? Then the deleted style will be added.
   * @return
   */
  private void appendCssClasses(final Item<?> item, final Serializable rowDataId,
      final Serializable highlightedRowId,
      final boolean isDeleted)
  {
    if (rowDataId == null) {
      return;
    }
    if (rowDataId instanceof Integer == false) {
      log.warn("Error in calling getCssStyle: Integer expected instead of " + rowDataId.getClass());
    }
    if (highlightedRowId != null && rowDataId != null && ObjectUtils.equals(highlightedRowId, rowDataId) == true) {
      appendCssClasses(item, RowCssClass.HIGHLIGHTED);
    }
    if (isDeleted == true) {
      appendCssClasses(item, RowCssClass.MARKED_AS_DELETED);
    }
  }

  /**
   * Adds some standard css classes such as {@link RowCssClass#MARKED_AS_DELETED} for deleted entries.
   * 
   * @param item The item where to add the css classes.
   * @param rowDataId If the current row data is equals to the hightlightedRow then the style will contain highlighting.
   * @param isDeleted Is this entry deleted? Then the deleted style will be added.
   * @return
   */
  private void appendCssClasses(final Item<?> item, final Serializable rowDataId, final boolean isDeleted)
  {
    appendCssClasses(item, rowDataId, this.highlightedRowId, isDeleted);
  }

  /**
   * @param item The item where to add the css classes.
   * @param rowCssClasses The css class to append to the given item.
   * @return
   */
  private void appendCssClasses(final Item<?> item, final RowCssClass... rowCssClasses)
  {
    WicketUtils.append(item, rowCssClasses);
  }

  /**
   * Tiny helper method.
   * 
   * @param propertyName
   * @param sortable
   * @return return sortable ? propertyName : null;
   */
  private String getSortable(final String propertyName, final boolean sortable)
  {
    return sortable ? propertyName : null;
  }

}
