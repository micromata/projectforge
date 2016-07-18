package org.projectforge.web.selenium.administration;

import org.openqa.selenium.By;
import org.projectforge.web.selenium.EditPage;

public class SeleniumUserEditPage extends EditPage<SeleniumUserEditPage>
{

  public String getUserName() {
    return getStringElementById("userName");
  }

  public SeleniumUserEditPage setUserName(String value) {
    return setStringElementById("userName", value);
  }

  public SeleniumUserEditPage setFirstName(String value) {
    return setStringElementById("firstName", value);
  }

  public String getFirstName() {
    return getStringElementById("firstName");
  }

  public String getLastName() {
    return getStringElementById("lastName");
  }

  public SeleniumUserEditPage setLastName(String value) {
    return setStringElementById("lastName", value);
  }

  public String getOrganization() {
    return getStringElementById("organization");
  }

  public SeleniumUserEditPage setOrganization(String value) {
    return setStringElementById("organization",value);
  }

  public String getEmail() {
    return getStringElementById("email");
  }

  public SeleniumUserEditPage setEmail(String value) {
    return setStringElementById("email",value);
  }

  public String getJiraUsername() {
    return getStringElementById("jiraUsername");
  }

  public SeleniumUserEditPage setJiraUsername(String value) {
    return setStringElementById("jiraUsername", value);
  }

  public SeleniumUserEditPage clickInvalidateAllStayLoggedInSessions() {
    driver.findElement(By.id("invalidateStayLoggedInSessions"));
    return this;
  }

  public SeleniumUserEditPage setPersonalPhoneIdentifiers(String value) {
    return setStringElementById("personalPhoneIdentifiers", value);
  }

  public String getPersonalPhoneIdentifiers() {
    return getStringElementById("personalPhoneIdentifiers");
  }

  public SeleniumUserEditPage setPersonalMobileNumbers(String value) {
    return setStringElementById("personalMebMobileNumbers", value);
  }

  public String getPersonalMobileNumbers() {
    return getStringElementById("personalMebMobileNumbers");
  }

  public String getDescription() {
    return getStringElementById("description");
  }

  public SeleniumUserEditPage setDescription(String value) {
    return setStringElementById("description", value);
  }

  public SeleniumUserEditPage setPassword(String value) {
    return setStringElementById("password", value);
  }

  public SeleniumUserEditPage setPasswordRepeat(String value) {
    return setStringElementById("passwordRepeat", value);
  }

  @Override
  protected String urlPostfix()
  {
    return "wa/userEdit?";
  }
}
