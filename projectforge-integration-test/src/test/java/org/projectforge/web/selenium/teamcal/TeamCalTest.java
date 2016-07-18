package org.projectforge.web.selenium.teamcal;

import org.projectforge.web.selenium.SeleniumSuiteTestBase;
import org.projectforge.web.selenium.administration.SeleniumGroupEditPage;
import org.projectforge.web.selenium.administration.SeleniumUserEditPage;
import org.projectforge.web.selenium.common.SeleniumCalendarListPage;
import org.projectforge.web.selenium.login.SeleniumLoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;

public class TeamCalTest extends SeleniumSuiteTestBase
{

  private String calendarName;

  @Test
  public void test()
  {
    SeleniumLoginPage seleniumLoginPage = new SeleniumLoginPage();
    seleniumLoginPage
        .callPage()
        .loginAsAdmin();

    prepareUsersAndGroups();

    seleniumLoginPage
        .callPage()
        .login("teamCalteamCalAOwnerUser", "admin1");
    checkSelectAccess(true, "teamCalteamCalAOwnerUser", "teamCalBFullUser1",
        "teamCalDFullUser3", "teamCalEReadonlyUser1", "teamCalGReadonlyUser3", "teamCalHMinimalUser1",
        "teamCalJMinimalUser3");
    checkSelectAccess(false, "teamCalKNoAccessUser");
    checkUpdateAccess(true, "teamCalteamCalAOwnerUser");
    checkUpdateAccess(false, "teamCalBFullUser1", "teamCalDFullUser3", "teamCalEReadonlyUser1", "teamCalGReadonlyUser3",
        "teamCalHMinimalUser1",
        "teamCalJMinimalUser3");

  }

  private void checkSelectAccess(boolean accessAllowed, String... userNames)
  {
    SeleniumLoginPage seleniumLoginPage = new SeleniumLoginPage();
    for (String userName : userNames) {
      if (driver.getCurrentUrl().startsWith(seleniumLoginPage.getPageUrl()) == false) {
        seleniumLoginPage
            .logout();
      }

      seleniumLoginPage
          .callPage()
          .login(userName, "admin1");

      SeleniumCalendarListPage seleniumCalendarListPage = new SeleniumCalendarListPage();
      try {
        Assert.assertEquals(seleniumCalendarListPage
            .callPage()
            .clickRowWhereColumnLike(calendarName)
            .getTitle(), calendarName);
      } catch (Exception e) {
        if (accessAllowed) {
          Assert.fail();
        }
      }
    }
    seleniumLoginPage
        .logout();
  }

  private void checkUpdateAccess(boolean accessAllowed, String... userNames)
  {
    SeleniumLoginPage seleniumLoginPage = new SeleniumLoginPage();
    String title = null;
    for (String userName : userNames) {
      if (driver.getCurrentUrl().startsWith(seleniumLoginPage.getPageUrl()) == false) {
        seleniumLoginPage
            .logout();
      }

      seleniumLoginPage
          .callPage()
          .login(userName, "admin1");

      SeleniumCalendarListPage seleniumCalendarListPage = new SeleniumCalendarListPage();
      try {
        if (title == null) {
          title = calendarName;
        }
        seleniumCalendarListPage
            .callPage()
            .clickRowWhereColumnLike(title)
            .setId(42)
            .setTitle("Calendar of " + userName)
            .clickCreateOrUpdate();
        title = "Calendar of " + userName;

        seleniumCalendarListPage
            .callPage()
            .clickRowWhereColumnLike(title);

      } catch (Exception e) {
        if (accessAllowed) {
          Assert.fail();
        }
      }
    }
    seleniumLoginPage
        .logout();
    calendarName = title;
  }

  protected void prepareUsersAndGroups()
  {
    preparUsersAndGroups("teamCal");
  }

  private void preparUsersAndGroups(String prefix)
  {
    String firstName = "A";
    String userName = prefix + firstName + "OwnerUser";
    String password = "admin1";
    String ownerName = prefix + userName;
    addUser(firstName, "OwnerUser", ownerName, password);
    addUser("B", "FullUser1", prefix + "BFullUser1", password);
    addUser("C", "FullUser2", prefix + "CFullUser2", password);
    addUser("D", "FullUser3", prefix + "DFullUser3", password);
    addUser("E", "ReadonlyUser1", prefix + "EReadonlyUser1", password);
    addUser("F", "ReadonlyUser2", prefix + "FReadonlyUser2", password);
    addUser("G", "ReadonlyUser3", prefix + "GReadonlyUser3", password);
    addUser("H", "MinimalUser1", prefix + "HMinimalUser1", password);
    addUser("I", "MinimalUser2", prefix + "IMinimalUser2", password);
    addUser("J", "MinimalUser3", prefix + "JMinimalUser3", password);
    addUser("K", "NoAccessUser", prefix + "KNoAccessUser", password);
    addGroup("AllCalUsers", "B" + " " + "FullUser1",
        "C" + " " + "FullUser2",
        "D" + " " + "FullUser3",
        "E" + " " + "ReadonlyUser1",
        "F" + " " + "ReadonlyUser2",
        "G" + " " + "ReadonlyUser3",
        "H" + " " + "MinimalUser1",
        "I" + " " + "MinimalUser2",
        "J" + " " + "MinimalUser3",
        "K" + " " + "NoAccessUser", "A" + " " + "OwnerUser");
    addGroup(prefix + "FullGroup1", "B" + " " + "FullUser1", "A" + " " + "OwnerUser");
    addGroup(prefix + "ReadonlyGroup1", "E" + " " + "ReadonlyUser1", "A" + " " + "OwnerUser");
    addGroup(prefix + "MinimalGroup", "H" + " " + "MinimalUser1", "A" + " " + "OwnerUser");

    SeleniumLoginPage seleniumLoginPage = new SeleniumLoginPage();
    seleniumLoginPage.logout();
    seleniumLoginPage
        .callPage()
        .login("teamCalteamCalAOwnerUser", password);

    calendarName = new Date().toString();
    SeleniumCalendarListPage seleniumCalendarListPage = new SeleniumCalendarListPage();
    seleniumCalendarListPage
        .callPage()
        .addEntry()
        .setTitle(calendarName)
        .setOwner("teamCalteamCalAOwnerUser")
        .setFullAccessGroups(prefix + "FullGroup1")
        .setReadOnlyAccessGroups(prefix + "ReadonlyGroup1")
        .setMinimalAccessGroups(prefix + "MinimalGroup")
        .setFullAccessUsers("D" + " " + "FullUser3")
        .setReadOnlyAccessUsers("G" + " " + "ReadonlyUser3")
        .setMinimalAccessUsers("J" + " " + "MinimalUser3")
        .clickCreateOrUpdate();

    seleniumLoginPage.logout();

  }

  private void addGroup(String groupName, String... userNames)
  {
    SeleniumGroupEditPage seleniumGroupEditPage = new SeleniumGroupEditPage();
    seleniumGroupEditPage
        .callPage()
        .setName(groupName)
        .chooseOptionsOfSelect2("users", userNames)
        .clickCreateOrUpdate();
  }

  private void addUser(String firstName, String lastName, String userName,
      String password)
  {
    SeleniumUserEditPage userEditPage = new SeleniumUserEditPage();
    try {
      userEditPage
          .callPage()
          .setFirstName(firstName)
          .setLastName(lastName)
          .setUserName(userName)
          .setPassword(password)
          .setPasswordRepeat(password)
          .clickCreateOrUpdate();
    } catch (Exception e) {
      userEditPage
          .expectedErrorMessage("user.error.usernameAlreadyExists");
    }
  }
}
