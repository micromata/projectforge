package org.projectforge.web.selenium.common;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.projectforge.web.selenium.ListPage;
import org.junit.jupiter.api.Assertions;

public class SeleniumCalendarListPage extends ListPage<SeleniumCalendarListPage, SeleniumCalendarEditPage>
{
  @Override
  public SeleniumCalendarEditPage getEditPage()
  {
    return new SeleniumCalendarEditPage();
  }

  SeleniumCalendarListPage clickExportHolidays()
  {
    driver.findElement(By.id("exportHolidays"));
    return this;
  }

  SeleniumCalendarListPage clickExportTimesheets()
  {
    driver.findElement(By.id("exportTimesheets"));
    return this;
  }

  SeleniumCalendarListPage clickExportWekksOfYear()
  {
    driver.findElement(By.id("exportWeekOfYears")).click();
    return this;
  }

  public SeleniumCalendarListPage setOptionPanel(boolean flag)
  {
    String input = "//div[@class='controls controls-row']/div/label";
    try {
      List<WebElement> elements = driver.findElements(By.xpath(input));
      for (WebElement e : elements) {
        if (e.getText() == "all")
          clickAndWaitForFullPageReload(elements.get(2));
      }
      if (flag) {
        elements = driver.findElements(By.xpath(input));
        for (WebElement e : elements) {
          if (e.getText() == "Full access" || e.getText() == "Read-only access" || e.getText() == "Minimal access")
            clickAndWaitForFullPageReload(e);
        }
      }
    } catch (Exception e) {
      Assertions.fail(e.getMessage());
    }
    return this;
  }

  @Override

  public String getUrlPostfix()
  {
    return "wa/wicket/bookmarkable/org.projectforge.web.teamcal.admin.TeamCalListPage?";
  }
}
