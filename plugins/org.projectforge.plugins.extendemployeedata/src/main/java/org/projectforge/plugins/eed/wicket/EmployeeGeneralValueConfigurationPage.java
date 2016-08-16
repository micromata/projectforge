package org.projectforge.plugins.eed.wicket;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.plugins.eed.EmployeeGeneralValueDO;
import org.projectforge.plugins.eed.EmployeeGeneralValueDao;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;

import java.util.ArrayList;
import java.util.List;

@ListPage(editPage = EmployeeGeneralValueEditPage.class)
public class EmployeeGeneralValueConfigurationPage extends AbstractListPage<EmployeeGeneralValueConfigurationForm, EmployeeGeneralValueDao,EmployeeGeneralValueDO> implements
    IListPageColumnsCreator<EmployeeGeneralValueDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeGeneralValueConfigurationPage.class);

  @SpringBean
  private EmployeeGeneralValueDao employeeGeneralValueDao;

  private List<EmployeeGeneralValueDO> dataList;


  public EmployeeGeneralValueConfigurationPage(PageParameters parameters)
  {
    super(parameters, "plugins.eed.config");
  }

  @Override
  public List<IColumn<EmployeeGeneralValueDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<EmployeeGeneralValueDO, String>> columns = new ArrayList<>();

    final CellItemListener<EmployeeGeneralValueDO> cellItemListener = (CellItemListener<EmployeeGeneralValueDO>) (item, componentId,
        rowModel) -> {
      final EmployeeGeneralValueDO configValue = rowModel.getObject();
      appendCssClasses(item, configValue.getId(), configValue.isDeleted());
    };

    columns.add(new CellItemListenerPropertyColumn<>(new ResourceModel("key"),
        getSortable("key", sortable),
        "key", cellItemListener));

    /*columns.add(new CellItemListenerPropertyColumn<>(new ResourceModel("value"),
        getSortable("value", sortable),
        "value", cellItemListener));*/

    columns.add(new StringValueColumn(new ResourceModel("value"),"value"));

    return columns;
  }

  @Override
  protected void init()
  {
    final List<IColumn<EmployeeGeneralValueDO, String>> columns = createColumns(this, true);
    dataTable = createDataTable(columns, "key", SortOrder.ASCENDING);
    form.add(dataTable);

    // remove add and reindex buttons from context menu
    //contentMenuBarPanel = new MenuBarPanel("menuBar");

  }

  @Override
  public EmployeeGeneralValueDao getBaseDao()
  {
    return employeeGeneralValueDao;
  }

  @Override
  protected void addBottomPanel(final String id)
  {
    form.add(form.getSaveButtonPanel(id));
  }

  @Override
  protected EmployeeGeneralValueConfigurationForm newListForm(AbstractListPage<?, ?, ?> parentPage)
  {
    return new EmployeeGeneralValueConfigurationForm(this);
  }

  public void saveList()
  {
    for (EmployeeGeneralValueDO e : this.dataList) {
      employeeGeneralValueDao.update(e);
    }
    info(I18nHelper.getLocalizedString("plugins.eed.listcare.savesucces"));
  }

  @Override
  public List<EmployeeGeneralValueDO> getList()
  {
    this.dataList = super.getList();
    return this.dataList;
  }
}
