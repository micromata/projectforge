package org.projectforge.web.selenium.login;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.projectforge.web.selenium.Const;
import org.projectforge.web.selenium.TestPageBase;

public class SeleniumLoginPage extends TestPageBase<SeleniumLoginPage>
{

  public SeleniumLoginPage()
  {

  }

  /**
   * fills the two input fields with the given parameters and clicks "login"
   *
   * @param username
   * @param password
   */
  public SeleniumLoginPage login(String username, String password)
  {
    WebElement usernameTextField = driver.findElement(By.id("username"));
    usernameTextField.clear();
    usernameTextField.sendKeys(username);

    WebElement passwordTextField = driver.findElement(By.id("password"));
    passwordTextField.clear();
    passwordTextField.sendKeys(password);

    clickAndWaitForFullPageReload("loginButton");
    return this;
  }

  public SeleniumLoginPage loginAsAdmin()
  {
    return login(Const.ADMIN_USERNAME, Const.ADMIN_PASSWORD);
  }

  /**
   * fills the two input fields with the given parameters and clicks "login"
   * and checks the stay logged in checkbox
   *
   * @param username
   * @param password
   */
  public SeleniumLoginPage loginAndStayLoggedIn(String username, String password)
  {
    WebElement stayLoggedIn = driver.findElement(By.id("loggedIn"));
    if (stayLoggedIn.isSelected() == false) {
      stayLoggedIn.click();
    }
    return login(username, password);
  }

  @Override
  public String getUrlPostfix()
  {
    return "wa/login?";
  }
}
