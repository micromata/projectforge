package org.projectforge.web.selenium.fibu;

import static org.testng.Assert.assertTrue;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;
import org.projectforge.web.selenium.EditPage;

public class SeleniumEmployeeEditPage extends AddressAndBankingEditingPage<SeleniumEmployeeEditPage>
{

  public static final String status_FEST_ANGESTELLTER = "FEST_ANGESTELLTER";
  public static final String status_BEFRISTET_ANGESTELLTER = "BEFRISTET_ANGESTELLTER";
  public static final String status_FREELANCER = "FREELANCER";
  public static final String status_AUSHILFE = "AUSHILFE";
  public static final String status_STUDENTISCHE_HILFSKRAFT = "STUDENTISCHE_HILFSKRAFT";
  public static final String status_STUD_ABSCHLUSSARBEIT = "STUD_ABSCHLUSSARBEIT";
  public static final String status_PRAKTIKANT = "PRAKTIKANT";
  public static final String gender_NOT_KNOWN = "NOT_KNOWN";
  public static final String gender_MALE = "MALE";
  public static final String gender_FEMALE = "FEMALE";
  public static final String gender_NOT_APPLICABLE = "NOT_APPLICABLE";

  public SeleniumEmployeeEditPage()
  {
    super();
  }

  public SeleniumEmployeeEditPage(int id)
  {
    super(id);
  }

  @Override
  protected String urlPostfix()
  {
    return "wa/employeeEdit?";
  }

  /**
   * @param userId
   */
  public SeleniumEmployeeEditPage setAssociatedUsername(String userId)
  {
    return setStringElementById("user", userId);
  }

  /**
   * @param kost1
   */
  public SeleniumEmployeeEditPage setKost1(String kost1)
  {
    return setStringElementById("kost1", kost1);
  }

  /**
   * Set the status with the defined stauts_* constants in this class
   *
   * @param status
   */
  public SeleniumEmployeeEditPage setStatus(String status)
  {
    Select statusSelect = new Select(driver.findElement(By.id("status")));
    statusSelect.selectByValue(status);
    return this;
  }

  /**
   * Set the gender with the defined gender_* constants in this class
   *
   * @param gender
   */
  public SeleniumEmployeeEditPage setGender(String gender)
  {
    Select statusSelect = new Select(driver.findElement(By.id("gender")));
    statusSelect.selectByValue(gender);
    return this;
  }

  public SeleniumEmployeeEditPage setStaffNumber(String staffNumber)
  {
    return setStringElementById("staffNumber", staffNumber);
  }
  /**
   * CAUTION: this method cuts off everything after the "?" in the given URL-Prefix
   *
   * @param prefix
   * @return
   */
  @Override
  public SeleniumEmployeeEditPage currentPageUrlStartsWith(String prefix)
  {
    assertTrue(driver.getCurrentUrl().startsWith(prefix.split("\\?")[0]));
    return this;
  }
}
