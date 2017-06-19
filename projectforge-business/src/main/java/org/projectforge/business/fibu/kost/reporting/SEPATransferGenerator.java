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

package org.projectforge.business.fibu.kost.reporting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.projectforge.business.fibu.EingangsrechnungDO;
import org.projectforge.business.fibu.PaymentType;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.generated.AccountIdentificationSEPA;
import org.projectforge.generated.ActiveOrHistoricCurrencyAndAmountSEPA;
import org.projectforge.generated.ActiveOrHistoricCurrencyCodeEUR;
import org.projectforge.generated.AmountTypeSEPA;
import org.projectforge.generated.BranchAndFinancialInstitutionIdentificationSEPA1;
import org.projectforge.generated.BranchAndFinancialInstitutionIdentificationSEPA3;
import org.projectforge.generated.CashAccountSEPA1;
import org.projectforge.generated.CashAccountSEPA2;
import org.projectforge.generated.CreditTransferTransactionInformationSCT;
import org.projectforge.generated.CustomerCreditTransferInitiationV03;
import org.projectforge.generated.Document;
import org.projectforge.generated.FinancialInstitutionIdentificationSEPA1;
import org.projectforge.generated.FinancialInstitutionIdentificationSEPA3;
import org.projectforge.generated.GroupHeaderSCT;
import org.projectforge.generated.ObjectFactory;
import org.projectforge.generated.PartyIdentificationSEPA1;
import org.projectforge.generated.PartyIdentificationSEPA2;
import org.projectforge.generated.PaymentIdentificationSEPA;
import org.projectforge.generated.PaymentInstructionInformationSCT;
import org.projectforge.generated.PaymentMethodSCTCode;
import org.projectforge.generated.PaymentTypeInformationSCT1;
import org.projectforge.generated.RemittanceInformationSEPA1Choice;
import org.projectforge.generated.ServiceLevelSEPA;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;

/**
 * This component generates and reads transfer files in pain.001.003.03 format.
 *
 * @author Stefan Niemczyk (s.niemczyk@micromata.de)
 */
@Component
public class SEPATransferGenerator
{
  final private static String PAIN_001_003_03_XSD = "misc/pain.001.003.03.xsd";

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SEPATransferGenerator.class);

  private Schema painSchema;
  private JAXBContext jaxbContext;
  private SimpleDateFormat formatter;

  public SEPATransferGenerator()
  {
    this.formatter = new SimpleDateFormat("yyyy-MM-dd-HH:mm");
    this.painSchema = null;
    this.jaxbContext = null;

    URL xsd = getClass().getClassLoader().getResource(PAIN_001_003_03_XSD);

    if (xsd == null) {
      log.error("pain.001.003.03.xsd file not found, transfer export not possible without it.");
      return;
    }

    try {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      painSchema = schemaFactory.newSchema(new File(xsd.getFile()));
      jaxbContext = JAXBContext.newInstance(Document.class.getPackage().getName());
    } catch (SAXException | JAXBException e) {
      log.error("An error occurred while reading pain.001.003.03.xsd and creating jaxb context -> transfer export not possible.", e);
    }
  }

  /**
   * Generates an transfer xml for the given invoice in pain.001.003.03 format.
   *
   * @param invoice The invoice to generate the transfer xml
   * @return Returns the byte array of the xml or null in case of an error.
   */
  public byte[] format(final EingangsrechnungDO invoice)
  {
    // if jaxb context is missing, generation is not possible
    if (this.jaxbContext == null) {
      log.error("Transfer export not possible, jaxb context is missing. Please check your pain.001.003.03.xsd file.");
      return null;
    }

    // validate invoice, check field values
    if (invoice.getGrossSum() == null || invoice.getGrossSum().compareTo(BigDecimal.ZERO) == 0 ||
        invoice.getPaymentType() != PaymentType.BANK_TRANSFER || invoice.getBic() == null ||
        invoice.getIban() == null || invoice.getReceiver() == null || invoice.getBemerkung() == null) {
      return null;
    }

    // Generate structure
    ObjectFactory factory = new ObjectFactory();
    String msgID = String.format("transfer-%s-%s", invoice.getPk(), this.formatter.format(new Date()));
    final BigDecimal amount = invoice.getGrossSum();
    String debitor = invoice.getTenant().getName();

    // create document
    final Document document = factory.createDocument();

    // create root element
    final CustomerCreditTransferInitiationV03 cstmrCdtTrfInitn = factory.createCustomerCreditTransferInitiationV03();
    document.setCstmrCdtTrfInitn(cstmrCdtTrfInitn);

    // create group header
    final GroupHeaderSCT grpHdr = factory.createGroupHeaderSCT();
    cstmrCdtTrfInitn.setGrpHdr(grpHdr);

    grpHdr.setMsgId(msgID);
    grpHdr.setCreDtTm(new XMLGregorianCalendarImpl((GregorianCalendar) GregorianCalendar.getInstance()));
    grpHdr.setNbOfTxs(String.valueOf(1));
    grpHdr.setCtrlSum(amount);
    final PartyIdentificationSEPA1 partyIdentificationSEPA1 = factory.createPartyIdentificationSEPA1();
    grpHdr.setInitgPty(partyIdentificationSEPA1);
    partyIdentificationSEPA1.setNm(debitor);

    // create payment information
    PaymentInstructionInformationSCT pmtInf = factory.createPaymentInstructionInformationSCT();
    cstmrCdtTrfInitn.getPmtInf().add(pmtInf);

    pmtInf.setPmtInfId(msgID + "-1");
    pmtInf.setPmtMtd(PaymentMethodSCTCode.TRF);
    pmtInf.setBtchBookg(true);
    pmtInf.setNbOfTxs("1");
    pmtInf.setCtrlSum(amount);
    // the default value for most HBCI/FinTS is 01/01/1999 or the current date
    // pmtInf.setReqdExctnDt(XMLGregorianCalendarImpl.createDate(1999, 1, 1, 0));
    pmtInf.setReqdExctnDt(new XMLGregorianCalendarImpl((GregorianCalendar) GregorianCalendar.getInstance()));
    // pmtInf.setChrgBr(ChargeBearerTypeSEPACode.SLEV); // TODO check if this is required

    // set payment type information
    PaymentTypeInformationSCT1 pmTpInf = factory.createPaymentTypeInformationSCT1();
    ServiceLevelSEPA svcLvl = factory.createServiceLevelSEPA();
    svcLvl.setCd("SEPA");
    pmTpInf.setSvcLvl(svcLvl);
    pmtInf.setPmtTpInf(pmTpInf);

    // set debitor
    PartyIdentificationSEPA2 dbtr = factory.createPartyIdentificationSEPA2();
    dbtr.setNm(debitor); // other fields should not be used
    pmtInf.setDbtr(dbtr);

    // set debitor iban
    CashAccountSEPA1 dbtrAcct = factory.createCashAccountSEPA1();
    AccountIdentificationSEPA id = factory.createAccountIdentificationSEPA();
    dbtrAcct.setId(id);
    id.setIBAN("DE87200500001234567890"); // dummy iban, is replaced by external program afterwards
    pmtInf.setDbtrAcct(dbtrAcct);

    // set debitor bic
    BranchAndFinancialInstitutionIdentificationSEPA3 dbtrAgt = factory.createBranchAndFinancialInstitutionIdentificationSEPA3();
    FinancialInstitutionIdentificationSEPA3 finInstnId = factory.createFinancialInstitutionIdentificationSEPA3();
    dbtrAgt.setFinInstnId(finInstnId);
    finInstnId.setBIC("BANKDEFFXXX"); // dummy bic, is replaced by external program afterwards
    pmtInf.setDbtrAgt(dbtrAgt);

    // create transaction
    CreditTransferTransactionInformationSCT cdtTrfTxInf = factory.createCreditTransferTransactionInformationSCT();
    pmtInf.getCdtTrfTxInf().add(cdtTrfTxInf);

    // set transaction id
    PaymentIdentificationSEPA pmtId = factory.createPaymentIdentificationSEPA();
    pmtId.setEndToEndId(msgID + "-1-1");
    cdtTrfTxInf.setPmtId(pmtId);

    // set amount type (currency)
    AmountTypeSEPA amt = factory.createAmountTypeSEPA();
    ActiveOrHistoricCurrencyAndAmountSEPA instdAmt = factory.createActiveOrHistoricCurrencyAndAmountSEPA();
    amt.setInstdAmt(instdAmt);
    instdAmt.setCcy(ActiveOrHistoricCurrencyCodeEUR.EUR);
    instdAmt.setValue(amount);
    cdtTrfTxInf.setAmt(amt);

    // set creditor
    PartyIdentificationSEPA2 cdtr = factory.createPartyIdentificationSEPA2();
    cdtr.setNm(invoice.getReceiver());
    cdtTrfTxInf.setCdtr(cdtr);

    // set creditor iban
    CashAccountSEPA2 cdtrAcct = factory.createCashAccountSEPA2();
    AccountIdentificationSEPA cdtrAcctId = factory.createAccountIdentificationSEPA();
    cdtrAcctId.setIBAN(invoice.getIban());
    cdtrAcct.setId(cdtrAcctId);
    cdtTrfTxInf.setCdtrAcct(cdtrAcct);

    // set creditor bic
    BranchAndFinancialInstitutionIdentificationSEPA1 cdtrAgt = factory.createBranchAndFinancialInstitutionIdentificationSEPA1();
    FinancialInstitutionIdentificationSEPA1 finInstId = factory.createFinancialInstitutionIdentificationSEPA1();
    cdtrAgt.setFinInstnId(finInstId);
    finInstId.setBIC(invoice.getBic().toUpperCase());
    cdtTrfTxInf.setCdtrAgt(cdtrAgt);

    // set remittance information (bemerkung/purpose)
    RemittanceInformationSEPA1Choice rmtInf = factory.createRemittanceInformationSEPA1Choice();
    rmtInf.setUstrd(invoice.getBemerkung());
    cdtTrfTxInf.setRmtInf(rmtInf);

    // marshaling
    try {
      Marshaller marshaller = this.jaxbContext.createMarshaller();
      marshaller.setSchema(this.painSchema);
      marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      marshaller.marshal(factory.createDocument(document), out);

      return out.toByteArray();
    } catch (JAXBException e) {
      log.error("An error occurred while marshaling the generated java transaction object. Transfer generation failed.", e);
      
      if (e.getLinkedException() != null && e.getLinkedException().getMessage() != null) {
        throw new UserException("fibu.rechnung.transferExport.error", e.getLinkedException().getMessage());
      }

      return null;
    }
  }

  public Document parse(final byte[] input)
  {
    if (this.jaxbContext == null) {
      log.error("Parsing transfer not possible, jaxb context is missing. Please check your pain.001.003.03.xsd file.");
      return null;
    }

    try {
      Unmarshaller unmarshaller = this.jaxbContext.createUnmarshaller();
      unmarshaller.setSchema(this.painSchema);
      InputStream inStream = new ByteArrayInputStream(input);
      JAXBElement<Document> result = (JAXBElement<Document>) unmarshaller.unmarshal(inStream);

      return result.getValue();
    } catch (JAXBException e) {
      log.error("An error occurred while unmarshaling the java transaction object.", e);
    }

    return null;
  }
}
