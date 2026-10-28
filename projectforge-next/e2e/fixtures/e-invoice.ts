/**
 * A small e-invoice for the e-invoice checker spec: an XRechnung in CII syntax with two line items, built
 * here rather than exported, since a test instance configures no seller (`projectforge.einvoice.seller.*`)
 * and so cannot export one.
 */

export const E_INVOICE_NUMBER = "E2E-CHECK-4711";

export function xRechnungFile(): {
  name: string;
  mimeType: string;
  buffer: Buffer;
} {
  return {
    name: `XRechnung_${E_INVOICE_NUMBER}.xml`,
    mimeType: "application/xml",
    buffer: Buffer.from(XRECHNUNG, "utf-8"),
  };
}

function lineItem(
  id: number,
  name: string,
  quantity: string,
  price: string,
  net: string
): string {
  return `
    <ram:IncludedSupplyChainTradeLineItem>
      <ram:AssociatedDocumentLineDocument><ram:LineID>${id}</ram:LineID></ram:AssociatedDocumentLineDocument>
      <ram:SpecifiedTradeProduct><ram:Name>${name}</ram:Name></ram:SpecifiedTradeProduct>
      <ram:SpecifiedLineTradeAgreement>
        <ram:NetPriceProductTradePrice><ram:ChargeAmount>${price}</ram:ChargeAmount></ram:NetPriceProductTradePrice>
      </ram:SpecifiedLineTradeAgreement>
      <ram:SpecifiedLineTradeDelivery>
        <ram:BilledQuantity unitCode="HUR">${quantity}</ram:BilledQuantity>
      </ram:SpecifiedLineTradeDelivery>
      <ram:SpecifiedLineTradeSettlement>
        <ram:ApplicableTradeTax>
          <ram:TypeCode>VAT</ram:TypeCode>
          <ram:CategoryCode>S</ram:CategoryCode>
          <ram:RateApplicablePercent>19</ram:RateApplicablePercent>
        </ram:ApplicableTradeTax>
        <ram:SpecifiedTradeSettlementLineMonetarySummation>
          <ram:LineTotalAmount>${net}</ram:LineTotalAmount>
        </ram:SpecifiedTradeSettlementLineMonetarySummation>
      </ram:SpecifiedLineTradeSettlement>
    </ram:IncludedSupplyChainTradeLineItem>`;
}

function party(
  tag: string,
  name: string,
  street: string,
  zip: string,
  city: string,
  vatId: string
): string {
  return `
      <ram:${tag}>
        <ram:Name>${name}</ram:Name>
        <ram:PostalTradeAddress>
          <ram:PostcodeCode>${zip}</ram:PostcodeCode>
          <ram:LineOne>${street}</ram:LineOne>
          <ram:CityName>${city}</ram:CityName>
          <ram:CountryID>DE</ram:CountryID>
        </ram:PostalTradeAddress>
        <ram:SpecifiedTaxRegistration><ram:ID schemeID="VA">${vatId}</ram:ID></ram:SpecifiedTaxRegistration>
      </ram:${tag}>`;
}

const XRECHNUNG = `<?xml version="1.0" encoding="UTF-8"?>
<rsm:CrossIndustryInvoice xmlns:rsm="urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100"
    xmlns:ram="urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100"
    xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100">
  <rsm:ExchangedDocumentContext>
    <ram:GuidelineSpecifiedDocumentContextParameter>
      <ram:ID>urn:cen.eu:en16931:2017#compliant#urn:xeinkauf.de:kosit:xrechnung_3.0</ram:ID>
    </ram:GuidelineSpecifiedDocumentContextParameter>
  </rsm:ExchangedDocumentContext>
  <rsm:ExchangedDocument>
    <ram:ID>${E_INVOICE_NUMBER}</ram:ID>
    <ram:TypeCode>380</ram:TypeCode>
    <ram:IssueDateTime><udt:DateTimeString format="102">20240615</udt:DateTimeString></ram:IssueDateTime>
  </rsm:ExchangedDocument>
  <rsm:SupplyChainTradeTransaction>${lineItem(1, "Softwareentwicklung", "10", "150.00", "1500.00")}${lineItem(2, "Projektmanagement", "5", "120.00", "600.00")}
    <ram:ApplicableHeaderTradeAgreement>
      <ram:BuyerReference>04011000-1234512345-12</ram:BuyerReference>${party("SellerTradeParty", "E2E Verkauf GmbH", "Marktplatz 1", "34117", "Kassel", "DE123456789")}${party("BuyerTradeParty", "E2E Kundin GmbH", "Musterstraße 42", "12345", "Berlin", "DE987654321")}
    </ram:ApplicableHeaderTradeAgreement>
    <ram:ApplicableHeaderTradeDelivery>
      <ram:ActualDeliverySupplyChainEvent>
        <ram:OccurrenceDateTime><udt:DateTimeString format="102">20240531</udt:DateTimeString></ram:OccurrenceDateTime>
      </ram:ActualDeliverySupplyChainEvent>
    </ram:ApplicableHeaderTradeDelivery>
    <ram:ApplicableHeaderTradeSettlement>
      <ram:InvoiceCurrencyCode>EUR</ram:InvoiceCurrencyCode>
      <ram:SpecifiedTradeSettlementPaymentMeans>
        <ram:TypeCode>58</ram:TypeCode>
        <ram:PayeePartyCreditorFinancialAccount><ram:IBANID>DE89370400440532013000</ram:IBANID></ram:PayeePartyCreditorFinancialAccount>
      </ram:SpecifiedTradeSettlementPaymentMeans>
      <ram:ApplicableTradeTax>
        <ram:CalculatedAmount>399.00</ram:CalculatedAmount>
        <ram:TypeCode>VAT</ram:TypeCode>
        <ram:BasisAmount>2100.00</ram:BasisAmount>
        <ram:CategoryCode>S</ram:CategoryCode>
        <ram:RateApplicablePercent>19</ram:RateApplicablePercent>
      </ram:ApplicableTradeTax>
      <ram:SpecifiedTradePaymentTerms>
        <ram:DueDateDateTime><udt:DateTimeString format="102">20240715</udt:DateTimeString></ram:DueDateDateTime>
      </ram:SpecifiedTradePaymentTerms>
      <ram:SpecifiedTradeSettlementHeaderMonetarySummation>
        <ram:LineTotalAmount>2100.00</ram:LineTotalAmount>
        <ram:TaxBasisTotalAmount>2100.00</ram:TaxBasisTotalAmount>
        <ram:TaxTotalAmount currencyID="EUR">399.00</ram:TaxTotalAmount>
        <ram:GrandTotalAmount>2499.00</ram:GrandTotalAmount>
        <ram:DuePayableAmount>2499.00</ram:DuePayableAmount>
      </ram:SpecifiedTradeSettlementHeaderMonetarySummation>
    </ram:ApplicableHeaderTradeSettlement>
  </rsm:SupplyChainTradeTransaction>
</rsm:CrossIndustryInvoice>
`;
