package org.projectforge.business.fibu.kost.reporting;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.util.GregorianCalendar;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.EingangsrechnungDO;
import org.projectforge.business.fibu.PaymentType;
import org.projectforge.framework.configuration.ApplicationContextProvider;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;

/**
 * Created by Stefan Niemczyk on 14.06.17.
 */
@Component
public class InvoiceTransferExport
{
  final private static String PAIN_001_003_03_XSD = "misc/pain.001.003.03.xsd";

  private Schema painSchema;
  private JAXBContext jaxbContext;

  public InvoiceTransferExport()
  {
    painSchema = null;
    jaxbContext = null;

    URL xsd = getClass().getClassLoader().getResource(PAIN_001_003_03_XSD);

    if (xsd == null) {
      // TODO
      return;
    }

    try {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      painSchema = schemaFactory.newSchema(new File(xsd.getFile()));
      jaxbContext = JAXBContext.newInstance(Document.class.getPackage().getName());
    } catch (SAXException e) {
      // TODO
      e.printStackTrace();
    } catch (JAXBException e) {
      // TODO
      e.printStackTrace();
    }
  }

  public byte[] generateTransfer(final EingangsrechnungDO invoice)
  {
    if (this.jaxbContext == null) {
      return null;
    }

    if (invoice.getGrossSum() == null || invoice.getPaymentType().equals(PaymentType.BANK_TRANSFER) == false ||
        invoice.getBic() == null || invoice.getIban() == null || invoice.getReceiver() == null || invoice.getBemerkung() == null) {
      return null;
    }

    // Generate structure
    ObjectFactory factory = new ObjectFactory();
    String msgID = "transaction-" + invoice.getPk();
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

    pmtInf.setPmtInfId(msgID + ".1");
    pmtInf.setPmtMtd(PaymentMethodSCTCode.TRF);
    pmtInf.setBtchBookg(true);
    pmtInf.setNbOfTxs("1");
    pmtInf.setCtrlSum(amount);
    pmtInf.setReqdExctnDt(new XMLGregorianCalendarImpl((GregorianCalendar) GregorianCalendar.getInstance())); // TODO check correct value
    // pmtInf.setChrgBr(ChargeBearerTypeSEPACode.SLEV); // TODO check if this is required

    PaymentTypeInformationSCT1 pmTpInf = factory.createPaymentTypeInformationSCT1();
    ServiceLevelSEPA svcLvl = factory.createServiceLevelSEPA();
    svcLvl.setCd("SEPA");
    pmTpInf.setSvcLvl(svcLvl);
    pmtInf.setPmtTpInf(pmTpInf);

    PartyIdentificationSEPA2 dbtr = factory.createPartyIdentificationSEPA2();
    dbtr.setNm(debitor); // other fields should not be used
    pmtInf.setDbtr(dbtr);

    CashAccountSEPA1 dbtrAcct = factory.createCashAccountSEPA1();
    AccountIdentificationSEPA id = factory.createAccountIdentificationSEPA();
    dbtrAcct.setId(id);
    id.setIBAN("DE87200500001234567890"); // dummy iban, is swapped by program afterwards
    pmtInf.setDbtrAcct(dbtrAcct);

    BranchAndFinancialInstitutionIdentificationSEPA3 dbtrAgt = factory.createBranchAndFinancialInstitutionIdentificationSEPA3();
    FinancialInstitutionIdentificationSEPA3 finInstnId = factory.createFinancialInstitutionIdentificationSEPA3();
    dbtrAgt.setFinInstnId(finInstnId);
    finInstnId.setBIC("BANKDEFFXXX"); // dummy bic, is swapped by program afterwards
    pmtInf.setDbtrAgt(dbtrAgt);

    // create transaction
    CreditTransferTransactionInformationSCT cdtTrfTxInf = factory.createCreditTransferTransactionInformationSCT();
    pmtInf.getCdtTrfTxInf().add(cdtTrfTxInf);

    PaymentIdentificationSEPA pmtId = factory.createPaymentIdentificationSEPA();
    pmtId.setEndToEndId(msgID + ".1.1");
    cdtTrfTxInf.setPmtId(pmtId);

    AmountTypeSEPA amt = factory.createAmountTypeSEPA();
    ActiveOrHistoricCurrencyAndAmountSEPA instdAmt = factory.createActiveOrHistoricCurrencyAndAmountSEPA();
    amt.setInstdAmt(instdAmt);
    instdAmt.setCcy(ActiveOrHistoricCurrencyCodeEUR.EUR);
    instdAmt.setValue(amount);
    cdtTrfTxInf.setAmt(amt);

    BranchAndFinancialInstitutionIdentificationSEPA1 cdtrAgt = factory.createBranchAndFinancialInstitutionIdentificationSEPA1();
    FinancialInstitutionIdentificationSEPA1 finInstId = factory.createFinancialInstitutionIdentificationSEPA1();
    cdtrAgt.setFinInstnId(finInstId);
    finInstId.setBIC(invoice.getBic());
    cdtTrfTxInf.setCdtrAgt(cdtrAgt);

    PartyIdentificationSEPA2 cdtr = factory.createPartyIdentificationSEPA2();
    cdtr.setNm(invoice.getReceiver());
    cdtTrfTxInf.setCdtr(cdtr);

    CashAccountSEPA2 cdtrAcct = factory.createCashAccountSEPA2();
    AccountIdentificationSEPA cdtrAcctId = factory.createAccountIdentificationSEPA();
    cdtrAcctId.setIBAN(invoice.getIban());
    cdtrAcct.setId(cdtrAcctId);
    cdtTrfTxInf.setCdtrAcct(cdtrAcct);

    RemittanceInformationSEPA1Choice rmtInf = factory.createRemittanceInformationSEPA1Choice();
    rmtInf.setUstrd(invoice.getBemerkung());
    cdtTrfTxInf.setRmtInf(rmtInf);

    // marshalling
    try {
      Marshaller marshaller = this.jaxbContext.createMarshaller();
      marshaller.setSchema(this.painSchema);
      marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      marshaller.marshal(factory.createDocument(document), out);

      return out.toByteArray();
    } catch (JAXBException e) {
      // TODO
      e.printStackTrace();
    }

    return null;
  }
}
