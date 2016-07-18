package org.projectforge.web.selenium.administration;

import org.projectforge.web.selenium.ListPage;

public class SeleniumUserListPage extends ListPage<SeleniumUserListPage,SeleniumUserEditPage>
{

  @Override
  public String getUrlPostfix()
  {
    return "wa/userList?";
  }

  @Override
  public SeleniumUserEditPage getEditPage()
  {
    return new SeleniumUserEditPage();
  }
}
