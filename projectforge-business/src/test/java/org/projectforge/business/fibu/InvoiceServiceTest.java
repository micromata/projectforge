package org.projectforge.business.fibu;

import static org.testng.AssertJUnit.*;

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
  public void invoiceFilenameTooLongTest()
  {
    RechnungDO data = new RechnungDO();
    String character = "abc";
    for (int i = 1; i < 85; i++) {
      data.setBetreff((data.getBetreff() != null ? data.getBetreff() : "") + character);
    }
    String filename = invoiceService.getInvoiceFilename(data);
    assertNotNull(filename);
    assertTrue(filename.length() < 256);
    assertEquals(
        "abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcab[more]_invoice.docx",
        filename);
  }

}
