package org.projectforge.business.employee;

import static org.testng.Assert.*;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.Gender;
import org.projectforge.business.fibu.GenderConverter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.test.AbstractBase;
import org.projectforge.test.AbstractTestNGBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class EmployeeTest extends AbstractTestNGBase
{

  @Autowired
  private EmployeeDao employeeDao;

  private List<EmployeeDO> employeeList;

  @BeforeClass
  public void init()
  {
    logon(AbstractBase.TEST_FULL_ACCESS_USER);
    employeeList = employeeDao.internalLoadAll();
    assertTrue(employeeList.size() > 0, "Keine Mitarbeiter in der Test DB!");
  }

  @AfterMethod
  public void clean()
  {
    //Get initial infos
    log.info("Cleanup deleted employess -> undelete");
    log.info("Count employees: " + employeeList.size());
    assertTrue(employeeList.size() > 0);
    for (EmployeeDO e : employeeList) {
      log.info("Employee: " + e.toString());
      if (e.isDeleted()) {
        //Undelete
        employeeDao.internalUndelete(e);
      }
    }
  }

  @Test
  public void testBirthday()
  {
    //Get initial infos
    log.info("Count employees: " + employeeList.size());
    assertTrue(employeeList.size() > 0);
    EmployeeDO e = employeeList.get(0);
    log.info("Employee: " + e.toString());

    List<DisplayHistoryEntry> historyEntries = employeeDao.getDisplayHistoryEntries(e);
    log.info("Employee history entry size: " + historyEntries.size());

    //Update employee
    Calendar birthday = new GregorianCalendar(1985, Calendar.DECEMBER, 17);
    e.setBirthday(birthday.getTime());
    employeeDao.update(e);

    //Check updates
    EmployeeDO updatdEmployee = employeeDao.getById(e.getId());
    assertEquals(updatdEmployee.getBirthday().getTime(), e.getBirthday().getTime());

    // test history
    List<DisplayHistoryEntry> updatedHistoryEntries = employeeDao.getDisplayHistoryEntries(e);
    assertEquals(updatedHistoryEntries.size(), historyEntries.size() + 1);

    //Remove data and check
    e.setBirthday(null);
    employeeDao.update(e);

    //Check updates
    EmployeeDO updatdEmployeeRemove = employeeDao.getById(e.getId());
    assertNull(updatdEmployeeRemove.getBirthday());

    // test history
    List<DisplayHistoryEntry> updatedHistoryRemoveEntries = employeeDao.getDisplayHistoryEntries(e);
    assertEquals(updatedHistoryRemoveEntries.size(), historyEntries.size() + 2);
  }

  @Test
  public void testMarkAsDeleted()
  {
    //Get initial infos
    log.info("Count employees: " + employeeList.size());
    assertTrue(employeeList.size() > 0);
    EmployeeDO e = employeeList.get(0);
    log.info("Employee: " + e.toString());

    //Mark as deleted
    employeeDao.markAsDeleted(e);

    //Check updates
    EmployeeDO updatdEmployee = employeeDao.getById(e.getId());
    assertTrue(updatdEmployee.isDeleted());

    employeeDao.update(e);
  }

  @Test
  public void testGender()
  {
    //Get initial infos
    log.info("Count employees: " + employeeList.size());
    assertTrue(employeeList.size() > 0);
    EmployeeDO e = employeeList.get(0);
    log.info("Employee: " + e.toString());
    List<DisplayHistoryEntry> historyEntriesBefore = employeeDao.getDisplayHistoryEntries(e);

    e.setGender(Gender.valueOf("NOT_KNOWN"));

    assertEquals(e.getGender(), Gender.NOT_KNOWN);
    assertEquals(e.getGender().getI18nKey(), Gender.NOT_KNOWN.getI18nKey());
    assertEquals(e.getGender().ordinal(), Gender.NOT_KNOWN.ordinal());
    assertEquals(e.getGender().getIsoCode(), Gender.NOT_KNOWN.getIsoCode());

    e.setGender(Gender.MALE);

    assertEquals(e.getGender(), Gender.MALE);
    assertEquals(e.getGender().getI18nKey(), Gender.MALE.getI18nKey());
    assertEquals(e.getGender().ordinal(), Gender.MALE.ordinal());
    assertEquals(e.getGender().getIsoCode(), Gender.MALE.getIsoCode());

    e.setGender(Gender.FEMALE);

    assertEquals(e.getGender(), Gender.FEMALE);
    assertEquals(e.getGender().getI18nKey(), Gender.FEMALE.getI18nKey());
    assertEquals(e.getGender().ordinal(), Gender.FEMALE.ordinal());
    assertEquals(e.getGender().getIsoCode(), Gender.FEMALE.getIsoCode());

    e.setGender(Gender.NOT_APPLICABLE);

    assertEquals(e.getGender(), Gender.NOT_APPLICABLE);
    assertEquals(e.getGender().getI18nKey(), Gender.NOT_APPLICABLE.getI18nKey());
    assertEquals(e.getGender().ordinal(), Gender.NOT_APPLICABLE.ordinal());
    assertEquals(e.getGender().getIsoCode(), Gender.NOT_APPLICABLE.getIsoCode());

    // test history
    employeeDao.update(e);
    List<DisplayHistoryEntry> historyEntriesAfter = employeeDao.getDisplayHistoryEntries(e);
    assertEquals(historyEntriesBefore.size() + 1, historyEntriesAfter.size());

    final DisplayHistoryEntry genderHistoryEntry = historyEntriesAfter.get(0);
    assertEquals(genderHistoryEntry.getPropertyName(), "gender");
    assertEquals(genderHistoryEntry.getNewValue(), "[" + Gender.NOT_APPLICABLE.toString() + "]");
  }

  @Test
  public void testGenderConverter()
  {
    GenderConverter genderConverter = new GenderConverter();

    assertEquals(genderConverter.convertToDatabaseColumn(null).intValue(), Gender.NOT_KNOWN.getIsoCode());
    assertEquals(genderConverter.convertToDatabaseColumn(Gender.NOT_KNOWN).intValue(), Gender.NOT_KNOWN.getIsoCode());
    assertEquals(genderConverter.convertToDatabaseColumn(Gender.MALE).intValue(), Gender.MALE.getIsoCode());
    assertEquals(genderConverter.convertToDatabaseColumn(Gender.FEMALE).intValue(), Gender.FEMALE.getIsoCode());
    assertEquals(genderConverter.convertToDatabaseColumn(Gender.NOT_APPLICABLE).intValue(), Gender.NOT_APPLICABLE.getIsoCode());

    assertEquals(genderConverter.convertToEntityAttribute(null), Gender.NOT_KNOWN);
    assertEquals(genderConverter.convertToEntityAttribute(Integer.MAX_VALUE), Gender.NOT_KNOWN);
    assertEquals(genderConverter.convertToEntityAttribute(Integer.MIN_VALUE), Gender.NOT_KNOWN);
    assertEquals(genderConverter.convertToEntityAttribute(0), Gender.NOT_KNOWN);
    assertEquals(genderConverter.convertToEntityAttribute(1), Gender.MALE);
    assertEquals(genderConverter.convertToEntityAttribute(2), Gender.FEMALE);
    assertEquals(genderConverter.convertToEntityAttribute(9), Gender.NOT_APPLICABLE);
  }

  @Test
  public void testBanking()
  {
    log.info("Count employees: " + employeeList.size());
    assertTrue(employeeList.size() > 0);
    EmployeeDO e = employeeList.get(0);
    log.info("Employee: " + e.toString());
    List<DisplayHistoryEntry> historyEntriesBefore = employeeDao.getDisplayHistoryEntries(e);

    String iban = "/*/*/*/*/*///*/*//*//*/*/*/*/*/*/*/*/*/*/*/*/*////";
    e.setIban(iban);
    assertEquals(e.getIban(), iban);
    employeeDao.update(e); // for history test

    String bic = "ööäööäööäöä";
    e.setBic(bic);
    assertEquals(e.getBic(), bic);
    employeeDao.update(e); // for history test

    String accountHolder = "Mr. X";
    e.setAccountHolder(accountHolder);
    assertEquals(e.getAccountHolder(), accountHolder);
    employeeDao.update(e); // for history test

    // test history
    List<DisplayHistoryEntry> historyEntriesAfter = employeeDao.getDisplayHistoryEntries(e);
    assertEquals(historyEntriesBefore.size() + 3, historyEntriesAfter.size());

    assertHistoryEntry(historyEntriesAfter.get(0), "accountHolder", accountHolder);
    assertHistoryEntry(historyEntriesAfter.get(1), "bic", bic);
    assertHistoryEntry(historyEntriesAfter.get(2), "iban", iban);
  }

  @Test
  public void testAddress()
  {
    assertTrue(employeeList.size() > 0);
    EmployeeDO e = employeeList.get(0);
    List<DisplayHistoryEntry> historyEntriesBefore = employeeDao.getDisplayHistoryEntries(e);

    String street = "Some street";
    e.setStreet(street);
    assertEquals(e.getStreet(), street);
    employeeDao.update(e);

    String zipCode = "12345";
    e.setZipCode(zipCode);
    assertEquals(e.getZipCode(), zipCode);
    employeeDao.update(e);

    String city = "Kassel";
    e.setCity(city);
    assertEquals(e.getCity(), city);
    employeeDao.update(e);

    String country = "Deutschland";
    e.setCountry(country);
    assertEquals(e.getCountry(), country);
    employeeDao.update(e);

    String state = "Hessen";
    e.setState(state);
    assertEquals(e.getState(), state);
    employeeDao.update(e);

    // test history
    List<DisplayHistoryEntry> historyEntriesAfter = employeeDao.getDisplayHistoryEntries(e);
    assertEquals(historyEntriesBefore.size() + 5, historyEntriesAfter.size());

    assertHistoryEntry(historyEntriesAfter.get(0), "state", state);
    assertHistoryEntry(historyEntriesAfter.get(1), "country", country);
    assertHistoryEntry(historyEntriesAfter.get(2), "city", city);
    assertHistoryEntry(historyEntriesAfter.get(3), "zipCode", zipCode);
    assertHistoryEntry(historyEntriesAfter.get(4), "street", street);
  }

  @Test
  public void testStaffNumber()
  {
    assertTrue(employeeList.size() > 0);
    EmployeeDO e = employeeList.get(0);
    List<DisplayHistoryEntry> historyEntriesBefore = employeeDao.getDisplayHistoryEntries(e);

    String staffNumber = "123abc456def";
    e.setStaffNumber(staffNumber);
    assertEquals(e.getStaffNumber(), staffNumber);
    employeeDao.update(e);

    // test history
    List<DisplayHistoryEntry> historyEntriesAfter = employeeDao.getDisplayHistoryEntries(e);
    assertEquals(historyEntriesBefore.size() + 1, historyEntriesAfter.size());

    assertHistoryEntry(historyEntriesAfter.get(0), "staffNumber", staffNumber);
  }

  private void assertHistoryEntry(final DisplayHistoryEntry historyEntry, final String propertyName, final String newValue)
  {
    assertEquals(historyEntry.getPropertyName(), propertyName);
    assertEquals(historyEntry.getNewValue(), newValue);
  }

}
