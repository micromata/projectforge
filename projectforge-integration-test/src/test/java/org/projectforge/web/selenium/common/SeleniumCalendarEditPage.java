package org.projectforge.web.selenium.common;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.projectforge.web.selenium.EditPage;

public class SeleniumCalendarEditPage extends EditPage<SeleniumCalendarEditPage>
{

  public SeleniumCalendarEditPage() {
    super();
  }

  public SeleniumCalendarEditPage(int id) {
    super(id);
  }


  public SeleniumCalendarEditPage setTitle(String title) {
    return setStringElementById("title", title);
  }

  public String getTitle() {
    return getStringElementById("title");
  }

  public SeleniumCalendarEditPage setDescription(String description) {
    return setStringElementById("description", description);
  }

  public String getDescription() {
    return getStringElementById("description");
  }

  public SeleniumCalendarEditPage setOwner(String owner) {
    return setStringElementById("owner", owner);
  }

  public String getOwner() {
    return getStringElementById("owner");
  }

  public SeleniumCalendarEditPage checkExternalSubscription() {
    WebElement externalSubscription = driver.findElement(By.id("externalSubscription"));
    if(externalSubscription.isSelected() == false) {
      externalSubscription.sendKeys(Keys.SPACE);
    }
    return this;
  }

  public SeleniumCalendarEditPage uncheckExternalSubscription() {
    WebElement externalSubscription = driver.findElement(By.id("externalSubscription"));
    if(externalSubscription.isSelected() == true) {
      externalSubscription.sendKeys(Keys.SPACE);
    }
    return this;
  }

  public SeleniumCalendarEditPage setFullAccessGroups(String... groupNames) {
    return chooseOptionsOfSelect2("fullAccessGroups", groupNames);
  }

  public SeleniumCalendarEditPage setMinimalAccessGroups(String... groupNames) {
    return chooseOptionsOfSelect2("minimalAccessGroups", groupNames);
  }

  public SeleniumCalendarEditPage setReadOnlyAccessGroups(String... groupNames) {
    return chooseOptionsOfSelect2("readOnlyAccessGroups", groupNames);
  }

  public SeleniumCalendarEditPage setMinimalAccessUsers(String... users) {
    return chooseOptionsOfSelect2("minimalAccessUsers", users);
  }

  public SeleniumCalendarEditPage setReadOnlyAccessUsers(String... users) {
    return chooseOptionsOfSelect2("readOnlyAccessUsers", users);
  }

  public SeleniumCalendarEditPage setFullAccessUsers(String... users) {
    return chooseOptionsOfSelect2("fullAccessUsers", users);
  }

  /**
   * The calendar create/edit page is not directly callable
   * @returns null
   */
  @Override
  protected String urlPostfix()
  {
    return null;
  }
}
