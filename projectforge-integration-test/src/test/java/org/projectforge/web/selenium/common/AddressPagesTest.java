package org.projectforge.web.selenium.common;

import org.projectforge.web.selenium.SeleniumSuiteTestBase;
import org.projectforge.web.selenium.login.SeleniumLoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AddressPagesTest extends SeleniumSuiteTestBase
{
  @Test
  public void test()
  {
    SeleniumLoginPage seleniumLoginPage = new SeleniumLoginPage();
    seleniumLoginPage
        .callPage()
        .loginAsAdmin();
    SeleniumAddressListPage seleniumAddressListPage = new SeleniumAddressListPage();
    seleniumAddressListPage
        .callPage()
        .addEntry()
        .setOderbook("Global")
        .setFirstName("First")
        .setName("Second")
        .setEmail("Third")
        .setOrganization("Fourth")
        .setDivision("Fifth")
        .setPosition("Sixth")
        .setPrivateEmail("Seventh")
        .setWebsite("Eighth")
        .setBirthday("01.02.2003")
        .setFingerprint("tenth")
        .setPublicKey("eleventh")
        .setAddressStatus(SeleniumAddressEditPage.STATE_UPTODATE)
        .setForm(SeleniumAddressEditPage.FORM_UNKNOWN)
        .clickCreateOrUpdate();

    seleniumAddressListPage
        .callPage()
        .clickRowWhereColumnLike("First")
        .clickmarkAsDeleted();

    boolean failed = false;
    try {
      seleniumAddressListPage
          .callPage()
          .clickRowWhereColumnLike("First");
    } catch (Exception e) {
      failed = true;
    }

    Assert.assertTrue(failed);
  }
}
