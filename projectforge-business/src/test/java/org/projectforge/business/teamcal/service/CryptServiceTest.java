/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.teamcal.service;

import org.junit.jupiter.api.Test;
import org.projectforge.business.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CryptServiceTest extends AbstractTestBase
{
  @Autowired
  private CryptService cryptService;

  @Test
  public void testEncryptDecrypt()
  {
    String message = "UID=4711&ATTENDEE=4712&status=ACCEPTED";
    String encryptParameterMessage = cryptService.encryptParameterMessage(message);
    Map<String, String> decryptParameterMessage = cryptService.decryptParameterMessage(encryptParameterMessage);
    assertEquals(3, decryptParameterMessage.size());
    assertEquals("4711", decryptParameterMessage.get("UID"));
    assertEquals("4712", decryptParameterMessage.get("ATTENDEE"));
    assertEquals("ACCEPTED", decryptParameterMessage.get("status"));
  }

  @Test
  public void testDecryptFaultMessage()
  {
    String encryptParameterMessage = "1234123412341234";
    Map<String, String> decryptParameterMessage = cryptService.decryptParameterMessage(encryptParameterMessage);
    assertEquals(0, decryptParameterMessage.size());
  }
}
