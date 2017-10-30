package org.projectforge.web.selenium.fibu;

import static org.testng.Assert.assertTrue;

import org.projectforge.web.selenium.Const;
import org.projectforge.web.selenium.SeleniumSuiteTestBase;
import org.projectforge.web.selenium.TestPageBase;
import org.projectforge.web.selenium.login.SeleniumLoginPage;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class EmployeeTest extends SeleniumSuiteTestBase
{

  @BeforeTest
  public void createAdminEmployee()
  {

    new SeleniumLoginPage()
        .callPage()
        .loginAsAdmin();
    boolean caughtException = false;
    SeleniumEmployeeListPage seleniumEmployeeListPage = new SeleniumEmployeeListPage();

    try {
      seleniumEmployeeListPage
          .callPage()
          .clickRowWhereColumnLike("Administrator1");
    } catch (Exception e) {
      caughtException = true;
    }

    if (caughtException == true) {
      seleniumEmployeeListPage
          .addEntry()
          .callPage()
          .setKost1("3.000.00.00")
          .setStatus("10.12.1999", SeleniumEmployeeEditPage.status_FEST_ANGESTELLTER)
          .setAssociatedUsername(Const.ADMIN_USERNAME)
          .setGender(SeleniumEmployeeEditPage.gender_MALE)
          .setStaffNumber("1234")
          .setPayeTaxNumber("abc")
          .clickCreateOrUpdate();
    }
    new SeleniumLoginPage().logout();
  }

  @Test
  public void testBaseData()
  {
    testRequiredBankingDetails();
    testStaffNumber();
    testProbation();
    testNutrition();
    testHealthinsurance();
    testIncomeTax();

    testActive();
    testDelete();
  }

  public void testRequiredBankingDetails()
  {
    new SeleniumLoginPage()
        .callPage()
        .loginAsAdmin();

    SeleniumEmployeeListPage seleniumEmployeeListPage = new SeleniumEmployeeListPage();
    seleniumEmployeeListPage
        .callPage()
        .clickRowWhereColumnLike("Administrator");

    String currentUrl = TestPageBase.getDriver().getCurrentUrl();
    assertTrue(currentUrl.contains("employeeEdit"));
    String employeeId = currentUrl.split("id=")[1];

    new SeleniumEmployeeEditPage(Integer.parseInt(employeeId))
        .callPage()

        // wrong input, we should stay on this page
        .setAccountHolder("Konto Vollmacht")
        .setIban("")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        // wrong input, we should stay on this page
        .setAccountHolder("")
        .setIban("dfgdfgdfgdfgdf")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        // correct input, we should go to list page
        .setAccountHolder("fgdfgdfgdfgdfg")
        .setIban("hmhvngfdfgdfg")
        .clickCreateOrUpdate();

    seleniumEmployeeListPage.assertWeAreOnThisPage();
    new SeleniumLoginPage().logout();
  }

  public void testStaffNumber()
  {
    new SeleniumLoginPage()
        .callPage()
        .loginAsAdmin();

    new SeleniumEmployeeListPage()
        .callPage()
        .clickRowWhereColumnLike("Administrator");

    String currentUrl = TestPageBase.getDriver().getCurrentUrl();
    assertTrue(currentUrl.contains("employeeEdit"));
    String employeeId = currentUrl.split("id=")[1];

    new SeleniumEmployeeEditPage(Integer.parseInt(employeeId))
        .callPage()

        // wrong input, we should stay on this page
        .setStaffNumber("asd 123")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        // wrong input, we should stay on this page
        .setStaffNumber("asd.123")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        // wrong input, we should stay on this page
        .setStaffNumber("asd-123")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        // correct input, we should go to list page
        .setStaffNumber("asd123")
        .clickCreateOrUpdate();

    new SeleniumEmployeeListPage().assertWeAreOnThisPage();
    new SeleniumLoginPage().logout();
  }

  public void testProbation()
  {
    new SeleniumLoginPage()
        .callPage()
        .loginAsAdmin();

    new SeleniumEmployeeListPage()
        .callPage()
        .clickRowWhereColumnLike("Administrator");

    String currentUrl = TestPageBase.getDriver().getCurrentUrl();
    assertTrue(currentUrl.contains("employeeEdit"));
    String employeeId = currentUrl.split("id=")[1];

    SeleniumEmployeeEditPage seleniumEmployeeEditPage = new SeleniumEmployeeEditPage(Integer.parseInt(employeeId));

    seleniumEmployeeEditPage
        .callPage()

        // wrong input, we should stay on this page
        .setProbation("asd")
        .clickCreateOrUpdate();

    new SeleniumEmployeeListPage().assertWeAreOnThisPage();

    seleniumEmployeeEditPage
        .callPage()

        // wrong input, we should stay on this page
        .setProbation("1234")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        // correct input, we should go to list page
        .setProbation("12.12.1999")
        .clickCreateOrUpdate();

    new SeleniumEmployeeListPage().assertWeAreOnThisPage();

    Assert.assertEquals("12.12.1999", seleniumEmployeeEditPage.callPage().getProbation());
    new SeleniumLoginPage().logout();
  }

  public void testNutrition()
  {
    new SeleniumLoginPage()
        .callPage()
        .loginAsAdmin();

    new SeleniumEmployeeListPage()
        .callPage()
        .clickRowWhereColumnLike("Administrator");

    String currentUrl = TestPageBase.getDriver().getCurrentUrl();
    assertTrue(currentUrl.contains("employeeEdit"));
    String employeeId = currentUrl.split("id=")[1];

    SeleniumEmployeeEditPage seleniumEmployeeEditPage = new SeleniumEmployeeEditPage(Integer.parseInt(employeeId));

    seleniumEmployeeEditPage
        .callPage()
        .setNutrition(SeleniumEmployeeEditPage.nutrition_omnivorous)
        .clickCreateOrUpdate();

    new SeleniumEmployeeListPage().assertWeAreOnThisPage();

    seleniumEmployeeEditPage
        .callPage()
        .setNutrition(SeleniumEmployeeEditPage.nutrition_vegan)
        .clickCreateOrUpdate();

    new SeleniumEmployeeListPage().assertWeAreOnThisPage();

    seleniumEmployeeEditPage
        .callPage()
        .setNutrition(SeleniumEmployeeEditPage.nutrition_vegetarian)
        .clickCreateOrUpdate();

    new SeleniumEmployeeListPage().assertWeAreOnThisPage();
    new SeleniumLoginPage().logout();
  }

  public void testHealthinsurance()
  {
    new SeleniumLoginPage()
        .callPage()
        .loginAsAdmin();

    new SeleniumEmployeeListPage()
        .callPage()
        .clickRowWhereColumnLike("Administrator");

    String currentUrl = TestPageBase.getDriver().getCurrentUrl();
    assertTrue(currentUrl.contains("employeeEdit"));
    String employeeId = currentUrl.split("id=")[1];

    SeleniumEmployeeEditPage seleniumEmployeeEditPage = new SeleniumEmployeeEditPage(Integer.parseInt(employeeId));

    seleniumEmployeeEditPage
        .callPage()
        .clickOnElement("healthinsurance-addButton")
        .setHealthinsurance("", "", "")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        .setHealthinsurance("asd123", "asd123", "03.03.2017")
        .clickCreateOrUpdate();

    new SeleniumEmployeeListPage().assertWeAreOnThisPage();

    new SeleniumLoginPage().logout();
  }

  public void testIncomeTax()
  {
    new SeleniumLoginPage()
        .callPage()
        .loginAsAdmin();

    new SeleniumEmployeeListPage()
        .callPage()
        .clickRowWhereColumnLike("Administrator");

    String currentUrl = TestPageBase.getDriver().getCurrentUrl();
    assertTrue(currentUrl.contains("employeeEdit"));
    String employeeId = currentUrl.split("id=")[1];

    SeleniumEmployeeEditPage seleniumEmployeeEditPage = new SeleniumEmployeeEditPage(Integer.parseInt(employeeId));

    seleniumEmployeeEditPage
        .callPage()
        .clickOnElement("wagetax-addButton")
        .setWageTax("20.10.1999", "")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        .setWageTax("20.10.1999", "8")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        .setWageTax("20.10.1999", "a")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        .setWageTax("21.10.1999", "2")
        .clickCreateOrUpdate();

    new SeleniumEmployeeListPage().assertWeAreOnThisPage();
    new SeleniumLoginPage().logout();
  }

  public void testActive()
  {
    new SeleniumLoginPage()
        .callPage()
        .loginAsAdmin();

    new SeleniumEmployeeListPage()
        .callPage()
        .clickRowWhereColumnLike("Administrator");

    String currentUrl = TestPageBase.getDriver().getCurrentUrl();
    assertTrue(currentUrl.contains("employeeEdit"));
    String employeeId = currentUrl.split("id=")[1];

    SeleniumEmployeeEditPage seleniumEmployeeEditPage = new SeleniumEmployeeEditPage(Integer.parseInt(employeeId));

    seleniumEmployeeEditPage
        .callPage()
        .setEndDate("06.06.2016")
        .clickCreateOrUpdate();

    boolean caughtException = false;
    try {
      new SeleniumEmployeeListPage()
          .callPage()
          .setOptionPanel(true, false)
          .clickRowWhereColumnLike("Administrator");
    } catch (Exception e) {
      caughtException = true;
    }
    assertTrue(caughtException);

    new SeleniumEmployeeListPage()
        .callPage()
        .setOptionPanel(true, false);

    new SeleniumLoginPage().logout();
  }

  public void testDelete()
  {
    new SeleniumLoginPage()
        .callPage()
        .loginAsAdmin();

    new SeleniumEmployeeListPage()
        .callPage()
        .clickRowWhereColumnLike("Administrator");

    String currentUrl = TestPageBase.getDriver().getCurrentUrl();
    assertTrue(currentUrl.contains("employeeEdit"));
    String employeeId = currentUrl.split("id=")[1];

    SeleniumEmployeeEditPage seleniumEmployeeEditPage = new SeleniumEmployeeEditPage(Integer.parseInt(employeeId));

    seleniumEmployeeEditPage
        .callPage()
        .clickmarkAsDeleted();

    boolean caughtException = false;
    try {
      new SeleniumEmployeeListPage()
          .callPage()
          .clickRowWhereColumnLike("Administrator");
    } catch (Exception e) {
      caughtException = true;
    }
    assertTrue(caughtException);

    new SeleniumEmployeeListPage()
        .callPage()
        .setOptionPanel(false, true)
        .clickRowWhereColumnLike("Administrator");

    seleniumEmployeeEditPage = new SeleniumEmployeeEditPage(Integer.parseInt(employeeId));

    seleniumEmployeeEditPage
        .callPage()
        .clickmarkAsUnDeleted();

    new SeleniumEmployeeListPage()
        .callPage()
        .setOptionPanel(false, true);

    new SeleniumLoginPage().logout();
  }

  public void testWeekendWork()
  {
    new SeleniumLoginPage()
        .callPage()
        .loginAsAdmin();

    new SeleniumEmployeeListPage()
        .callPage()
        .clickRowWhereColumnLike("Administrator");

    String currentUrl = TestPageBase.getDriver().getCurrentUrl();
    assertTrue(currentUrl.contains("employeeEdit"));
    String employeeId = currentUrl.split("id=")[1];

    SeleniumEmployeeEditPage seleniumEmployeeEditPage = new SeleniumEmployeeEditPage(Integer.parseInt(employeeId));

    seleniumEmployeeEditPage
        .callPage()
        .clickOnElement("weekendwork-addButton")
        .setWeekendWork("20.10.1999", "", "", "")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        .setWeekendWork("20.10.1999", "1", "1", "")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        .setWeekendWork("20.10.1999", "1", "", "1")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        .setWeekendWork("20.10.1999", "", "1", "1")
        .clickCreateOrUpdate()
        .assertWeAreOnThisPage()

        .setWeekendWork("20.10.1999", "1", "1", "1")
        .clickCreateOrUpdate();

    new SeleniumEmployeeListPage().assertWeAreOnThisPage();
    new SeleniumLoginPage().logout();
  }

}