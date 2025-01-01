/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;

import java.util.Objects;

public class CellFormat
{
  private String dataFormat;

  private Short alignment;

  private Font font;

  private Short fillForegroundColor;

  private Boolean wrapText;

  private Boolean autoDatePrecision;

  /**
   * @param format Excel format string, e. g. "yyyy-MM-dd HH:mm", "#,##0", ...
   */
  public CellFormat(String format)
  {
    this.dataFormat = format;
  }

  public CellFormat()
  {
  }

  /**
   * @param format
   * @param alignment
   */
  public CellFormat(String format, Short alignment)
  {
    this.dataFormat = format;
    this.alignment = alignment;
  }

  public CellFormat setAutoDatePrecision(Boolean autoDatePrecision)
  {
    this.autoDatePrecision = autoDatePrecision;
    return this;
  }

  /**
   * Only for values of type DateHolder: if true then the date format is dependent on the precision of the DateHolder.
   *
   * @see DateHolder#getPrecision()
   */
  public Boolean getAutoDatePrecision()
  {
    return autoDatePrecision;
  }

  /**
   * Please note: the data format should be set by the ContentProvider (for re-usage).
   *
   * @param cellStyle
   */
  public void copyToCellStyle(final CellStyle cellStyle)
  {
    if (alignment != null) {
      cellStyle.setAlignment(HorizontalAlignment.forInt(alignment));
    }
    if (font != null) {
      cellStyle.setFont(font);
    }
    if (fillForegroundColor != null) {
      cellStyle.setFillForegroundColor(fillForegroundColor);
    }
    if (wrapText != null) {
      cellStyle.setWrapText(wrapText);
    }
  }

  /**
   * Excel format string, e. g. "yyyy-MM-dd HH:mm", "#,##0", ... Ignored, if null.
   */
  public String getDataFormat()
  {
    return dataFormat;
  }

  public CellFormat setDataFormat(String dataFormat)
  {
    this.dataFormat = dataFormat;
    return this;
  }

  /**
   * Ignored, if null.
   */
  public Short getAlignment()
  {
    return alignment;
  }

  public CellFormat setAlignment(Short alignment)
  {
    this.alignment = alignment;
    return this;
  }

  /**
   * Ignored, if null.
   */
  public Short getFillForegroundColor()
  {
    return fillForegroundColor;
  }

  public CellFormat setFillForegroundColor(Short fillForegroundColor)
  {
    this.fillForegroundColor = fillForegroundColor;
    return this;
  }

  /**
   * Ignored, if null.
   */
  public Font getFont()
  {
    return font;
  }

  public CellFormat setFont(Font font)
  {
    this.font = font;
    return this;
  }

  public Boolean getWrapText()
  {
    return wrapText;
  }

  public void setWrapText(Boolean wrapText)
  {
    this.wrapText = wrapText;
  }

  @Override
  public int hashCode()
  {
    return new HashCodeBuilder().append(this.dataFormat).append(alignment).append(font).append(fillForegroundColor).append(wrapText).hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof CellFormat) {
      final CellFormat other = (CellFormat) obj;
      if (!Objects.equals(this.dataFormat, other.dataFormat)
          || !Objects.equals(this.alignment, other.alignment)
          || !Objects.equals(this.font, other.font)
          || !Objects.equals(this.fillForegroundColor, other.fillForegroundColor))
        return false;
      return this.wrapText == other.wrapText;
    }
    return false;
  }

  @Override
  protected CellFormat clone() {
    //final CellFormat clone = (CellFormat) super.clone();
    final CellFormat clone = new CellFormat();
    clone.alignment = this.alignment;
    clone.autoDatePrecision = this.autoDatePrecision;
    clone.dataFormat = this.dataFormat;
    clone.fillForegroundColor = this.fillForegroundColor;
    clone.font = this.font;
    clone.wrapText = this.wrapText;
    return clone;
  }
}
