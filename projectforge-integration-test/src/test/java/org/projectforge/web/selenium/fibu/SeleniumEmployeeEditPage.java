package org.projectforge.web.selenium.fibu;

import static org.testng.Assert.assertTrue;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class SeleniumEmployeeEditPage extends AddressAndBankingEditingPage<SeleniumEmployeeEditPage>
{

  public static final String status_FEST_ANGESTELLTER = "0_fibu.employee.status.festAngestellter";
  public static final String status_BEFRISTET_ANGESTELLTER = "1_fibu.employee.status.befristetAngestellter";
  public static final String status_FREELANCER = "2_fibu.employee.status.freelancer";
  public static final String status_AUSHILFE = "3_fibu.employee.status.aushilfe";
  public static final String status_STUDENTISCHE_HILFSKRAFT = "4_fibu.employee.status.studentischeHilfskraft";
  public static final String status_STUD_ABSCHLUSSARBEIT = "5_fibu.employee.status.studentischeAbschlussarbeit";
  public static final String status_PRAKTIKANT = "6_fibu.employee.status.praktikant";
  public static final String status_AZUBI = "7_fibu.employee.status.azubi";
  public static final String gender_NOT_KNOWN = "NOT_KNOWN";
  public static final String gender_MALE = "MALE";
  public static final String gender_FEMALE = "FEMALE";
  public static final String gender_NOT_APPLICABLE = "NOT_APPLICABLE";
  public static final String nutrition_omnivorous = "0_fibu.employee.nutrition.omnivorous";
  public static final String nutrition_vegetarian = "1_fibu.employee.nutrition.vegetarian";
  public static final String nutrition_vegan = "2_fibu.employee.nutrition.vegan";

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
  public SeleniumEmployeeEditPage setStatus(String date, String status)
  {
    clickOnElement("employeestatus-addButton");
    setStringElementById("employeestatus-startTime", date);
    clickOnElement("employeestatus-status");
    String input = "//option[@value='" + status + "']";
    driver.findElement(By.xpath(input)).click();
    return this;
  }

  public SeleniumEmployeeEditPage deleteAttreData(String id)
  {
    clickAndWaitForFullPageReload(id);
    String input = "//button[@value='Do it!']";
    List<WebElement> elements = driver.findElements(By.xpath(input));
    for (WebElement element : elements) {
      if (element.getText().equals("Ja") || element.getText().equals("Yes")) {
        element.click();
        waitForPageReload(element);
        return this;
      }
    }
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

  /**
   * Set the nutrition with the defined nutrition_* constants in this class
   *
   * @param nutrition
   */
  public SeleniumEmployeeEditPage setNutrition(String nutrition)
  {
    driver.findElement(By.id("nutrition-nutrition")).click();
    String input = "//option[@value='" + nutrition + "']";
    driver.findElement(By.xpath(input)).click();
    return this;
  }

  public SeleniumEmployeeEditPage setHealthinsurance(String number, String name, String date)
  {
    setStringElementById("healthinsurance-startTime", date);
    setStringElementById("healthinsurance-name", name);
    setStringElementById("healthinsurance-number", number);
    return this;
  }

  public SeleniumEmployeeEditPage setWageTax(String date, String wageTax)
  {
    driver.findElement(By.id("wagetax-startTime")).click();
    driver.findElement(By.id("wagetax-startTime")).clear();
    driver.findElement(By.id("wagetax-startTime")).sendKeys(date);
    driver.findElement(By.id("wagetax-taxbracket")).click();
    driver.findElement(By.id("wagetax-taxbracket")).clear();
    driver.findElement(By.id("wagetax-taxbracket")).sendKeys(wageTax);
    return this;
  }

  public SeleniumEmployeeEditPage setWeekendWork(String date, String saturday, String sunday, String holydays)
  {
    driver.findElement(By.id("weekendwork-startTime")).click();
    driver.findElement(By.id("weekendwork-startTime")).clear();
    driver.findElement(By.id("weekendwork-startTime")).sendKeys(date);

    driver.findElement(By.id("weekendwork-workinghourssaturday")).click();
    driver.findElement(By.id("weekendwork-workinghourssaturday")).clear();
    driver.findElement(By.id("weekendwork-workinghourssaturday")).sendKeys(saturday);

    driver.findElement(By.id("weekendwork-workinghourssunday")).click();
    driver.findElement(By.id("weekendwork-workinghourssunday")).clear();
    driver.findElement(By.id("weekendwork-workinghourssunday")).sendKeys(sunday);

    driver.findElement(By.id("weekendwork-workinghoursholiday")).click();
    driver.findElement(By.id("weekendwork-workinghoursholiday")).clear();
    driver.findElement(By.id("weekendwork-workinghoursholiday")).sendKeys(holydays);
    return this;
  }

  public SeleniumEmployeeEditPage setProbation(String probation)
  {
    return setStringElementById("probation-probation", probation);
  }

  public String getProbation()
  {
    return getStringElementById("probation-probation");
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

  public SeleniumEmployeeEditPage setPayeTaxNumber(String payeTaxNumber)
  {
    return setStringElementById("payetaxnumber-payetaxnumber", payeTaxNumber);
  }

  public SeleniumEmployeeEditPage setEndDate(final String data)
  {
    setStringElementById("endDate", data);
    return this;
  }
}
