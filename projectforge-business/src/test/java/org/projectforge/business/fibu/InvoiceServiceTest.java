package org.projectforge.business.fibu;

import static org.testng.AssertJUnit.*;

import java.sql.Date;
import java.util.Calendar;

import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class InvoiceServiceTest extends AbstractTestBase
{
  @Autowired
  private InvoiceService invoiceService;

  @Test
  public void invoiceFilenameEmptyTest()
  {
    RechnungDO data = new RechnungDO();
    String filename = invoiceService.getInvoiceFilename(data);
    assertNotNull(filename);
    assertTrue(filename.length() < 256);
    assertEquals("invoice.docx", filename);
  }

  @Test
  public void invoiceFilenameStandardTest()
  {
    RechnungDO data = new RechnungDO();
    data.setNummer(12345);
    KundeDO kunde = new KundeDO();
    kunde.setName("Kunde");
    data.setKunde(kunde);
    ProjektDO projekt = new ProjektDO();
    projekt.setName("Projekt");
    data.setProjekt(projekt);
    data.setBetreff("Betreff");
    Calendar calendar = Calendar.getInstance();
    calendar.set(2017, Calendar.AUGUST, 4);
    data.setDatum(new Date(calendar.getTimeInMillis()));

    String filename = invoiceService.getInvoiceFilename(data);
    assertNotNull(filename);
    assertTrue(filename.length() < 256);
    assertEquals("12345_Kunde_Projekt_Betreff_08_04_2017_invoice.docx", filename);
  }

  @Test
  public void invoiceFilenameSpecialCharacterTest()
  {
    RechnungDO data = new RechnungDO();
    data.setNummer(12345);
    KundeDO kunde = new KundeDO();
    kunde.setName("Kunde & Kunde");
    data.setKunde(kunde);
    ProjektDO projekt = new ProjektDO();
    projekt.setName("Projekt-Titel");
    data.setProjekt(projekt);
    data.setBetreff("Betreff/Änderung?");
    Calendar calendar = Calendar.getInstance();
    calendar.set(2017, Calendar.AUGUST, 4);
    data.setDatum(new Date(calendar.getTimeInMillis()));

    String filename = invoiceService.getInvoiceFilename(data);
    assertNotNull(filename);
    assertTrue(filename.length() < 256);
    assertEquals("12345_Kunde_Kunde_Projekt_Titel_Betreff_nderung__08_04_2017_invoice.docx", filename);
  }

  @Test
  public void invoiceFilenameTooLongTest()
  {
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
    String filename = invoiceService.getInvoiceFilename(data);
    assertNotNull(filename);
    assertTrue(filename.length() < 256);
    assertEquals(
        "12345_Kunde_Projekt_abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc[more]_invoice.docx",
        filename);
  }

}
