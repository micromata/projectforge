package org.projectforge.business.fibu;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.session.UserAgentBrowser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Created by blumenstein on 08.05.17.
 * <p>
 * Copy some code from https://stackoverflow.com/questions/22268898/replacing-a-text-in-apache-poi-xwpf
 */
@Service
public class InvoiceService
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(InvoiceService.class);

  private static final int CONTEXTPATH_LENGTH = 50;

  private static final int DOWNLOAD_MAXLENGTH_SAFARI = 170;

  private static final int DOWNLOAD_MAXLENGTH_OTHER = 255;

  @Autowired
  private ConfigurationService configurationService;

  @Autowired
  private ApplicationContext applicationContext;

  @Value("${projectforge.domain}")
  private String domain;

  @Value("${projectforge.invoiceTemplate}")
  private String customInvoiceTemplateName;

  public ByteArrayOutputStream getInvoiceWordDocument(final RechnungDO data)
  {
    ByteArrayOutputStream result = null;
    try {
      Resource invoiceTemplate = null;
      boolean isSkonto = data.getDiscountMaturity() != null && data.getDiscountPercent() != null && data.getDiscountZahlungsZielInTagen() != null;
      if (customInvoiceTemplateName.isEmpty() == false) {
        String resourceDir = configurationService.getResourceDir();
        invoiceTemplate = applicationContext
            .getResource("file://" + resourceDir + "/officeTemplates/" + customInvoiceTemplateName + (isSkonto ? "_Skonto" : "") + ".docx");
      }
      if (invoiceTemplate == null || invoiceTemplate.exists() == false) {
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

  private String getReplacementForAttachment(final RechnungDO data)
  {
    if (StringUtils.isEmpty(data.getAttachment()) == false) {
      return I18nHelper.getLocalizedMessage("fibu.attachment") + ":\r\n" + data.getAttachment();
    } else {
      return "";
    }
  }

  private void replaceInWholeDocument(XWPFDocument document, Map<String, String> map)
  {
    List<XWPFParagraph> paragraphs = document.getParagraphs();
    for (XWPFParagraph paragraph : paragraphs) {
      if (StringUtils.isEmpty(paragraph.getText()) == false) {
        replaceInParagraph(paragraph, map);
      }
    }
  }

  private void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> map)
  {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String searchText = "{" + entry.getKey() + "}";
      if (StringUtils.isEmpty(paragraph.getText()) == false && StringUtils.contains(paragraph.getText(), searchText)) {
        replaceInParagraph(paragraph, searchText, entry.getValue() != null ? entry.getValue() : "");
      }
    }
  }

  private void replaceInParagraph(XWPFParagraph paragraph, String searchText, String replacement)
  {
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

  private void replaceInTable(XWPFDocument document, Map<String, String> map)
  {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String searchText = "{" + entry.getKey() + "}";
      replaceInTable(document, searchText, entry.getValue() != null ? entry.getValue() : "");
    }
  }

  private void replaceInTable(XWPFDocument document, String searchText, String replacement)
  {
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

  private Map<Integer, XWPFRun> getPosToRuns(XWPFParagraph paragraph)
  {
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

  private void replaceInPosTable(final XWPFDocument templateDocument, final RechnungDO invoice)
  {
    XWPFTable posTbl = generatePosTableRows(templateDocument, invoice.getPositionen());
    replacePosDataInTable(posTbl, invoice);
    replaceSumDataInTable(posTbl, invoice);
  }

  private void replaceSumDataInTable(final XWPFTable posTbl, final RechnungDO invoice)
  {
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

  private String formatBigDecimal(final BigDecimal value)
  {
    if (value == null) {
      return "";
    }
    DecimalFormat df = new DecimalFormat("#,###.00");
    return df.format(value.setScale(2, BigDecimal.ROUND_HALF_DOWN));
  }

  private void replacePosDataInTable(final XWPFTable posTbl, final RechnungDO invoice)
  {
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

  private String getPeriodOfPerformance(final RechnungsPositionDO position, final RechnungDO invoice)
  {
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

  private XWPFTable generatePosTableRows(final XWPFDocument templateDocument, final List<RechnungsPositionDO> positionen)
  {
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

  private void copyTableRow(final XWPFTable posTbl, final int rowCounter)
  {
    XWPFTableRow rowToCopy = posTbl.getRow(1);
    CTRow row = posTbl.getCTTbl().insertNewTr(rowCounter);
    row.set(rowToCopy.getCtRow());
    XWPFTableRow copyRow = new XWPFTableRow(row, posTbl);
    posTbl.getRows().add(rowCounter, copyRow);
  }

  private XWPFDocument readWordFile(InputStream is)
  {
    try {
      return new XWPFDocument(is);
    } catch (IOException e) {
      log.error("Exception while reading docx file.", e);
    }
    return null;
  }

  public String getInvoiceFilename(RechnungDO invoice, final UserAgentBrowser browser)
  {
    int DOWNLOAD_MAX_LENGTH = DOWNLOAD_MAXLENGTH_OTHER;
    if (UserAgentBrowser.SAFARI.equals(browser)) {
      DOWNLOAD_MAX_LENGTH = DOWNLOAD_MAXLENGTH_SAFARI;
    }
    final String suffix = ".docx";
    if (invoice == null) {
      return suffix;
    }
    //Rechnungsnummer_Kunde_Projekt_Betreff(mit Unterstrichen statt Leerzeichen)_Datum(2017-07-04)
    final String number = invoice.getNummer() != null ? invoice.getNummer().toString() + "_" : "";
    String sanitizedCustomer = invoice.getKunde() != null ? invoice.getKunde().getName().replaceAll("\\W+", "_") + "_" : "";
    if (StringUtils.isEmpty(sanitizedCustomer)) {
      sanitizedCustomer = invoice.getKundeText() != null ? invoice.getKundeText().replaceAll("\\W+", "_") + "_" : "";
    }
    final String sanitizedProject = invoice.getProjekt() != null ? invoice.getProjekt().getName().replaceAll("\\W+", "_") + "_" : "";
    final String sanitizedBetreff = invoice.getBetreff() != null ? invoice.getBetreff().replaceAll("\\W+", "_") + "_" : "";
    final String invoiceDate = DateTimeFormatter.instance().getFormattedDate(invoice.getDatum()).replaceAll("\\W+", "_");
    String filename =
        number + sanitizedCustomer + sanitizedProject + sanitizedBetreff + invoiceDate;
    final int downloadCompleteLength = domain.length() + CONTEXTPATH_LENGTH + filename.length() + suffix.length();
    if (downloadCompleteLength > DOWNLOAD_MAX_LENGTH) {
      int diff = downloadCompleteLength - DOWNLOAD_MAX_LENGTH;
      String more = "[more]";
      filename = filename.substring(0, filename.length() - diff - more.length());
      filename = filename + more + suffix;
    } else if (StringUtils.isEmpty(filename)) {
      filename = suffix;
    } else {
      filename = filename + suffix;
    }
    return filename;
  }
}
