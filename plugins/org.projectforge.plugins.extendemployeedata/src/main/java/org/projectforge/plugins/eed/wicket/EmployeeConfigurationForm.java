package org.projectforge.plugins.eed.wicket;

import org.apache.log4j.Logger;
import org.projectforge.plugins.eed.EmployeeConfigurationDO;
import org.projectforge.web.wicket.AbstractEditForm;

public class EmployeeConfigurationForm
    extends AbstractEditForm<EmployeeConfigurationDO, EmployeeConfigurationPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(EmployeeConfigurationForm.class);

  public EmployeeConfigurationForm(EmployeeConfigurationPage parentPage,
      EmployeeConfigurationDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}