package org.projectforge.business.fibu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.projectforge.business.configuration.ConfigurationService;
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
      map.put("{Rechnungsadresse}", data.getCustomerAddress());
      map.put("{Typ}", data.getTyp().toString());
      map.put("{Kundenreferenz}", data.getCustomerref1());
      map.put("{Kundenreferenz2}", data.getCustomerref2());
      map.put("{Auftragsnummer}", data.getPositionen().get(0).getAuftragsPosition().getAuftrag().getNummer().toString());
      map.put("{Nummer}", data.getNummer().toString());
      map.put("{Rechnungsdatum}", data.getDatum().toString());
      replace(templateDocument, map);
      //      replaceTextInDoc(templateDocument, "Rechnungsadresse", data.getCustomerAddress());
      //      replaceTextInDoc(templateDocument, "Typ", data.getTyp().toString());
      //      replaceTextInDoc(templateDocument, "Kundenreferenz", data.getCustomerref1());
      //      replaceTextInDoc(templateDocument, "Kundenreferenz2", data.getCustomerref2());
      //      replaceTextInDoc(templateDocument, "Auftragsnummer", data.getPositionen().get(0).getAuftragsPosition().getAuftrag().getNummer().toString());
      //      replaceTextInDoc(templateDocument, "Nummer", data.getNummer().toString());
      //      replaceTextInDoc(templateDocument, "Rechnungsdatum", data.getDatum().toString());
      //Skonto
      if (data.getDiscountPercent() != null) {
        String skontoText =
            "Wir bitten um Überweisung des Gesamtbetrages abzüglich " + data.getDiscountPercent().toString() + "% Skonto bis zum " + " danach bis zum " + data
                .getFaelligkeit().toString() + " ohne Abzüge und freuen uns auf eine weiterhin erfolgreiche Zusammenarbeit";
        replaceTextInDoc(templateDocument, "EndText", skontoText);
      } else {
        String faelligkeitText = "Wir bitten um Überweisung des Gesamtbetrages bis zum " + data
            .getFaelligkeit().toString() + " und freuen uns auf eine weiterhin erfolgreiche Zusammenarbeit.";
        replaceTextInDoc(templateDocument, "EndText", faelligkeitText);
      }
      //Positionen
      XWPFTableRow tmpRow = null;
      for (XWPFTable tbl : templateDocument.getTables()) {
        tmpRow = tbl.getRow(1);
      }
      for (RechnungsPositionDO pos : data.getPositionen()) {
        if (pos.getNumber() == 1) {
          generateTableText(templateDocument, pos, data);
        } else {
          templateDocument.getTables().get(0).addRow(tmpRow, pos.getNumber());
          generateTableText(templateDocument, pos, data);
        }
      }
      replaceTextInTable(templateDocument, "Zwischensumm", data.getNetSum().toString());
      replaceTextInTable(templateDocument, "MwSt", data.getNetSum().divide(new BigDecimal(100)).multiply(new BigDecimal(19)).toString());
      replaceTextInTable(templateDocument, "Gesamtbetrag",
          data.getNetSum().add(data.getNetSum().divide(new BigDecimal(100)).multiply(new BigDecimal(19))).toString());

      result = new ByteArrayOutputStream();
      templateDocument.write(result);
    } catch (IOException e) {
      log.error("Could not read invoice template", e);
    }
    return result;
  }

  public void replace(XWPFDocument document, Map<String, String> map)
  {
    List<XWPFParagraph> paragraphs = document.getParagraphs();
    for (XWPFParagraph paragraph : paragraphs) {
      if (StringUtils.isEmpty(paragraph.getText()) == false) {
        replace(paragraph, map);
      }
      if (checkForTextBox(paragraph)) {
        replaceInTextBox(paragraph, map);
      }
    }
  }

  private boolean checkForTextBox(final XWPFParagraph paragraph)
  {
    XmlObject[] textBoxObjects = paragraph.getCTP().selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' "
        + "declare namespace wps='http://schemas.microsoft.com/office/word/2010/wordprocessingShape' .//*/wps:txbx/w:txbxContent");
    return textBoxObjects.length > 0;
  }

  private void replaceInTextBox(final XWPFParagraph paragraph, final Map<String, String> map)
  {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String searchText = entry.getKey();
      //Text-Box
      XmlObject[] textBoxObjects = paragraph.getCTP().selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' "
          + "declare namespace wps='http://schemas.microsoft.com/office/word/2010/wordprocessingShape' .//*/wps:txbx/w:txbxContent");
      for (int i = 0; i < textBoxObjects.length; i++) {
        XWPFParagraph embeddedPara = null;
        try {
          XmlObject[] paraObjects = textBoxObjects[i].selectChildren(new QName("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "p"));
          for (int j = 0; j < paraObjects.length; j++) {
            XWPFParagraph textBoxParagraph = new XWPFParagraph(CTP.Factory.parse(paraObjects[j].xmlText()), paragraph.getBody());
            if (StringUtils.contains(textBoxParagraph.getText(), entry.getKey())) {
              replace(textBoxParagraph, entry.getKey(),
                  entry.getValue() != null ? entry.getValue() : "");
              textBoxParagraph.
            }
          }
        } catch (XmlException e) {
          //handle
        }
      }
    }
  }

  public void replace(XWPFDocument document, String searchText, String replacement)
  {
    List<XWPFParagraph> paragraphs = document.getParagraphs();
    for (XWPFParagraph paragraph : paragraphs) {
      if (StringUtils.isEmpty(paragraph.getText()) == false && StringUtils.contains(paragraph.getText(), searchText)) {
        replace(paragraph, searchText, replacement);
      }
    }
  }

  private void replace(XWPFParagraph paragraph, Map<String, String> map)
  {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String searchText = entry.getKey();
      if (StringUtils.isEmpty(paragraph.getText()) == false && StringUtils.contains(paragraph.getText(), searchText)) {
        replace(paragraph, searchText, entry.getValue() != null ? entry.getValue() : "");
      }
    }
  }

  public void replace(XWPFParagraph paragraph, String searchText, String replacement)
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

  private Map<Integer, XWPFRun> getPosToRuns(XWPFParagraph paragraph)
  {
    int pos = 0;
    Map<Integer, XWPFRun> map = new HashMap<Integer, XWPFRun>(10);
    for (XWPFRun run : paragraph.getRuns()) {
      String runText = run.text();
      if (runText != null) {
        for (int i = 0; i < runText.length(); i++) {
          map.put(pos + i, run);
        }
        pos += runText.length();
      }
    }
    return (map);
  }

  private void generateTableText(XWPFDocument docx, RechnungsPositionDO pos, RechnungDO data)
  {
    replaceTextInTable(docx, "id", String.valueOf(pos.getNumber()));
    replaceTextInTable(docx, String.valueOf(pos.getNumber()) + "Posnummer", String.valueOf(pos.getNumber()));
    replaceTextInTable(docx, String.valueOf(pos.getNumber()) + "Text", pos.getText());
    if (pos.getPeriodOfPerformanceType() == PeriodOfPerformanceType.OWN) {
      replaceTextInTable(docx, String.valueOf(pos.getNumber()) + "Leistungszeitraum",
          pos.getPeriodOfPerformanceBegin().toString() + "-" + pos.getPeriodOfPerformanceEnd());
    } else {
      replaceTextInTable(docx, String.valueOf(pos.getNumber()) + "Leistungszeitraum",
          data.getPeriodOfPerformanceBegin().toString() + "-" + data.getPeriodOfPerformanceEnd());
    }
    replaceTextInTable(docx, String.valueOf(pos.getNumber()) + "Menge", pos.getMenge().toString());
    replaceTextInTable(docx, String.valueOf(pos.getNumber()) + "Einzelpreis", pos.getEinzelNetto().toString());
    replaceTextInTable(docx, String.valueOf(pos.getNumber()) + "Betrag", pos.getEinzelNetto().multiply(pos.getMenge()).toString());
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

  private void replaceTextInDoc(XWPFDocument docx, String text, String replaceText)
  {
    //Zeilen
    for (XWPFParagraph p : docx.getParagraphs()) {
      String pText = p.getText();
      log.info("Found text in word document (Paragraph): " + pText);
      searchInRuns(p.getRuns(), text, replaceText);
    }
    //Text-Box
    for (XWPFParagraph p : docx.getParagraphs()) {
      XmlObject[] textBoxObjects = p.getCTP().selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' "
          + "declare namespace wps='http://schemas.microsoft.com/office/word/2010/wordprocessingShape' .//*/wps:txbx/w:txbxContent");
      for (int i = 0; i < textBoxObjects.length; i++) {
        XWPFParagraph embeddedPara = null;
        try {
          XmlObject[] paraObjects = textBoxObjects[i].selectChildren(new QName("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "p"));
          for (int j = 0; j < paraObjects.length; j++) {
            embeddedPara = new XWPFParagraph(CTP.Factory.parse(paraObjects[j].xmlText()), p.getBody());
            searchInRuns(embeddedPara.getRuns(), text, replaceText);
          }
        } catch (XmlException e) {
          //handle
        }
      }
    }
  }

  private void searchInRuns(List<XWPFRun> runs, String text, String replaceText)
  {
    if (runs != null) {
      for (XWPFRun r : runs) {
        String tmp_text = r.getText(0);
        log.info("Found text in word document (Run): " + tmp_text);
        if (tmp_text != null && tmp_text.contains("{" + text + "}")) {
          if (replaceText == null) {
            tmp_text = tmp_text.replace("{" + text + "}", "");
          } else {
            tmp_text = tmp_text.replace("{" + text + "}", replaceText);
          }
          r.setText(tmp_text, 0);
        }
      }
    }
  }

  private void replaceTextInTable(XWPFDocument docx, String text, String replaceText)
  {
    //Tabellen
    for (XWPFTable tbl : docx.getTables()) {
      for (XWPFTableRow row : tbl.getRows()) {
        for (XWPFTableCell cell : row.getTableCells()) {
          if (cell.getText().contains("{" + text + "}")) {
            if (replaceText == null) {
              cell.setText(cell.getText().replace("{" + text + "}", ""));
            } else {
              cell.setText(cell.getText().replace("{" + text + "}", replaceText));
            }
          }
        }
      }
    }

  }

}
