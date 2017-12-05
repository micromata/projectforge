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

package org.projectforge.web.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.util.resource.AbstractResourceStreamWriter;
import org.apache.wicket.util.resource.IResourceStream;
import org.projectforge.business.excel.ExportWorkbook;
import org.projectforge.export.ExportJFreeChart;
import org.projectforge.export.ExportZipFile;

/**
 * For exporting multiple objects by one script you may collect all objects within this zip archive.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ExportZipArchive
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExportZipArchive.class);

  private final Collection<ExportZipFile> zipFiles = new LinkedList<ExportZipFile>();

  private final String filename;

  public ExportZipArchive()
  {
    this.filename = "archive";
  }

  /**
   * @param filename The filename of the zip archive (without extension), default is "archive".
   */
  public ExportZipArchive(final String filename)
  {
    this.filename = filename;
  }

  public void write(final OutputStream out)
  {
    final ZipOutputStream zipOut = new ZipOutputStream(out);
    try {
      zipOut.putNextEntry(new ZipEntry(filename + "/"));
      for (final ExportZipFile file : zipFiles) {
        final ZipEntry zipEntry = new ZipEntry(filename + "/" + file.getFilename());
        zipOut.putNextEntry(zipEntry);
        if (file.getExportObject() instanceof ExportWorkbook) {
          final ExportWorkbook workbook = (ExportWorkbook) file.getExportObject();
          final byte[] xls = workbook.getAsByteArray();
          if (xls == null || xls.length == 0) {
            log.error("Oups, xls has zero size. Filename: " + filename);
            continue;
          }
          zipOut.write(xls);
        } else if (file.getExportObject() instanceof ExportJFreeChart) {
          final ExportJFreeChart exportJFreeChart = (ExportJFreeChart) file.getExportObject();
          exportJFreeChart.write(zipOut);
        }
        zipOut.closeEntry();
      }
    } catch (final IOException ex) {
      log.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    } finally {
      IOUtils.closeQuietly(zipOut);
    }
  }

  public IResourceStream createResourceStreamWriter()
  {
    final IResourceStream iResourceStream = new AbstractResourceStreamWriter()
    {
      private static final long serialVersionUID = 7780552906708508709L;

      @Override
      public String getContentType()
      {
        return "application/zip";
      }

      @Override
      public void write(final OutputStream output)
      {
        ExportZipArchive.this.write(output);
      }
    };
    return iResourceStream;
  }

  public ExportZipArchive add(final String filename, final ExportWorkbook exportWorkbook)
  {
    zipFiles.add(new ExportZipFile(filename, exportWorkbook));
    return this;
  }

  public ExportZipArchive add(final ExportWorkbook exportWorkbook)
  {
    zipFiles.add(new ExportZipFile(exportWorkbook.getFilename(), exportWorkbook));
    return this;
  }

  public ExportZipArchive add(final String filename, final ExportJFreeChart exportJFreeChart)
  {
    zipFiles.add(new ExportZipFile(filename, exportJFreeChart));
    return this;
  }

  /**
   * @return the filename
   */
  public String getFilename()
  {
    return filename;
  }

  public Collection<ExportZipFile> getFiles()
  {
    return zipFiles;
  }
}
