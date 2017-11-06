package org.projectforge.web.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.WebElement;

public abstract class ListPage<O extends TestPageBase, EditPage extends TestPageBase> extends TestPageBase<O>
{

  /**
   * clicks the "+" symbol at the top of the site
   *
   * @return
   */
  public EditPage addEntry()
  {
    WebElement element = driver.findElement(By.xpath("//li[@id='addEntry']/a/i[contains(@data-original-title,'N')]"));
    clickAndWaitForFullPageReload(element);

    return getEditPage();
  }

  /**
   * @return a new instance of the used EditPage for this ListPage
   */
  public abstract EditPage getEditPage();

  public EditPage clickRowWhereColumnLike(String pattern) throws InvalidSelectorException
  {
    WebElement element = driver
        .findElement(By.xpath("//*[@class='dataview']/tbody/tr/td[contains(.,'" + pattern + "')]"));
    clickAndWaitForFullPageReload(element);
    return getEditPage();
  }
}
