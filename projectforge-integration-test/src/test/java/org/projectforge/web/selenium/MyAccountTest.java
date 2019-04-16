package org.projectforge.web.selenium;

import org.projectforge.web.selenium.fibu.SeleniumEmployeeEditPage;
import org.projectforge.web.selenium.fibu.SeleniumEmployeeListPage;
import org.projectforge.web.selenium.login.SeleniumLoginPage;
import org.junit.jupiter.api.Assertions;
import org.testng.annotations.BeforeMethod;
import org.junit.jupiter.api.Test;

public class MyAccountTest extends SeleniumSuiteTestBase
{
  @BeforeMethod
  public void createAdminEmployee()
  {
    System.out.println("createAdminEmployee()");
    System.out.println("  - new SeleniumLoginPage");
    new SeleniumLoginPage()
        .callPage()
        .loginAsAdmin();
    boolean caughtException = false;
    System.out.println("  - new SeleniumEmployeeListPage");
    SeleniumEmployeeListPage seleniumEmployeeListPage = new SeleniumEmployeeListPage();

    try {
      System.out.println("  - try seleniumEmployeeListPage");
      seleniumEmployeeListPage
          .callPage()
          .clickRowWhereColumnLike("Administrator");
    } catch (Exception e) {
      System.out.println("  - catch Exception");
      caughtException = true;
    }

    if (caughtException == true) {
      System.out.println("  - new seleniumEmployeeListPage");
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
  public void testPersonalData()
  {
    new SeleniumLoginPage()
        .callPage()
        .loginAsAdmin();

    SeleniumMyAccountPage seleniumMyAccountPage = new SeleniumMyAccountPage();
    String mail = "mymail@example.com";
    String organization = "My Company";
    String lasName = "Administrator";
    String firstName = "Vorname";
    seleniumMyAccountPage
        .callPage()
        .setFirstName(firstName)
        .setLastName(lasName)
        .setEmail(mail)
        .setOrganization(organization)
        .update()
        .callPage()
        .assertTextOfElementWithIdEquals("firstName", firstName)
        .assertTextOfElementWithIdEquals("lastName", lasName)
        .assertTextOfElementWithIdEquals("organization", organization)
        .assertTextOfElementWithIdEquals("email", mail)
        .logout();
  }

  @Test
  public void testBankingDetails()
  {
    SeleniumLoginPage seleniumLoginPage = new SeleniumLoginPage();
    seleniumLoginPage
        .callPage()
        .loginAsAdmin();

    SeleniumMyAccountPage seleniumMyAccountPage = new SeleniumMyAccountPage();

    String mail = "mymail@example.com";
    String organization = "My Company";
    String lasName = "Administrator";
    String firstName = "Vorname";
    String iban = "DE12345678901234567890";
    String accountHolder = "Vorname Nachname";
    String bic = "12345678";
    seleniumMyAccountPage
        .callPage()
        .setFirstName(firstName)
        .setLastName(lasName)
        .setEmail(mail)
        .setOrganization(organization)
        .setIban("DE1234567890123456789")
        .setBic(bic)
        .setAccountHolder(accountHolder)
        .update()
        .assertWeAreOnThisPage()
        .setIban(iban)
        .update()
        .currentPageUrlStartsWith(Const.PF_URL + "wa/wicket/page");

    SeleniumEmployeeListPage seleniumEmployeeListPage = new SeleniumEmployeeListPage();
    SeleniumEmployeeEditPage employeeEditPage = seleniumEmployeeListPage
        .callPage()
        .clickRowWhereColumnLike("Administrator");
    Assertions.assertEquals(employeeEditPage.getIban(), iban);
    Assertions.assertEquals(employeeEditPage.getBic(), bic);
    Assertions.assertEquals(employeeEditPage.getAccountHolder(), accountHolder);
  }

  @Test
  public void testAdress()
  {
    SeleniumLoginPage seleniumLoginPage = new SeleniumLoginPage();
    seleniumLoginPage
        .callPage()
        .loginAsAdmin();

    SeleniumMyAccountPage seleniumMyAccountPage = new SeleniumMyAccountPage();

    String mail = "mymail@example.com";
    String organization = "My Company";
    String lasName = "Administrator";
    String firstName = "Vorname";
    String city = "city";
    String country = "country";
    String state = "state";
    String zipcode = "123";
    String street = "street";
    seleniumMyAccountPage
        .callPage()
        .setFirstName(firstName)
        .setLastName(lasName)
        .setEmail(mail)
        .setOrganization(organization)
        .setCity(city)
        .setCountry(country)
        .setState(state)
        .setZipCode(zipcode)
        .setStreet(street)
        .update()
        .currentPageUrlStartsWith(Const.PF_URL + "wa/wicket/page");

    SeleniumEmployeeListPage seleniumEmployeeListPage = new SeleniumEmployeeListPage();
    SeleniumEmployeeEditPage employeeEditPage = seleniumEmployeeListPage
        .callPage()
        .clickRowWhereColumnLike("Administrator");

    Assertions.assertEquals(employeeEditPage.getCity(), city);
    Assertions.assertEquals(employeeEditPage.getCountry(), country);
    Assertions.assertEquals(employeeEditPage.getState(), state);
    Assertions.assertEquals(employeeEditPage.getStreet(), street);

  }

  @Test
  public void testBirthday()
  {
    SeleniumLoginPage seleniumLoginPage = new SeleniumLoginPage();
    seleniumLoginPage
        .callPage()
        .loginAsAdmin();

    SeleniumMyAccountPage seleniumMyAccountPage = new SeleniumMyAccountPage();

    String mail = "mymail@example.com";
    String organization = "My Company";
    String lasName = "Administrator";
    String firstName = "Vorname";
    String city = "city";
    String country = "country";
    String state = "state";
    String zipcode = "123";
    String street = "street";
    String birthday = "01.02.1980";
    seleniumMyAccountPage
        .callPage()
        .setFirstName(firstName)
        .setLastName(lasName)
        .setEmail(mail)
        .setOrganization(organization)
        .setCity(city)
        .setCountry(country)
        .setState(state)
        .setZipCode(zipcode)
        .setStreet(street)
        //        .setBirthday("dfdfgfdgdfg") <-- this is not possible because the input field does not allow text
        .assertWeAreOnThisPage()
        .setBirthday("01/01/1200")
        .update()
        .assertWeAreOnThisPage()
        .setBirthday(birthday)
        .update()
        .currentPageUrlStartsWith(Const.PF_URL + "wa/wicket/page");

    SeleniumEmployeeListPage seleniumEmployeeListPage = new SeleniumEmployeeListPage();
    SeleniumEmployeeEditPage employeeEditPage = seleniumEmployeeListPage
        .callPage()
        .clickRowWhereColumnLike("Administrator");

    Assertions.assertEquals(employeeEditPage.getBirthday(), birthday);
  }

}
