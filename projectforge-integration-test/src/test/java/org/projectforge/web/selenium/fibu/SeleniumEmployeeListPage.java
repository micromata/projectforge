package org.projectforge.web.selenium.fibu;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.projectforge.web.selenium.ListPage;
import org.junit.jupiter.api.Assertions;

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

  public SeleniumEmployeeListPage setOptionPanel(boolean active, boolean delete)
  {
    String input = "//div[@class='controls']/div/label";
    try {
      List<WebElement> elements = driver.findElements(By.xpath(input));
      if (active)
        clickAndWaitForFullPageReload(elements.get(0));
      if (delete)
        clickAndWaitForFullPageReload(elements.get(1));
    } catch (Exception e) {
      Assertions.fail(e.getMessage());
    }
    return this;
  }
}
