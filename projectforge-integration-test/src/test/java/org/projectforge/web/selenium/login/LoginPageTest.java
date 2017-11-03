package org.projectforge.web.selenium.login;

import org.projectforge.web.selenium.SeleniumSuiteTestBase;
import org.projectforge.web.selenium.teamcal.SeleniumTeamCalPage;
import org.testng.annotations.Test;

public class LoginPageTest extends SeleniumSuiteTestBase
{

  /**
   * This test runs a successful login for the standard admin
   */
  @Test
  public void testSuccessfulLogin() throws InterruptedException
  {
    SeleniumLoginPage seleniumLoginPage = new SeleniumLoginPage();
    seleniumLoginPage.callPage();

    Thread.sleep(1000);

    seleniumLoginPage.loginAsAdmin();

    new SeleniumTeamCalPage().assertWeAreOnThisPage();
  }

  /**
   * This test runs a successful login for the standard admin
   */
  @Test
  public void testFailedLogin()
  {
    new SeleniumLoginPage()
        .callPage()
        .login("admin", "gjghjghj")
        .assertWeAreOnThisPage();
  }
}
