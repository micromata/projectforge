/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.business.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class SEPATransferGeneratorTest extends AbstractTestBase {
  private SEPATransferGenerator SEPATransferGenerator = new SEPATransferGenerator();

  @Autowired
  private ConfigurationDao configurationDao;

  @Test
  public void generateTransfer() throws Exception {
    ConfigurationDO param = configurationDao.getEntry(ConfigurationParam.ORGANIZATION);
    param.setStringValue("Test debitor");
    configurationDao.update(param, false);
    // Test error cases
    this.testInvoice("Test debitor", null, "DE12341234123412341234", "abcdefg1234", "Do stuff", new BigDecimal(100.0), false);
    this.testInvoice("Test debitor", "Test creditor", null, "abcdefg1234", "Do stuff", new BigDecimal(100.0), false);
    this.testInvoice("Test debitor", "Test creditor", "DE12341234123412341234", "abcdefg1234", null, new BigDecimal(100.0), false);
    this.testInvoice("Test debitor", "Test creditor", "DE12341234123412341234", "abcdefg1234", "Do stuff", new BigDecimal(0.0), false);
    this.testInvoice("Test debitor", "Test creditor", "41234", "abcdefg1234", "Do stuff", new BigDecimal(100.0), false);
    this.testInvoice("Test debitor", "Test creditor", "AB12341234123412341234", "abcde", "Do stuff", new BigDecimal(100.0), false);
    this.testInvoice("Test debitor", "Test creditor", "AB12341234123412341234", null, "Do stuff", new BigDecimal(100.0), false);

    // Test success cases
    this.testInvoice("Test debitor", "Test creditor", "DE12341234123412341234", "abcdefg1234", "Do stuff", new BigDecimal(100.0), true);
    this.testInvoice("Test debitor", "Test creditor", "DE12341234123412341234", "abcdefg1234", "Do stuff", new BigDecimal(123456.56), true);
    this.testInvoice("Test debitor", "Test creditor", "DE12341234123412341234", null, "Do stuff", new BigDecimal(100.0), true);
  }

  @Test
  public void testIban() {
    ConfigurationDO param = configurationDao.getEntry(ConfigurationParam.ORGANIZATION);
    param.setStringValue("ACME INC.");
    configurationDao.update(param, false);
    String iban = "DE12 3456 7890 1234 5678 90";
    String bic = null;
    String xml = testIban(iban, null);
    Assertions.assertNotNull(xml);
    Assertions.assertTrue(xml.contains("<IBAN>" + iban.replaceAll("\\s", "") + "</IBAN>"), "xml: " + xml);
    Assertions.assertTrue(xml.contains("<Nm>ACME INC.</Nm>"), "xml: " + xml);
    Assertions.assertTrue(xml.contains("<Nm>Kai Reinhard</Nm>"), "xml: " + xml);
    Assertions.assertTrue(xml.contains("<IBAN>DE87200500001234567890</IBAN>"), "xml: " + xml);
    xml = testIban("IT12 3456 7890 1234 5678 90", null);
    Assertions.assertNull(xml, "xml: " + xml);
    iban = "IT12 3456 7890 1234 5678 90";
    bic = "UNCRITM1J27";
    xml = testIban(iban, bic);
    Assertions.assertNotNull("xml: " + xml, xml);
    Assertions.assertTrue(xml.contains("<IBAN>" + iban.replaceAll("\\s", "") + "</IBAN>"), "xml: " + xml);
    Assertions.assertTrue(xml.contains("<BIC>" + bic + "</BIC>"), "xml: " + xml);
  }

  private void testInvoice(final String debitor, final String creditor, final String iban, final String bic, final String purpose, final BigDecimal amount,
                           boolean ok)
      throws Exception {
    EingangsrechnungDO invoice = new EingangsrechnungDO();

    // set values
    invoice.setId(1234L);
    invoice.setPaymentType(PaymentType.BANK_TRANSFER);
    invoice.setReceiver(creditor);
    invoice.setIban(iban);
    invoice.setBic(bic);
    invoice.setReferenz(purpose);
    EingangsrechnungsPositionDO position = new EingangsrechnungsPositionDO();
    position.setEinzelNetto(amount);
    position.setMenge(BigDecimal.ONE);
    invoice.setPositionen(new ArrayList<>());
    invoice.getPositionen().add(position);

    // recalculate
    invoice.recalculate();

    SEPATransferResult result = this.SEPATransferGenerator.format(invoice);

    if (!ok) {
      Assertions.assertFalse(result.isSuccessful());
      return;
    }

    Assertions.assertTrue(result.isSuccessful());

    // Parse XML using DOM
    String xml = new String(result.getXml(), StandardCharsets.UTF_8);
    Document document = parseXml(xml);
    Assertions.assertNotNull(document);

    // check debitor using XPath
    Assertions.assertEquals(debitor, getXPathValue(document, "//Dbtr/Nm"));

    // check creditor using XPath
    Assertions.assertEquals(creditor, getXPathValue(document, "//Cdtr/Nm"));
    Assertions.assertEquals(iban, getXPathValue(document, "//CdtrAcct/Id/IBAN"));
    if (!iban.toUpperCase().startsWith("DE")) {
      Assertions.assertEquals(bic.toUpperCase(), getXPathValue(document, "//CdtrAgt/FinInstnId/BIC"));
    }
    Assertions.assertEquals(purpose, getXPathValue(document, "//Ustrd"));

    // check sum
    String ctrlSum = getXPathValue(document, "//GrpHdr/CtrlSum");
    Assertions.assertEquals(amount.doubleValue(), Double.parseDouble(ctrlSum), 0.0000001);
    String instdAmt = getXPathValue(document, "//InstdAmt");
    Assertions.assertEquals(amount.doubleValue(), Double.parseDouble(instdAmt), 0.0000001);
  }

  private Document parseXml(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(new InputSource(new StringReader(xml)));
  }

  private String getXPathValue(Document doc, String xpathExpression) throws Exception {
    XPath xpath = XPathFactory.newInstance().newXPath();
    // Convert to namespace-agnostic XPath
    String converted;
    if (xpathExpression.contains("@")) {
      converted = xpathExpression;
    } else {
      String[] parts = xpathExpression.split("/");
      StringBuilder sb = new StringBuilder();
      for (String part : parts) {
        if (part.isEmpty() || part.equals("*") || part.startsWith("@")) {
          sb.append(part);
        } else {
          sb.append("*[local-name()='").append(part).append("']");
        }
        sb.append("/");
      }
      converted = sb.substring(0, sb.length() - 1);
    }
    return (String) xpath.evaluate(converted, doc, XPathConstants.STRING);
  }

  private String testIban(String iban, String bic) {
    SEPATransferGenerator generator = new SEPATransferGenerator();
    EingangsrechnungDO invoice = new EingangsrechnungDO();
    invoice.setIban(iban);
    invoice.setBic(bic);
    invoice.setPaymentType(PaymentType.BANK_TRANSFER);
    invoice.setReceiver("Kai Reinhard");
    invoice.setReferenz("Consulting ProjectForge");
    EingangsrechnungsPositionDO position = new EingangsrechnungsPositionDO();
    position.setEingangsrechnung(invoice);
    position.setEinzelNetto(new BigDecimal(100));
    invoice.addPosition(position);
    SEPATransferResult result = generator.format(invoice);
    if (result.getXml() == null) {
      return null;
    }
    return new String(result.getXml(), StandardCharsets.UTF_8);
  }
}
