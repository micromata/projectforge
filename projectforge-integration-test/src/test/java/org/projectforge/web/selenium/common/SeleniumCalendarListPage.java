package org.projectforge.web.selenium.common;

import org.openqa.selenium.By;
import org.projectforge.web.selenium.ListPage;

public class SeleniumCalendarListPage extends ListPage<SeleniumCalendarListPage, SeleniumCalendarEditPage>
{
  @Override
  public SeleniumCalendarEditPage getEditPage()
  {
    return new SeleniumCalendarEditPage();
  }

  SeleniumCalendarListPage clickExportHolidays() {
    driver.findElement(By.id("exportHolidays"));
    return this;
  }

  SeleniumCalendarListPage clickExportTimesheets() {
    driver.findElement(By.id("exportTimesheets"));
    return this;
  }

  SeleniumCalendarListPage clickExportWekksOfYear() {
    driver.findElement(By.id("exportWeekOfYears")).click();
    return this;
  }


  @Override
  public String getUrlPostfix()
  {
    return "wa/wicket/bookmarkable/org.projectforge.web.teamcal.admin.TeamCalListPage?";
  }
}
