package org.projectforge.web.selenium;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.projectforge.web.selenium.common.SetupPage;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;

public class SeleniumSuiteTestBase
{

  protected static WebDriver driver;

  protected static FluentWait<WebDriver> wait;

  // waitin and timeout periods  all noted in seconds
  private static final int WAITINGPERIODSERVERSTARTUP = (5 * 60);

  private static final int WAITSLOTSERVERSTARTUP = 20;

  private static final int PAGELOADTIMEOUT = 30;

  public SeleniumSuiteTestBase()
  {
    if (driver == null) {
      //System.setProperty("webdriver.chrome.driver", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
      driver = new JBrowserDriver();
      wait = new FluentWait<WebDriver>(driver)
          .withTimeout(PAGELOADTIMEOUT, TimeUnit.SECONDS)
          .pollingEvery(500, TimeUnit.MILLISECONDS);
      driver.manage().timeouts().pageLoadTimeout(PAGELOADTIMEOUT, TimeUnit.SECONDS);
      TestPageBase.setDriver(driver);
      TestPageBase.setWait(wait);
      TestPageBase.setBaseUrl(Const.PF_URL);

      // test server availability
      int time = 0;
      boolean available = false;
      while (time < WAITINGPERIODSERVERSTARTUP && available == false) {
        try {
          driver.get(Const.PF_URL);
          if (driver.getCurrentUrl().contains("setup")) {
            throw null;
          }
          driver.findElement(By.id("username"));
          available = true;
        } catch (Exception e) {
          // check if setup page is available
          try {
            SetupPage setupPage = new SetupPage();
            setupPage.callPage()
                .setUsername(Const.ADMIN_USERNAME)
                .setPassword(Const.ADMIN_PASSWORD)
                .setRepeatPassword(Const.ADMIN_PASSWORD)
                .setCalendarDomain("de")
                .clickFinish()
                .logout();
          } catch (Exception ignored) {
            ignored.printStackTrace();
            // if not continue waiting
          }
        }
        System.out.print("Before-Time : " + time + " ### ");
        System.out.print(new Date(System.currentTimeMillis()));

        try {
          Thread.sleep(1000 * WAITSLOTSERVERSTARTUP);
        } catch (InterruptedException ignored) {
        }

        time += WAITSLOTSERVERSTARTUP;
        System.out.print("After-Time : " + time + " ### ");
        System.out.print(new Date(System.currentTimeMillis()));
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
