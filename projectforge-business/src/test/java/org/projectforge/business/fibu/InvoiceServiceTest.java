package org.projectforge.business.fibu;

import org.projectforge.test.AbstractTestBase;
import org.projectforge.web.session.UserAgentBrowser;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Month;
import java.util.Calendar;

import static org.testng.AssertJUnit.*;

public class InvoiceServiceTest extends AbstractTestBase {
  @Autowired
  private InvoiceService invoiceService;

  @Test
  public void invoiceFilenameEmptyTest() {
    RechnungDO data = new RechnungDO();
    String filename = invoiceService.getInvoiceFilename(data, UserAgentBrowser.UNKNOWN);
    assertNotNull(filename);
    assertTrue(filename.length() < 256);
    assertEquals(".docx", filename);
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
    assertEquals("12345_Kunde_Projekt_Betreff_08_04_2017.docx", filename);
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

    String filename = invoiceService.getInvoiceFilename(data, UserAgentBrowser.UNKNOWN);
    assertNotNull(filename);
    assertTrue(filename.length() < 256);
    assertEquals("12345_Kunde_Kunde_Projekt_Titel_Betreff_nderung__08_04_2017.docx", filename);
  }

  @Test
  public void invoiceFilenameTooLongTest() {
    RechnungDO data = new RechnungDO();
    data.setNummer(12345);
    KundeDO kunde = new KundeDO();
    kunde.setName("Kunde");
    data.setKunde(kunde);
    ProjektDO projekt = new ProjektDO();
    projekt.setName("Projekt");
    data.setProjekt(projekt);
    String character = "abc";
    for (int i = 1; i < 85; i++) {
      data.setBetreff((data.getBetreff() != null ? data.getBetreff() : "") + character);
    }
    String filename = invoiceService.getInvoiceFilename(data, UserAgentBrowser.UNKNOWN);
    assertNotNull(filename);
    assertTrue(filename.length() < 256);
    assertEquals("Assert equals is dependent from property projectforge.domain!",
            "12345_Kunde_Projekt_abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabca[more].docx",
            filename);
  }

}
