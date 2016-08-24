package org.projectforge.plugins.eed.wicket;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.plugins.eed.EmployeeConfigurationDO;
import org.projectforge.plugins.eed.EmployeeConfigurationDao;
import org.projectforge.plugins.eed.service.EmployeeConfigurationService;
import org.projectforge.web.wicket.AbstractEditPage;

public class EmployeeConfigurationPage
    extends AbstractEditPage<EmployeeConfigurationDO, EmployeeConfigurationForm, EmployeeConfigurationDao>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeConfigurationPage.class);

  @SpringBean
  private EmployeeConfigurationService employeeConfigurationService;

  private List<EmployeeConfigurationDO> dataList;

  public EmployeeConfigurationPage(PageParameters parameters)
  {
    super(parameters, "plugins.eed.config");

    final EmployeeConfigurationDO valueDO = employeeConfigurationService.getTheDO();
    init(valueDO);
  }

  @Override
  protected EmployeeConfigurationForm newEditForm(AbstractEditPage<?, ?, ?> parentPage, EmployeeConfigurationDO data)
  {
    return new EmployeeConfigurationForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  public EmployeeConfigurationDao getBaseDao()
  {
    return employeeConfigurationService.getDao();
  }
}
