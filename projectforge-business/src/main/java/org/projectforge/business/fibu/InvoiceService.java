/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.utils.ReplaceUtils;
import de.micromata.merlin.word.RunsProcessor;
import de.micromata.merlin.word.WordDocument;
import de.micromata.merlin.word.templating.Variables;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.configuration.DomainService;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.session.UserAgentBrowser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * Created by blumenstein on 08.05.17.
 * Migrated by Kai Reinhard
 */
@Service
public class InvoiceService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InvoiceService.class);

  private static final int FILENAME_MAXLENGTH = 100; // Higher values result in filename issues in Safari 13-

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private ConfigurationService configurationService;

  @Autowired
  private DomainService domainService;

  @Value("${projectforge.invoiceTemplate}")
  private String customInvoiceTemplateName;

  public ByteArrayOutputStream getInvoiceWordDocument(final RechnungDO data) {
    ByteArrayOutputStream result = null;
    try {
      Resource invoiceTemplate = null;
      boolean isSkonto = data.getDiscountMaturity() != null && data.getDiscountPercent() != null && data.getDiscountZahlungsZielInTagen() != null;
      if (!customInvoiceTemplateName.isEmpty()) {
        String resourceDir = configurationService.getResourceDir();
        invoiceTemplate = applicationContext
                .getResource("file://" + resourceDir + "/officeTemplates/" + customInvoiceTemplateName + ".docx");
      }
      if (invoiceTemplate == null || !invoiceTemplate.exists()) {
        invoiceTemplate = applicationContext.getResource("classpath:officeTemplates/InvoiceTemplate" + ".docx");
      }

      Variables variables = new Variables();
      variables.put("Rechnungsadresse", data.getCustomerAddress());
      variables.put("Typ", data.getTyp() != null ? I18nHelper.getLocalizedMessage(data.getTyp().getI18nKey()) : "");
      variables.put("Kundenreferenz", data.getCustomerref1());
      variables.put("Auftragsnummer", data.getPositionen().stream()
              .filter(pos -> pos.getAuftragsPosition() != null && pos.getAuftragsPosition().getAuftrag() != null)
              .map(pos -> String.valueOf(pos.getAuftragsPosition().getAuftrag().getNummer()))
              .distinct()
              .collect(Collectors.joining(", ")));
      variables.put("VORNAME_NACHNAME", ThreadLocalUserContext.getUser() != null && ThreadLocalUserContext.getUser().getFullname() != null ?
              ThreadLocalUserContext.getUser().getFullname().toUpperCase() :
              "");
      variables.put("Rechnungsnummer", data.getNummer() != null ? data.getNummer().toString() : "");
      variables.put("Rechnungsdatum", DateTimeFormatter.instance().getFormattedDate(data.getDatum()));
      variables.put("Faelligkeit", DateTimeFormatter.instance().getFormattedDate(data.getFaelligkeit()));
      variables.put("Anlage", getReplacementForAttachment(data));
      variables.put("isSkonto", isSkonto);
      if (isSkonto) {
        variables.put("Skonto", formatBigDecimal(data.getDiscountPercent().stripTrailingZeros()) + "%");
        variables.put("Faelligkeit_Skonto", DateTimeFormatter.instance().getFormattedDate(data.getDiscountMaturity()));
      }
      variables.put("Zwischensumme", formatCurrencyAmount(data.getNetSum()));
      variables.put("MwSt", formatCurrencyAmount(data.getVatAmountSum()));
      variables.put("Gesamtbetrag", formatCurrencyAmount(data.getGrossSum()));

      WordDocument document = new WordDocument(invoiceTemplate.getInputStream(), invoiceTemplate.getFile().getName());

      document.process(variables);
      generatePosTableRows(document.getDocument(), data);

      return document.getAsByteArrayOutputStream();
    } catch (IOException e) {
      log.error("Could not read invoice template", e);
      return null;
    }
  }

  private String getReplacementForAttachment(final RechnungDO data) {
    if (!StringUtils.isEmpty(data.getAttachment())) {
      return I18nHelper.getLocalizedMessage("fibu.attachment") + ":\r\n" + data.getAttachment();
    } else {
      return "";
    }
  }

  private String formatCurrencyAmount(final BigDecimal value) {
    return formatBigDecimal(value.setScale(2, RoundingMode.HALF_UP));
  }

  private String formatBigDecimal(final BigDecimal value) {
    if (value == null) {
      return "";
    }
    DecimalFormat df;
    if (value.scale() == 0) {
      df = new DecimalFormat("#,###");
    } else if (value.scale() == 1) {
      df = new DecimalFormat("#,###.0");
    } else if (value.scale() == 2) {
      df = new DecimalFormat("#,###.00");
    } else {
      df = new DecimalFormat("#,###.#");
    }
    return df.format(value);
  }

  private String getPeriodOfPerformance(final RechnungsPositionDO position, final RechnungDO invoice) {
    final LocalDate begin;
    final LocalDate end;

    begin = position.getPeriodOfPerformanceBegin();
    end = position.getPeriodOfPerformanceEnd();

    return DateTimeFormatter.instance().getFormattedDate(begin) + " - " + DateTimeFormatter.instance().getFormattedDate(end);
  }

  private XWPFTable generatePosTableRows(final XWPFDocument templateDocument, final RechnungDO invoice) {
    XWPFTable posTbl = null;
    for (XWPFTable tbl : templateDocument.getTables()) {
      if (tbl.getRow(0).getCell(0).getText().contains("Beschreibung")) {
        posTbl = tbl;
      }
    }
    int rowCounter = 2;
    for (RechnungsPositionDO position : invoice.getPositionen()) {
      createInvoicePositionRow(posTbl, rowCounter++, invoice, position);
    }
    posTbl.removeRow(1);
    return posTbl;
  }

  private void createInvoicePositionRow(final XWPFTable posTbl, final int rowCounter, final RechnungDO invoice, final RechnungsPositionDO position) {
    try {
      XWPFTableRow sourceRow = posTbl.getRow(1);
      CTRow ctrow = CTRow.Factory.parse(sourceRow.getCtRow().newInputStream());
      XWPFTableRow newRow = new XWPFTableRow(ctrow, posTbl);
      Variables variables = new Variables();
      variables.put("id", String.valueOf(position.getNumber()));
      variables.put("Posnummer", String.valueOf(position.getNumber()));
      variables.put("Text", position.getText());
      variables.put("Leistungszeitraum", getPeriodOfPerformance(position, invoice));
      variables.put("Menge", formatCurrencyAmount(position.getMenge()));
      variables.put("Einzelpreis", formatCurrencyAmount(position.getEinzelNetto()));
      variables.put("Betrag", formatCurrencyAmount(position.getNetSum()));
      for (XWPFTableCell cell : newRow.getTableCells()) {
        for (XWPFParagraph cellParagraph : cell.getParagraphs()) {
          new RunsProcessor(cellParagraph).replace(variables);
        }
      }
      posTbl.addRow(newRow, rowCounter);
    } catch (IOException | XmlException ex) {
      log.error("Error while trying to copy row: " + ex.getMessage(), ex);
    }
  }

  public String getInvoiceFilename(RechnungDO invoice, final UserAgentBrowser browser) {
    final String suffix = ".docx";
    if (invoice == null) {
      return suffix;
    }
    //Rechnungsnummer_Kunde_Projekt_Betreff(mit Unterstrichen statt Leerzeichen)_Datum(2017-07-04)
    final String number = invoice.getNummer() != null ? invoice.getNummer().toString() : "";
    String customer = invoice.getKunde() != null ? "_" + invoice.getKunde().getName() : "";
    if (StringUtils.isEmpty(customer)) {
      customer = invoice.getKundeText() != null ? "_" + invoice.getKundeText() : "";
    }
    final String project = invoice.getProjekt() != null ? "_" + invoice.getProjekt().getName() : "";
    final String subject = invoice.getBetreff() != null ? "_" + invoice.getBetreff() : "";
    final String invoiceDate = "_" + DateTimeFormatter.instance().getFormattedDate(invoice.getDatum());
    String filename = StringUtils.abbreviate(
            ReplaceUtils.encodeFilename(number + customer + project + subject + invoiceDate, true),
            "...", FILENAME_MAXLENGTH) + suffix;
    return filename;
  }
}
