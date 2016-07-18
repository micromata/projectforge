package org.projectforge.web.selenium;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Created by mhesse on 16.11.15.
 */
public class SeleniumTestWatcher extends TestWatcher
{

  public final static Logger logger = Logger.getLogger(SeleniumTestWatcher.class);
  private WebDriver driver;

  @Override
  protected void failed(Throwable e, Description description)
  {
    File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
    try {
      DateFormat format = new SimpleDateFormat("yyyyMMdd_HH-mm-ss");
      FileUtils
          .copyFile(scrFile, new File("./screenshots/" + description.getClassName() + "." + description.getMethodName() + format.format(new Date()) + ".jpg"));
      logger.error("ERROR: Fehler unter URL: " + driver.getCurrentUrl(), e);
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    driver.quit();
  }

  @Override
  protected void succeeded(Description description)
  {
    super.succeeded(description);
    driver.quit();
  }

  public WebDriver getDriver()
  {
    return driver;
  }

  public void setDriver(WebDriver driver)
  {
    this.driver = driver;
  }
}
