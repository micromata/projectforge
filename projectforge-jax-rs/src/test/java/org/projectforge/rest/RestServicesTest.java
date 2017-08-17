package org.projectforge.rest;

import java.sql.Date;
import java.util.Locale;

import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.AddressStatus;
import org.projectforge.test.AbstractTestBase;
import org.projectforge.web.rest.TaskDaoRest;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RestServicesTest extends AbstractTestBase
{

  @Autowired
  private AddressDaoRest addressDaoRest;

  @Autowired
  private TaskDaoRest taskDaoRest;

  @Autowired
  private AddressDao addressDao;

  private final static int SUCCESS_STATUS = 200;

  @BeforeClass
  public void init()
  {
    logon(TEST_FULL_ACCESS_USER);
  }

  @Test
  public void testAddressDaoRest()
  {
    AddressDO addressDO = new AddressDO();
    addressDO.setAddressStatus(AddressStatus.UPTODATE);
    addressDO.setAddressText("Some nice text");
    addressDO.setBirthday(new Date(System.currentTimeMillis()));
    addressDO.setBusinessPhone("1-800-STARWARS");
    addressDO.setCity("Kassel");
    addressDO.setCountry("Germany");
    addressDO.setName("Hesse");
    addressDO.setFirstName("Marcel");
    addressDO.setEmail("mail@acme.com");
    addressDO.setComment("No");
    addressDO.setCommunicationLanguage(Locale.getDefault());
    addressDO.setDivision("district 9");
    addressDO.setFax("3,1415");
    addressDO.setMobilePhone("888888888888888888888888888888");
    addressDO.setOrganization("ACME");
    addressDO.setPostalAddressText("Main Street 5");
    addressDO.setPostalCity("City2");
    addressDO.setPostalZipCode("159753");
    addressDO.setPostalCountry("country");
    addressDO.setPrivateAddressText("Second Street 2");
    addressDO.setPrivateCity("City.priv");
    addressDO.setPrivateState("true");
    addressDO.setPrivateCountry("Best Korea");
    addressDO.setPrivateZipCode("1337");
    addressDO.setPrivateMobilePhone("007");
    addressDO.setPrivatePhone("I forgot my number");
    addressDO.setImageData(new byte[] { 0, 1, 3 });
    addressDao.save(addressDO);

    Response response = addressDaoRest.getList("Marcel", 0l, true, true, true);
    Assert.assertTrue(((String) response.getEntity()).contains("\"firstName\":\"Marcel\""));
    Assert.assertTrue(response.getStatus() == SUCCESS_STATUS);

    response = addressDaoRest.getList("Marcel", 0l, false, true, true);
    Assert.assertFalse(((String) response.getEntity()).contains("\"firstName\":\"Marcel\""));
    Assert.assertTrue(response.getStatus() == SUCCESS_STATUS);

    response = addressDaoRest.getList("Marcel", 0l, true, false, true);
    Assert.assertTrue(((String) response.getEntity()).contains("\"firstName\":\"Marcel\""));
    String base64ImageData = Base64.encodeBase64String(new byte[] { 0, 1, 3 });
    Assert.assertTrue(((String) response.getEntity()).contains("\"image\":\"" + base64ImageData + "\""));
    Assert.assertTrue(response.getStatus() == SUCCESS_STATUS);
  }

  @Test
  public void testTaskDaoRest()
  {
    Response response = taskDaoRest.getList("ProjectForge", true, false, false, false);
    Assert.assertTrue(response.getStatus() == SUCCESS_STATUS);
    Assert.assertTrue(
        ((String) response.getEntity()).contains("\"shortDescription\":\"ProjectForge root task"));
  }
}
