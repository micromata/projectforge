package org.projectforge.web.selenium.common;

import org.projectforge.web.selenium.TestPageBase;
import org.projectforge.web.selenium.login.SeleniumLoginPage;

public class SetupPage extends TestPageBase<SetupPage>
{
  @Override
  public String getUrlPostfix()
  {
    return "wa/setup?";
  }

  public SetupPage setUsername(String username) {
    return setStringElementById("username", username);
  }

  public String getUsername() {
    return getStringElementById("username");
  }

  public SetupPage setPassword(String password) {
    return setStringElementById("password", password);
  }

  public SetupPage setRepeatPassword(String repeatPassword) {
    return setStringElementById("passwordRepeat", repeatPassword);
  }

  public SetupPage setCalendarDomain(String calendarDomain) {
    return setStringElementById("calendarDomain", calendarDomain);
  }

  public String getCalendarDomain() {
    return getStringElementById("calendarDomain");
  }

  public SeleniumLoginPage clickFinish() {
    clickAndWaitForFullPageReload("finish");
    return new SeleniumLoginPage();
  }
}
