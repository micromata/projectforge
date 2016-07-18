package org.projectforge.web.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;

import java.util.concurrent.TimeUnit;

public class SeleniumSuiteTestBase
{

  protected static WebDriver driver;

  protected static WebDriverWait wait;

  public SeleniumSuiteTestBase()
  {
    if (driver == null) {
      DesiredCapabilities dCaps = new DesiredCapabilities();
      dCaps.setJavascriptEnabled(true);
      dCaps.setCapability(
          PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--web-security=no", "--ignore-ssl-errors=yes"});
      dCaps.setJavascriptEnabled(true);
      driver = new PhantomJSDriver(dCaps);
      wait = new WebDriverWait(driver, 10);
      driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS);
      TestPageBase.setDriver(driver);
      TestPageBase.setWait(wait);
      TestPageBase.setBaseUrl(Const.PF_URL);

      // test server availability

      int time = 0;
      boolean available = false;
      while (time < (5 * 60) && available == false) {
        try {
          driver.get(Const.PF_URL);
          driver.findElement(By.id("username"));
          available = true;
        } catch (Exception ignored) {
        }
        try {
          Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }

        time += 5;
      }
    }
  }

  /**
   * Ensure that we have a clean browser and we are logged out before each test.
   */
  @BeforeMethod
  public void clearCookies()
  {
    driver.manage().deleteAllCookies();
  }

  @AfterSuite
  public void tearDown()
  {
    driver.quit();
  }

}
