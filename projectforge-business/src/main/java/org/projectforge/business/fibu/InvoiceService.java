package org.projectforge.business.fibu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
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

  @Autowired
  private ConfigurationService configurationService;

  @Autowired
  private ApplicationContext applicationContext;

  @Value("${projectforge.invoiceTemplate}")
  private String customInvoiceTemplateName;

  public ByteArrayOutputStream getInvoiceWordDocument(final RechnungDO data)
  {
    ByteArrayOutputStream result = null;
    try {
      Resource invoiceTemplate = null;
      if (customInvoiceTemplateName.isEmpty() == false) {
        String resourceDir = configurationService.getResourceDir();
        invoiceTemplate = applicationContext.getResource("file://" + resourceDir + "/officeTemplates/" + customInvoiceTemplateName);
      }
      if (invoiceTemplate == null || invoiceTemplate.exists() == false) {
        invoiceTemplate = applicationContext.getResource("classpath:officeTemplates/InvoiceTemplate.docx");
      }
      XWPFDocument templateDocument = readWordFile(invoiceTemplate.getInputStream());
      Map<String, String> map = new HashMap<>();
      map.put("Rechnungsadresse", data.getCustomerAddress());
      map.put("Typ", data.getTyp().toString());
      map.put("Kundenreferenz", data.getCustomerref1());
      map.put("Kundenreferenz2", data.getCustomerref2());
      map.put("Auftragsnummer", data.getPositionen().get(0).getAuftragsPosition().getAuftrag().getNummer().toString());
      map.put("Vorname_Nachname", ThreadLocalUserContext.getUser().getFullname());
      map.put("Rechnungsnummer", data.getNummer().toString());
      map.put("Rechnungsdatum", data.getDatum().toString());
      replaceInWholeDocument(templateDocument, map);

      replaceInPosTable(templateDocument, data.getPositionen());

      //Skonto
      //      if (data.getDiscountPercent() != null) {
      //        String skontoText =
      //            "Wir bitten um Überweisung des Gesamtbetrages abzüglich " + data.getDiscountPercent().toString() + "% Skonto bis zum " + " danach bis zum " + data
      //                .getFaelligkeit().toString() + " ohne Abzüge und freuen uns auf eine weiterhin erfolgreiche Zusammenarbeit";
      //        replaceTextInDoc(templateDocument, "EndText", skontoText);
      //      } else {
      //        String faelligkeitText = "Wir bitten um Überweisung des Gesamtbetrages bis zum " + data
      //            .getFaelligkeit().toString() + " und freuen uns auf eine weiterhin erfolgreiche Zusammenarbeit.";
      //        replaceTextInDoc(templateDocument, "EndText", faelligkeitText);
      //      }
      //      //Positionen
      //      XWPFTableRow tmpRow = null;
      //      for (XWPFTable tbl : templateDocument.getTables()) {
      //        tmpRow = tbl.getRow(1);
      //      }
      //      for (RechnungsPositionDO pos : data.getPositionen()) {
      //        if (pos.getNumber() == 1) {
      //          generateTableText(templateDocument, pos, data);
      //        } else {
      //          templateDocument.getTables().get(0).addRow(tmpRow, pos.getNumber());
      //          generateTableText(templateDocument, pos, data);
      //        }
      //      }
      //      replaceTextInTable(templateDocument, "Zwischensumm", data.getNetSum().toString());
      //      replaceTextInTable(templateDocument, "MwSt", data.getNetSum().divide(new BigDecimal(100)).multiply(new BigDecimal(19)).toString());
      //      replaceTextInTable(templateDocument, "Gesamtbetrag",
      //          data.getNetSum().add(data.getNetSum().divide(new BigDecimal(100)).multiply(new BigDecimal(19))).toString());

      result = new ByteArrayOutputStream();
      templateDocument.write(result);
    } catch (IOException e) {
      log.error("Could not read invoice template", e);
    }
    return result;
  }

  public void replaceInWholeDocument(XWPFDocument document, Map<String, String> map)
  {
    List<XWPFParagraph> paragraphs = document.getParagraphs();
    for (XWPFParagraph paragraph : paragraphs) {
      if (StringUtils.isEmpty(paragraph.getText()) == false) {
        replaceInParagraph(paragraph, map);
      }
      replaceInTable(document, map);
    }
  }

  public void replaceInWholeDocument(XWPFDocument document, String searchText, String replacement)
  {
    List<XWPFParagraph> paragraphs = document.getParagraphs();
    for (XWPFParagraph paragraph : paragraphs) {
      if (StringUtils.isEmpty(paragraph.getText()) == false && StringUtils.contains(paragraph.getText(), searchText)) {
        replaceInParagraph(paragraph, "{" + searchText + "}", replacement);
      }
      replaceInTable(document, "{" + searchText + "}", replacement);
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

  public void replaceInParagraph(XWPFParagraph paragraph, String searchText, String replacement)
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
        if (replacement.contains("\r\n")) {
          replacement = replacement.replace("\r", "");
        }
        run.setText(replacement, 0);
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

  private void replaceInPosTable(final XWPFDocument templateDocument, final List<RechnungsPositionDO> positionen)
  {
    generatePosTableRows(templateDocument, positionen);
  }

  private void generatePosTableRows(final XWPFDocument templateDocument, final List<RechnungsPositionDO> positionen)
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
      XWPFDocument docx = new XWPFDocument(is);
      return docx;
    } catch (IOException e) {
      log.error("Exception while reading docx file.", e);
    }
    return null;
  }

}
