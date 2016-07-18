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

package org.projectforge.common;

/**
 * Determines the mime-types of files by the file-name's extension.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum MimeType
{
  // See e. g.: http://de.selfhtml.org/diverses/mimetypen.htm
  DOC("application/vnd.ms-word", "doc", "docx"), //
  GZIP("application/gzip", "gz"), //
  JPG("image/jpeg", "jpg", "jpeg"), //
  MS_PROJECT("application/vnd.ms-project", "mpx"), // OCTET_STREAM("application/octet-stream"),
  PDF("application/pdf", "pdf"), //
  PNG("image/png", "png"), //
  SVG("image/svg+xml", "svg"), //
  TEXT("text", "txt", "csv", "sql"), //
  VCARD("text/x-vcard", "vcf"), //
  XLS("application/vnd.ms-excel", "xls"), //
  XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"), //
  XML("application/xml", "xml"), //
  ZIP("application/zip", "zip");

  private String mimeType;

  private String[] extensions;

  private MimeType(final String mimeType, final String... extensions)
  {
    this.mimeType = mimeType;
    this.extensions = extensions;
  }

  /**
   * "application/pdf", "application/xml" etc.
   * @return
   */
  public String getMimeTypeString()
  {
    return mimeType;
  }

  public static MimeType getMimeType(final String filename)
  {
    if (filename == null) {
      return null;
    }
    final int pos = filename.lastIndexOf('.');
    if (pos < 0 || pos + 1 == filename.length()) {
      return null;
    }
    final String extensionString = filename.substring(pos + 1);
    for (final MimeType type : MimeType.values()) {
      final String[] extensions = type.extensions;
      if (extensionString == null) {
        continue;
      }
      for (final String extension : extensions) {
        if (extensionString.equals(extension) == true) {
          return type;
        }
      }
    }
    return null;
  }
}
