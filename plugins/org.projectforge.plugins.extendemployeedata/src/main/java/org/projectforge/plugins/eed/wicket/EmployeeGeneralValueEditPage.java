package org.projectforge.plugins.eed.wicket;

import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.plugins.eed.EmployeeGeneralValueDO;
import org.projectforge.plugins.eed.EmployeeGeneralValueDao;
import org.projectforge.web.wicket.AbstractEditPage;

public class EmployeeGeneralValueEditPage extends AbstractEditPage<EmployeeGeneralValueDO, EmployeeGeneralValueEditForm, EmployeeGeneralValueDao>
{

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeGeneralValueEditPage.class);

  @SpringBean
  private EmployeeGeneralValueDao employeeGeneralValueDao;

  public EmployeeGeneralValueEditPage(PageParameters pageParameters)
  {
    this(pageParameters, "plugins.eed.config");
  }

  public EmployeeGeneralValueEditPage(PageParameters parameters,
      String i18nPrefix)
  {
    super(parameters, i18nPrefix);
    init();
  }

  @Override
  protected EmployeeGeneralValueDao getBaseDao()
  {
    return employeeGeneralValueDao;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected EmployeeGeneralValueEditForm newEditForm(AbstractEditPage<?, ?, ?> parentPage, EmployeeGeneralValueDO data)
  {
    return new EmployeeGeneralValueEditForm(this, data);
  }
}
