/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.business.fibu.kost;

import static org.testng.AssertJUnit.assertEquals;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.junit.Assert;
import org.projectforge.business.fibu.EingangsrechnungDO;
import org.projectforge.business.fibu.EingangsrechnungsPositionDO;
import org.projectforge.business.fibu.PaymentType;
import org.projectforge.business.fibu.kost.reporting.SEPATransferGenerator;
import org.projectforge.business.fibu.kost.reporting.SEPATransferResult;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.generated.CreditTransferTransactionInformationSCT;
import org.projectforge.generated.Document;
import org.projectforge.generated.PaymentInstructionInformationSCT;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class SEPATransferGeneratorTest extends AbstractTestBase
{

  @Autowired
  private SEPATransferGenerator SEPATransferGenerator;

  @Test
  public void generateTransfer() throws UnsupportedEncodingException
  {
    // Test error cases
    this.testInvoice("Test debitor", null, "DE12341234123412341234", "abcdefg1234", "Do stuff", new BigDecimal(100.0), false);
    this.testInvoice("Test debitor", "Test creditor", null, "abcdefg1234", "Do stuff", new BigDecimal(100.0), false);
    this.testInvoice("Test debitor", "Test creditor", "DE12341234123412341234", null, "", new BigDecimal(100.0), false);
    this.testInvoice("Test debitor", "Test creditor", "DE12341234123412341234", "abcdefg1234", null, new BigDecimal(100.0), false);
    this.testInvoice("Test debitor", "Test creditor", "DE12341234123412341234", "abcdefg1234", "Do stuff", new BigDecimal(0.0), false);
    this.testInvoice("Test debitor", "Test creditor", "41234", "abcdefg1234", "Do stuff", new BigDecimal(100.0), false);
    this.testInvoice("Test debitor", "Test creditor", "DE12341234123412341234", "abcde", "Do stuff", new BigDecimal(100.0), false);

    // Test success cases
    this.testInvoice("Test debitor", "Test creditor", "DE12341234123412341234", "abcdefg1234", "Do stuff", new BigDecimal(100.0), true);
    this.testInvoice("Test debitor", "Test creditor", "DE12341234123412341234", "abcdefg1234", "Do stuff", new BigDecimal(123456.56), true);

  }

  private void testInvoice(final String debitor, final String creditor, final String iban, final String bic, final String purpose, final BigDecimal amount,
      boolean ok)
      throws UnsupportedEncodingException
  {
    EingangsrechnungDO invoice = new EingangsrechnungDO();

    // set values
    invoice.setPk(1234);
    TenantDO tenant = new TenantDO();
    tenant.setName(debitor);
    invoice.setTenant(tenant);
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

    if (ok == false) {
      Assert.assertFalse(result.isSuccessful());
      return;
    }

    Assert.assertTrue(result.isSuccessful());

    // unmarshall
    Document document = this.SEPATransferGenerator.parse(result.getXml());
    Assert.assertNotNull(document);

    final PaymentInstructionInformationSCT pmtInf = document.getCstmrCdtTrfInitn().getPmtInf().get(0);
    Assert.assertNotNull(pmtInf);
    final CreditTransferTransactionInformationSCT cdtTrfTxInf = pmtInf.getCdtTrfTxInf().get(0);
    Assert.assertNotNull(cdtTrfTxInf);

    // check debitor
    Assert.assertEquals(debitor, pmtInf.getDbtr().getNm());

    // check creditor
    Assert.assertEquals(creditor, cdtTrfTxInf.getCdtr().getNm());
    Assert.assertEquals(iban, cdtTrfTxInf.getCdtrAcct().getId().getIBAN());
    Assert.assertEquals(bic.toUpperCase(), cdtTrfTxInf.getCdtrAgt().getFinInstnId().getBIC());
    Assert.assertEquals(purpose, cdtTrfTxInf.getRmtInf().getUstrd());

    // check sum
    Assert.assertEquals(amount.doubleValue(), document.getCstmrCdtTrfInitn().getGrpHdr().getCtrlSum().doubleValue(), 0.0000001);
    Assert.assertEquals(amount.doubleValue(), pmtInf.getCtrlSum().doubleValue(), 0.0000001);
    Assert.assertEquals(amount.doubleValue(), cdtTrfTxInf.getAmt().getInstdAmt().getValue().doubleValue(), 0.0000001);
  }
}
