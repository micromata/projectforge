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

package org.projectforge.business.fibu;

import org.junit.jupiter.api.Test;
import org.projectforge.test.AbstractTestBase;
import org.projectforge.web.session.UserAgentBrowser;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

public class InvoiceServiceTest extends AbstractTestBase {
  @Autowired
  private InvoiceService invoiceService;

  @Test
  public void invoiceFilenameEmptyTest() {
    RechnungDO data = new RechnungDO();
    String filename = invoiceService.getInvoiceFilename(data, UserAgentBrowser.UNKNOWN);
    assertNotNull(filename);
    assertTrue(filename.length() < 256);
    assertEquals("_.docx", filename);
  }

  @Test
  public void invoiceFilenameStandardTest() {
    RechnungDO data = new RechnungDO();
    data.setNummer(12345);
    KundeDO kunde = new KundeDO();
    kunde.setName("Kunde");
    data.setKunde(kunde);
    ProjektDO projekt = new ProjektDO();
    projekt.setName("Projekt");
    data.setProjekt(projekt);
    data.setBetreff("Betreff");
    LocalDate date = LocalDate.of(2017, Month.AUGUST, 4);
    data.setDatum(java.sql.Date.valueOf(date));

    String filename = invoiceService.getInvoiceFilename(data, UserAgentBrowser.UNKNOWN);
    assertNotNull(filename);
    assertTrue(filename.length() < 256);
    assertEquals("12345_Kunde_Projekt_Betreff_04_08_2017.docx", filename);
  }

  @Test
  public void invoiceFilenameSpecialCharacterTest() {
    RechnungDO data = new RechnungDO();
    data.setNummer(12345);
    KundeDO kunde = new KundeDO();
    kunde.setName("Kunde & Kunde");
    data.setKunde(kunde);
    ProjektDO projekt = new ProjektDO();
    projekt.setName("Projekt-Titel");
    data.setProjekt(projekt);
    data.setBetreff("Betreff/Änderung?");
    LocalDate date = LocalDate.of(2017, Month.AUGUST, 4);
    data.setDatum(java.sql.Date.valueOf(date));
    logon(TEST_USER);
    String filename = invoiceService.getInvoiceFilename(data, UserAgentBrowser.UNKNOWN);
    assertNotNull(filename);
    assertTrue(filename.length() < 256);
    assertEquals("12345_Kunde___Kunde_Projekt-Titel_Betreff_Aenderung__04_08_2017.docx", filename);
  }

  @Test
  public void invoiceFilenameTooLongTest() {
    RechnungDO data = new RechnungDO();
    data.setNummer(12345);
    KundeDO kunde = new KundeDO();
    kunde.setName("Kunde König");
    data.setKunde(kunde);
    ProjektDO projekt = new ProjektDO();
    projekt.setName("Projekt: $§webapp");
    data.setProjekt(projekt);
    String character = "abc";
    for (int i = 1; i < 85; i++) {
      data.setBetreff((data.getBetreff() != null ? data.getBetreff() : "") + character);
    }
    String filename = invoiceService.getInvoiceFilename(data, UserAgentBrowser.UNKNOWN);
    assertNotNull(filename);
    assertTrue(filename.length() < 256);
    assertEquals("12345_Kunde_Koenig_Projekt____webapp_abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc....docx",
            filename, "Assertions.equals is dependent from property projectforge.domain!");
  }
}
