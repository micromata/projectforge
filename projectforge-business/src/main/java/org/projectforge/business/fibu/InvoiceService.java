package org.projectforge.business.fibu;

import java.io.ByteArrayOutputStream;

import org.projectforge.business.word.WordExporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by blumenstein on 08.05.17.
 */
@Service
public class InvoiceService
{
  @Autowired
  private WordExporter wordExporter;

  public ByteArrayOutputStream getInvoiceWordDocument(final RechnungDO data)
  {
    wordExporter.readWordFile();
    return null;
  }
}
