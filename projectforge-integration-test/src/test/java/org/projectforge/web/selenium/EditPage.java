package org.projectforge.web.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

public abstract class EditPage<O extends TestPageBase> extends TestPageBase<O>
{
  private int id;

  public O clickmarkAsDeleted() {
    WebElement markAsDeleted = driver.findElement(By.id("markAsDeleted"));
    ((JavascriptExecutor)driver).executeScript("showDeleteQuestionDialog = function(){return true;}");
    markAsDeleted.click();
    waitForPageReload(markAsDeleted);
    return (O) this;
  }

  public O clickCreateOrUpdate()
  {
    if (id > 0) {
      clickAndWaitForFullPageReload("update");
    } else {
      clickAndWaitForFullPageReload("create");
    }
    return (O) this;
  }

  public EditPage() {
    id = -666;
  }

  public EditPage(int id) {
    this.id = id;
  }

  protected abstract String urlPostfix();

  @Override
  public final String getUrlPostfix()
  {
    String postfix = id > 0 ? "&id=" + id : "";
    String urlPostfix = urlPostfix();
    return urlPostfix + postfix;
  }

  public O setId(int i)
  {
    id = i;
    return (O) this;
  }
}
