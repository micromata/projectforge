package org.projectforge.web.selenium.fibu;

import org.projectforge.web.selenium.ListPage;

public class SeleniumEmployeeListPage extends ListPage<SeleniumEmployeeListPage, SeleniumEmployeeEditPage>
{

  @Override
  public String getUrlPostfix()
  {
    return "wa/employeeList?";
  }

  @Override
  public SeleniumEmployeeEditPage getEditPage()
  {
    return new SeleniumEmployeeEditPage();
  }
}
