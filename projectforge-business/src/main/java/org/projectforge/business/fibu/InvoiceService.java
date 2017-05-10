package org.projectforge.business.fibu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
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
}
