/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.export;

import org.apache.commons.lang3.BooleanUtils;
import org.projectforge.business.excel.CellFormat;
import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExportCell;
import org.projectforge.business.excel.ExportWorkbook;
import org.projectforge.business.excel.XlsContentProvider;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.KundeFormatter;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektFormatter;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.common.DateFormatType;
import org.projectforge.common.i18n.I18nEnum;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.DayHolder;

public class MyXlsContentProvider extends XlsContentProvider
{
  public static final int LENGTH_KOSTENTRAEGER = 11;

  public static final int LENGTH_USER = 20;

  public static final int LENGTH_ZIPCODE = 7;

  /**
   * @see XlsContentProvider#newInstance()
   */
  @Override
  public ContentProvider newInstance()
  {
    return new MyXlsContentProvider(this.workbook);
  }

  public MyXlsContentProvider(final ExportWorkbook workbook)
  {
    super(new MyXlsExportContext(), workbook);
    defaultFormatMap.put(DateHolder.class, new CellFormat("YYYY-MM-DD").setAutoDatePrecision(true)); // format unused.
    defaultFormatMap.put(DayHolder.class, new CellFormat(DateFormats.getExcelFormatString(DateFormatType.DATE)));
  }

  /**
   * @see XlsContentProvider#getCustomizedValue(java.lang.Object)
   */
  @Override
  public Object getCustomizedValue(final Object value)
  {
    if (value instanceof DateHolder) {
      return ((DateHolder) value).getCalendar();
    } else if (value instanceof PFUserDO) {
      return ((PFUserDO) value).getFullname();
    } else if (value instanceof I18nEnum) {
      return ThreadLocalUserContext.getLocalizedString(((I18nEnum) value).getI18nKey());
    } else if (value instanceof KontoDO) {
      final KontoDO konto = (KontoDO) value;
      return konto.formatKonto();
    } else if (value instanceof Kost1DO) {
      return KostFormatter.format((Kost1DO) value);
    } else if (value instanceof Kost2DO) {
      return KostFormatter.format((Kost2DO) value);
    } else if (value instanceof KundeDO) {
      return KundeFormatter.formatKundeAsString((KundeDO) value, null);
    } else if (value instanceof ProjektDO) {
      return ProjektFormatter.formatProjektKundeAsString((ProjektDO) value, null, null);
    } else if (value instanceof EmployeeDO) {
      final EmployeeDO employee = (EmployeeDO) value;
      return employee.getUser() != null ? employee.getUser().getFullname() : "???";
    }
    return null;
  }

  /**
   * @see XlsContentProvider#getCellFormat(ExportCell, java.lang.Object, java.lang.String,
   * java.util.Map)
   */
  @Override
  protected CellFormat getCustomizedCellFormat(final CellFormat format, final Object value)
  {
    if (value == null || DateHolder.class.isAssignableFrom(value.getClass()) == false) {
      return null;
    }
    if (format != null && BooleanUtils.isTrue(format.getAutoDatePrecision()) == false) {
      return null;
    }
    // Find a format dependent on the precision:
    final DatePrecision precision = ((DateHolder) value).getPrecision();
    if (precision == DatePrecision.DAY) {
      return new CellFormat(DateFormats.getExcelFormatString(DateFormatType.DATE));
    } else if (precision == DatePrecision.SECOND) {
      return new CellFormat(DateFormats.getExcelFormatString(DateFormatType.DATE_TIME_SECONDS));
    } else if (precision == DatePrecision.MILLISECOND) {
      return new CellFormat(DateFormats.getExcelFormatString(DateFormatType.DATE_TIME_MILLIS));
    } else {
      // HOUR_OF_DAY, MINUTE, MINUTE_15 or null
      return new CellFormat(DateFormats.getExcelFormatString(DateFormatType.DATE_TIME_MINUTES));
    }
  }
}
