package org.projectforge.web.selenium.administration;

import org.projectforge.web.selenium.ListPage;

public class SeleniumGroupListPage extends ListPage<SeleniumGroupListPage, SeleniumGroupEditPage>
{
  @Override
  public SeleniumGroupEditPage getEditPage()
  {
    return new SeleniumGroupEditPage();
  }

  @Override
  public String getUrlPostfix()
  {
    return "wa/groupList?";
  }
}
