package org.projectforge.plugins.eed.wicket;

import org.apache.log4j.Logger;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.plugins.eed.EmployeeGeneralValueDO;
import org.projectforge.plugins.eed.EmployeeGeneralValueDao;
import org.projectforge.plugins.eed.service.EmployeeGeneralValueService;
import org.projectforge.web.wicket.*;

import java.util.ArrayList;
import java.util.List;

public class EmployeeGeneralValueConfigurationPage extends AbstractEditPage<EmployeeGeneralValueDO,EmployeeGeneralValueConfigurationForm,EmployeeGeneralValueDao>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeGeneralValueConfigurationPage.class);

  @SpringBean
  private EmployeeGeneralValueDao employeeGeneralValueDao;

  @SpringBean
  private EmployeeGeneralValueService employeeGeneralValueService;

  private List<EmployeeGeneralValueDO> dataList;


  public EmployeeGeneralValueConfigurationPage(PageParameters parameters)
  {
    super(parameters, "plugins.eed.config");
    EmployeeGeneralValueDO valueDO = employeeGeneralValueService.getValueDO();
    init(valueDO);
  }

  @Override
  protected EmployeeGeneralValueConfigurationForm newEditForm(AbstractEditPage<?, ?, ?> parentPage, EmployeeGeneralValueDO data) {
    return new EmployeeGeneralValueConfigurationForm(this, data);
  }

  @Override
  protected Logger getLogger() {
    return log;
  }

  @Override
  public EmployeeGeneralValueDao getBaseDao()
  {
    return employeeGeneralValueDao;
  }
}