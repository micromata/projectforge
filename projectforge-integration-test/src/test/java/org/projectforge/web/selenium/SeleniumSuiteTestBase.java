package org.projectforge.web.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.projectforge.web.selenium.common.SetupPage;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;

import java.util.concurrent.TimeUnit;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;

public class SeleniumSuiteTestBase
{

  protected static WebDriver driver;

  protected static WebDriverWait wait;

  // waitin and timeout periods  all noted in seconds
  private static final int WAITINGPERIODSERVERSTARTUP = (5 * 60);

  private static final int WAITSLOTSERVERSTARTUP= 20;

  private static final int PAGELOADTIMEOUT = 30;

  public SeleniumSuiteTestBase()
  {
    if (driver == null) {
      DesiredCapabilities dCaps = new DesiredCapabilities();
      dCaps.setJavascriptEnabled(true);
      /*dCaps.setCapability(
          PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--web-security=no", "--ignore-ssl-errors=yes"});*/
      dCaps.setJavascriptEnabled(true);
      //driver = new PhantomJSDriver(dCaps);
      driver = new JBrowserDriver();
      wait = new WebDriverWait(driver, PAGELOADTIMEOUT);
      driver.manage().timeouts().pageLoadTimeout(PAGELOADTIMEOUT, TimeUnit.SECONDS);
      TestPageBase.setDriver(driver);
      TestPageBase.setWait(wait);
      TestPageBase.setBaseUrl(Const.PF_URL);

      // test server availability
        int time = 0;
        boolean available = false;
        while (true && time < WAITINGPERIODSERVERSTARTUP && available == false) {
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
          try {
            Thread.sleep(1000 * WAITINGPERIODSERVERSTARTUP);
          } catch (InterruptedException ignored) {
          }

          time += WAITSLOTSERVERSTARTUP;
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
