package org.projectforge.business.fibu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.namespace.QName;

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
        invoiceTemplate = applicationContext.getResource("classpath:officeTemplates/invoiceTemplate.docx");
      }
      XWPFDocument templateDocument = readWordFile(invoiceTemplate.getInputStream());
      replaceText(templateDocument,"= Rechnungsadresse", data.getCustomerAddress());
      replaceText(templateDocument,"= Typ", data.getTyp().toString());
      replaceText(templateDocument,":=Kundenreferenz", data.getCustomerref1());
      replaceText(templateDocument,":=Kundenreferenz 2", data.getCustomerref2());
      replaceText(templateDocument,"= Auftragsnummer(n)", "");
      replaceText(templateDocument,"= Nummer", data.getNummer().toString());
      replaceText(templateDocument,"= Rechnungsdatum", data.getDatum().toString());
      replaceText(templateDocument, "=Fälligkeit", data.getFaelligkeit().toString());
      replaceText(templateDocument, "= Skonto", "");
      replaceText(templateDocument, "=Fälligkeit Skonto", "");

      result = new ByteArrayOutputStream();
      templateDocument.write(result);
    } catch (IOException e) {
      log.error("Could not read invoice template", e);
    }
    return result;
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

  private void replaceText(XWPFDocument docx, String text, String replaceText)
  {
    //Zeilen
    for (XWPFParagraph p : docx.getParagraphs()) {
        searchInRuns(p.getRuns(), text , replaceText);
    }
    //Tabellen
    for (XWPFTable tbl : docx.getTables()) {
      for (XWPFTableRow row : tbl.getRows()) {
        for (XWPFTableCell cell : row.getTableCells()) {
          for (XWPFParagraph p : cell.getParagraphs()) {
            searchInRuns(p.getRuns(), text , replaceText);
          }
        }
      }
    }
    //Text-Box
    for (XWPFParagraph p : docx.getParagraphs()) {
      XmlObject[] textBoxObjects =  p.getCTP().selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' "
          + "declare namespace wps='http://schemas.microsoft.com/office/word/2010/wordprocessingShape' .//*/wps:txbx/w:txbxContent");
      for (int i =0; i < textBoxObjects.length; i++) {
        XWPFParagraph embeddedPara = null;
        try {
          XmlObject[] paraObjects = textBoxObjects[i].selectChildren( new QName("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "p"));
          for (int j=0; j<paraObjects.length; j++) {
            embeddedPara = new XWPFParagraph(CTP.Factory.parse(paraObjects[j].xmlText()), p.getBody());
            searchInRuns(embeddedPara.getRuns(), text , replaceText);
          }
        } catch (XmlException e) {
          //handle
        }
      }
    }
  }
  
  private void searchInRuns(List<XWPFRun> runs, String text, String replaceText )
  {
    if (runs != null) {
      for (XWPFRun r : runs) {
        String tmp_text = r.getText(0);
        if (tmp_text != null && tmp_text.contains(text)) {
          tmp_text = tmp_text.replace(text, replaceText);
          r.setText(tmp_text, 0);
        }
      }
    }
  }
}
