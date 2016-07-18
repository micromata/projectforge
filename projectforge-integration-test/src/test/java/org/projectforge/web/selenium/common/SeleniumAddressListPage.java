package org.projectforge.web.selenium.common;

import org.projectforge.web.selenium.ListPage;

public class SeleniumAddressListPage extends ListPage<SeleniumAddressListPage, SeleniumAddressEditPage>
{
  @Override
  public SeleniumAddressEditPage getEditPage()
  {
    return new SeleniumAddressEditPage();
  }

  @Override
  public String getUrlPostfix()
  {
    return "wa/addressList?";
  }
}
