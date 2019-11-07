/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.AddressStatus;
import org.projectforge.test.AbstractTestBase;
import org.projectforge.web.rest.TaskDaoRest;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.sql.Date;
import java.util.Locale;

public class RestServicesTest extends AbstractTestBase {
  @Autowired
  private AddressDaoRest addressDaoRest;

  @Autowired
  private TaskDaoRest taskDaoRest;

  @Autowired
  private AddressDao addressDao;

  private final static int SUCCESS_STATUS = 200;

  @BeforeEach
  public void setUp() {
    logon(AbstractTestBase.TEST_FULL_ACCESS_USER);
  }

  @Test
  public void testAddressDaoRest() {
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
    addressDO.setImageData(new byte[]{0, 1, 3});
    addressDao.save(addressDO);

    Response response = addressDaoRest.getList("", 0L, true, true, true);
    Assertions.assertTrue(((String) response.getEntity()).contains("\"firstName\":\"Marcel\""));
    Assertions.assertEquals(response.getStatus(), SUCCESS_STATUS);

    response = addressDaoRest.getList("", 0L, false, true, true);
    Assertions.assertFalse(((String) response.getEntity()).contains("\"firstName\":\"Marcel\""));
    Assertions.assertEquals(response.getStatus(), SUCCESS_STATUS);

    response = addressDaoRest.getList("", 0L, true, false, true);
    Assertions.assertTrue(((String) response.getEntity()).contains("\"firstName\":\"Marcel\""));
    String base64ImageData = Base64.encodeBase64String(new byte[]{0, 1, 3});
    Assertions.assertTrue(((String) response.getEntity()).contains("\"image\":\"" + base64ImageData + "\""));
    Assertions.assertEquals(response.getStatus(), SUCCESS_STATUS);
  }

  @Test
  public void testTaskDaoRest() {
    Response response = taskDaoRest.getList("", true, false, false, false);
    Assertions.assertEquals(response.getStatus(), SUCCESS_STATUS);
    Assertions.assertTrue(
            ((String) response.getEntity()).contains("\"shortDescription\":\"ProjectForge root task"));
  }
}
