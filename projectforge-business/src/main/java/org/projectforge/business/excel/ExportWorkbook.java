/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.business.excel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class ExportWorkbook
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ExportWorkbook.class);

  private Workbook poiWorkbook;

  private List<ExportSheet> sheets;

  private ContentProvider contentProvider;

  private int numberOfCellStyles = 0;

  private int numberOfDataFormats = 0;

  private String filename;

  private final Map<String, Short> dataFormats = new HashMap<String, Short>();

  public ExportWorkbook()
  {
    sheets = new ArrayList<ExportSheet>();
    poiWorkbook = new HSSFWorkbook();
  }

  public ExportWorkbook(final File excelFile) throws FileNotFoundException, IOException
  {
    this(new FileInputStream(excelFile));
  }

  public ExportWorkbook(final byte[] excelFile) throws FileNotFoundException, IOException
  {
    this(new ByteArrayInputStream(excelFile));
  }

  public ExportWorkbook(final InputStream is) throws IOException
  {
    try {
      poiWorkbook = new HSSFWorkbook(is, true);
      final int no = poiWorkbook.getNumberOfSheets();
      sheets = new ArrayList<ExportSheet>(no);
      for (int i = 0; i < no; i++) {
        final Sheet sh = poiWorkbook.getSheetAt(i);
        final XlsContentProvider cp = (XlsContentProvider) ExportConfig.getInstance().createNewContentProvider(this);
        cp.setAutoFormatCells(false);
        final ExportSheet sheet = new ExportSheet(cp, poiWorkbook.getSheetName(i), sh);
        sheet.setImported(true);
        sheets.add(sheet);
      }
    } finally {
      if (is != null) {
        is.close();
      }
    }
  }

  /**
   * The file name is ignored by the ExportWorkbook itself. The file name should be used by the caller to create a name
   * for the generated Excel file.
   *
   * @param filename
   */
  public void setFilename(final String filename)
  {
    this.filename = filename;
  }

  /**
   * The file name is ignored by the ExportWorkbook itself. The file name should be used by the caller to create a name
   * for the generated Excel file.
   *
   * @return filename if given and set, otherwise null.
   */
  public String getFilename()
  {
    return filename;
  }

  /**
   * Calls updateStyle for all containing sheets.
   *
   * @see ExportSheet#updateStyles(ContentProvider)
   */
  public void updateStyles()
  {
    for (final ExportSheet sheet : sheets) {
      if (sheet.isImported() == false) {
        // Don't update styles from imported files.
        sheet.updateStyles();
      }
    }
  }

  /**
   * Calls updateStyles first. The OutputStream will be closed by this method.
   *
   * @param out
   * @throws IOException
   * @see #updateStyles()
   */
  public void write(final OutputStream out) throws IOException
  {
    updateStyles();
    try {
      poiWorkbook.write(out);
      if (log.isDebugEnabled() == true) {
        log.info("Excel sheet exported: number of cell styles="
            + this.numberOfCellStyles
            + ", number of data formats="
            + this.numberOfDataFormats);
      }
    } finally {
      out.close();
    }
  }

  public byte[] getAsByteArray()
  {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      write(baos);
    } catch (final IOException ex) {
      log.fatal("Exception encountered " + ex, ex);
      throw new RuntimeException(ex);
    }
    return baos.toByteArray();
  }

  public ExportSheet addSheet(final String name)
  {
    return addSheet(name, null);
  }

  /**
   * @param name
   * @return
   */
  public ExportSheet addSheet(final String name, final ContentProvider contentProvider)
  {
    String title = name;
    if (name.length() >= ExportSheet.MAX_XLS_SHEETNAME_LENGTH) {
      title = StringUtils.abbreviate(name, ExportSheet.MAX_XLS_SHEETNAME_LENGTH);
    }
    final Sheet poiSheet = poiWorkbook.createSheet(title);
    ContentProvider cp = getContentProvider();
    if (contentProvider != null) {
      cp = contentProvider;
    } else {
      cp = ExportConfig.getInstance().createNewContentProvider(this);
    }
    final ExportSheet sheet = new ExportSheet(cp, name, poiSheet);
    sheets.add(sheet);
    return sheet;
  }

  public int getNumberOfSheets()
  {
    return sheets.size();
  }

  public ExportSheet getSheet(final int index)
  {
    return sheets.get(index);
  }

  public ExportSheet getSheet(final String name)
  {
    for (final ExportSheet sheet : sheets) {
      if (StringUtils.equals(sheet.getName(), name) == true) {
        return sheet;
      }
    }
    return null;
  }

  /**
   * Clones the current sheet.
   *
   * @see Workbook#cloneSheet(int)
   */
  public ExportSheet cloneSheet(final int sheetNum, final String name)
  {
    final ExportSheet originSheet = getSheet(sheetNum);
    final Sheet poiSheet = this.poiWorkbook.cloneSheet(sheetNum);
    this.poiWorkbook.setSheetName(sheets.size(), name);
    ContentProvider cp = getContentProvider();
    if (contentProvider != null) {
      cp = contentProvider;
    } else {
      cp = ExportConfig.getInstance().createNewContentProvider(this);
    }
    final ExportSheet sheet = new ExportSheet(cp, poiSheet.getSheetName(), poiSheet);
    sheet.setImported(originSheet.isImported());
    sheets.add(sheet);
    return sheet;
  }

  /**
   * Remove the sheet at the given position.
   *
   * @param index
   * @return this for chaining.
   */
  public ExportWorkbook removeSheetAt(final int index)
  {
    this.poiWorkbook.removeSheetAt(index);
    return this;
  }

  public CellStyle createCellStyle()
  {
    ++numberOfCellStyles;
    return poiWorkbook.createCellStyle();
  }

  public Font createFont()
  {
    return poiWorkbook.createFont();
  }

  public CreationHelper getCreationHelper()
  {
    return poiWorkbook.getCreationHelper();
  }

  public void setContentProvider(final ContentProvider contentProvider)
  {
    this.contentProvider = contentProvider;
  }

  public ContentProvider getContentProvider()
  {
    return contentProvider;
  }

  public short getDataFormat(final String format)
  {
    if (dataFormats.containsKey(format) == true) {
      return dataFormats.get(format);
    }
    final short value = getCreationHelper().createDataFormat().getFormat(format);
    dataFormats.put(format, value);
    ++this.numberOfDataFormats;
    return value;
  }

  public Workbook getPoiWorkbook()
  {
    return poiWorkbook;
  }
}
