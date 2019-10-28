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

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.configuration.DomainService;
import org.projectforge.common.ReplaceUtils;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.session.UserAgentBrowser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by blumenstein on 08.05.17.
 * <p>
 * Copy some code from https://stackoverflow.com/questions/22268898/replacing-a-text-in-apache-poi-xwpf
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
                .getResource("file://" + resourceDir + "/officeTemplates/" + customInvoiceTemplateName + (isSkonto ? "_Skonto" : "") + ".docx");
      }
      if (invoiceTemplate == null || !invoiceTemplate.exists()) {
        invoiceTemplate = applicationContext.getResource("classpath:officeTemplates/InvoiceTemplate" + (isSkonto ? "_Skonto" : "") + ".docx");
      }

      Map<String, String> replacementMap = new HashMap<>();
      replacementMap.put("Rechnungsadresse", data.getCustomerAddress());
      replacementMap.put("Typ", data.getTyp() != null ? I18nHelper.getLocalizedMessage(data.getTyp().getI18nKey()) : "");
      replacementMap.put("Kundenreferenz", data.getCustomerref1());
      replacementMap.put("Auftragsnummer", data.getPositionen().stream()
              .filter(pos -> pos.getAuftragsPosition() != null && pos.getAuftragsPosition().getAuftrag() != null)
              .map(pos -> String.valueOf(pos.getAuftragsPosition().getAuftrag().getNummer()))
              .distinct()
              .collect(Collectors.joining(", ")));
      replacementMap.put("VORNAME_NACHNAME", ThreadLocalUserContext.getUser() != null && ThreadLocalUserContext.getUser().getFullname() != null ?
              ThreadLocalUserContext.getUser().getFullname().toUpperCase() :
              "");
      replacementMap.put("Rechnungsnummer", data.getNummer() != null ? data.getNummer().toString() : "");
      replacementMap.put("Rechnungsdatum", DateTimeFormatter.instance().getFormattedDate(data.getDatum()));
      replacementMap.put("Faelligkeit", DateTimeFormatter.instance().getFormattedDate(data.getFaelligkeit()));
      replacementMap.put("Anlage", getReplacementForAttachment(data));
      if (isSkonto) {
        replacementMap.put("Skonto", formatBigDecimal(data.getDiscountPercent()) + " %");
        replacementMap.put("Faelligkeit_Skonto", DateTimeFormatter.instance().getFormattedDate(data.getDiscountMaturity()));
      }

      XWPFDocument templateDocument = readWordFile(invoiceTemplate.getInputStream());

      replaceInWholeDocument(templateDocument, replacementMap);

      replaceInTable(templateDocument, replacementMap);

      replaceInPosTable(templateDocument, data);

      result = new ByteArrayOutputStream();
      templateDocument.write(result);
    } catch (IOException e) {
      log.error("Could not read invoice template", e);
    }
    return result;
  }

  private String getReplacementForAttachment(final RechnungDO data) {
    if (!StringUtils.isEmpty(data.getAttachment())) {
      return I18nHelper.getLocalizedMessage("fibu.attachment") + ":\r\n" + data.getAttachment();
    } else {
      return "";
    }
  }

  private void replaceInWholeDocument(XWPFDocument document, Map<String, String> map) {
    List<XWPFParagraph> paragraphs = document.getParagraphs();
    for (XWPFParagraph paragraph : paragraphs) {
      if (!StringUtils.isEmpty(paragraph.getText())) {
        replaceInParagraph(paragraph, map);
      }
    }
  }

  private void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> map) {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String searchText = "{" + entry.getKey() + "}";
      if (!StringUtils.isEmpty(paragraph.getText()) && StringUtils.contains(paragraph.getText(), searchText)) {
        replaceInParagraph(paragraph, searchText, entry.getValue() != null ? entry.getValue() : "");
      }
    }
  }

  private void replaceInParagraph(XWPFParagraph paragraph, String searchText, String replacement) {
    boolean found = true;
    while (found) {
      found = false;
      int pos = paragraph.getText().indexOf(searchText);
      if (pos >= 0) {
        found = true;
        Map<Integer, XWPFRun> posToRuns = getPosToRuns(paragraph);
        XWPFRun run = posToRuns.get(pos);
        XWPFRun lastRun = posToRuns.get(pos + searchText.length() - 1);
        int runNum = paragraph.getRuns().indexOf(run);
        int lastRunNum = paragraph.getRuns().indexOf(lastRun);
        int runCount = paragraph.getRuns().size();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= runCount; i++) {
          if (i >= runNum && i <= lastRunNum) {
            sb.append(paragraph.getRuns().get(i).getText(0));
          }
        }
        String newText = sb.toString().replace(searchText, replacement);
        if (replacement.contains("\n")) {
          //Do the carriage return stuff
          replacement = replacement.replace("\r", "");
          String[] replacementLines = replacement.split("\n");
          // For each additional line, create a new run. Add carriage return on previous.
          for (int i = 0; i < replacementLines.length; i++) {
            // For every run except last one, add a carriage return.
            String textForLine = replacementLines[i];
            if (i == 0) {
              run.setText(textForLine, 0);
              run.addCarriageReturn();
            } else {
              paragraph.insertNewRun(runNum + 1);
              XWPFRun newRun = paragraph.getRuns().get(runNum + 1);
              CTRPr rPr = newRun.getCTR().isSetRPr() ? newRun.getCTR().getRPr() : newRun.getCTR().addNewRPr();
              rPr.set(run.getCTR().getRPr());
              newRun.setText(textForLine);
              //If last line, no cr
              if (i < (replacementLines.length - 1)) {
                newRun.addCarriageReturn();
              }
              runNum++;
              lastRunNum++;
            }
          }
        } else {
          //Do replace staff without carriage return
          run.setText(newText, 0);
        }
        for (int i = lastRunNum; i > runNum; i--) {
          paragraph.removeRun(i);
        }
      }
    }
  }

  private void replaceInTable(XWPFDocument document, Map<String, String> map) {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String searchText = "{" + entry.getKey() + "}";
      replaceInTable(document, searchText, entry.getValue() != null ? entry.getValue() : "");
    }
  }

  private void replaceInTable(XWPFDocument document, String searchText, String replacement) {
    for (XWPFTable tbl : document.getTables()) {
      for (XWPFTableRow row : tbl.getRows()) {
        for (XWPFTableCell cell : row.getTableCells()) {
          for (XWPFParagraph paragraph : cell.getParagraphs()) {
            replaceInParagraph(paragraph, searchText, replacement);
          }
        }
      }
    }
  }

  private Map<Integer, XWPFRun> getPosToRuns(XWPFParagraph paragraph) {
    int pos = 0;
    Map<Integer, XWPFRun> map = new HashMap<>(10);
    for (XWPFRun run : paragraph.getRuns()) {
      String runText = run.text();
      if (runText != null) {
        for (int i = 0; i < runText.length(); i++) {
          map.put(pos + i, run);
        }
        pos += runText.length();
      }
    }
    return map;
  }

  private void replaceInPosTable(final XWPFDocument templateDocument, final RechnungDO invoice) {
    XWPFTable posTbl = generatePosTableRows(templateDocument, invoice.getPositionen());
    replacePosDataInTable(posTbl, invoice);
    replaceSumDataInTable(posTbl, invoice);
  }

  private void replaceSumDataInTable(final XWPFTable posTbl, final RechnungDO invoice) {
    Map<String, String> map = new HashMap<>();
    map.put("Zwischensumme", formatBigDecimal(invoice.getNetSum()));
    map.put("MwSt", formatBigDecimal(invoice.getVatAmountSum()));
    map.put("Gesamtbetrag", formatBigDecimal(invoice.getGrossSum()));
    int tableRowSize = posTbl.getRows().size();
    for (int startSumRow = tableRowSize - 2; startSumRow < tableRowSize; startSumRow++) {
      for (XWPFTableCell cell : posTbl.getRow(startSumRow).getTableCells()) {
        for (XWPFParagraph cellParagraph : cell.getParagraphs()) {
          replaceInParagraph(cellParagraph, map);
        }
      }
    }
  }

  private String formatBigDecimal(final BigDecimal value) {
    if (value == null) {
      return "";
    }
    DecimalFormat df = new DecimalFormat("#,###.00");
    return df.format(value.setScale(2, BigDecimal.ROUND_HALF_DOWN));
  }

  private void replacePosDataInTable(final XWPFTable posTbl, final RechnungDO invoice) {
    int rowCount = 1;
    for (RechnungsPositionDO position : invoice.getPositionen()) {
      String identifier = "{" + position.getNumber() + "}";
      Map<String, String> map = new HashMap<>();
      map.put(identifier + "Posnummer", String.valueOf(position.getNumber()));
      map.put(identifier + "Text", position.getText());
      map.put(identifier + "Leistungszeitraum", getPeriodOfPerformance(position, invoice));
      map.put(identifier + "Menge", formatBigDecimal(position.getMenge()));
      map.put(identifier + "Einzelpreis", formatBigDecimal(position.getEinzelNetto()));
      map.put(identifier + "Betrag", formatBigDecimal(position.getNetSum()));
      for (XWPFTableCell cell : posTbl.getRow(rowCount).getTableCells()) {
        for (XWPFParagraph cellParagraph : cell.getParagraphs()) {
          replaceInParagraph(cellParagraph, map);
        }
      }
      rowCount++;
    }
  }

  private String getPeriodOfPerformance(final RechnungsPositionDO position, final RechnungDO invoice) {
    final Date begin;
    final Date end;

    if (position.getPeriodOfPerformanceType() == PeriodOfPerformanceType.OWN) {
      begin = position.getPeriodOfPerformanceBegin();
      end = position.getPeriodOfPerformanceEnd();
    } else {
      begin = invoice.getPeriodOfPerformanceBegin();
      end = invoice.getPeriodOfPerformanceEnd();
    }

    return DateTimeFormatter.instance().getFormattedDate(begin) + " - " + DateTimeFormatter.instance().getFormattedDate(end);
  }

  private XWPFTable generatePosTableRows(final XWPFDocument templateDocument, final List<RechnungsPositionDO> positionen) {
    XWPFTable posTbl = null;
    for (XWPFTable tbl : templateDocument.getTables()) {
      if (tbl.getRow(0).getCell(0).getText().contains("Beschreibung")) {
        posTbl = tbl;
      }
    }
    for (int i = 2; i <= positionen.size(); i++) {
      copyTableRow(posTbl, i);
    }
    int rowCount = 1;
    for (RechnungsPositionDO position : positionen) {
      for (XWPFTableCell cell : posTbl.getRow(rowCount).getTableCells()) {
        for (XWPFParagraph cellParagraph : cell.getParagraphs()) {
          replaceInParagraph(cellParagraph, "id", String.valueOf(position.getNumber()));
        }
      }
      rowCount++;
    }
    return posTbl;
  }

  private void copyTableRow(final XWPFTable posTbl, final int rowCounter) {
    XWPFTableRow rowToCopy = posTbl.getRow(1);
    CTRow row = posTbl.getCTTbl().insertNewTr(rowCounter);
    row.set(rowToCopy.getCtRow());
    XWPFTableRow copyRow = new XWPFTableRow(row, posTbl);
    posTbl.getRows().add(rowCounter, copyRow);
  }

  private XWPFDocument readWordFile(InputStream is) {
    try {
      return new XWPFDocument(is);
    } catch (IOException e) {
      log.error("Exception while reading docx file.", e);
    }
    return null;
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
            ReplaceUtils.INSTANCE.encodeFilename(number + customer + project + subject + invoiceDate, true),
            "...", FILENAME_MAXLENGTH) + suffix;
    return filename;
  }
}
