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

package org.projectforge.plugins.liquidityplanning;

import org.projectforge.business.excel.*;
import org.projectforge.business.scripting.I18n;
import org.projectforge.common.DateFormatType;
import org.projectforge.export.MyExcelExporter;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.utils.NumberHelper;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LiquidityForecastCashFlow implements Serializable {
  private static final long serialVersionUID = 7567091917817930061L;

  private final BigDecimal[] credits;

  private final BigDecimal[] debits;

  private final BigDecimal[] creditsExpected;

  private final BigDecimal[] debitsExpected;

  private final PFDay baseDate;

  public LiquidityForecastCashFlow(final LiquidityForecast forecast) {
    this(forecast, 90);
  }

  public LiquidityForecastCashFlow(final LiquidityForecast forecast, final int nextDays) {
    baseDate = PFDay.fromOrNow(forecast.getBaseDate());
    credits = newBigDecimalArray(nextDays);
    debits = newBigDecimalArray(nextDays);
    creditsExpected = newBigDecimalArray(nextDays);
    debitsExpected = newBigDecimalArray(nextDays);
    for (final LiquidityEntry entry : forecast.getEntries()) {
      final BigDecimal amount = entry.getAmount();
      if (amount == null) {
        continue;
      }
      final LocalDate dateOfPayment = entry.getDateOfPayment();
      LocalDate expectedDateOfPayment = entry.getExpectedDateOfPayment();
      if (expectedDateOfPayment == null) {
        expectedDateOfPayment = dateOfPayment;
      }
      int numberOfDay = 0;
      if (dateOfPayment != null) {
        final PFDay dayOfPayment = PFDay.from(dateOfPayment); // not null
        if (baseDate.isBefore(dayOfPayment) && !baseDate.isSameDay(dayOfPayment)) {
          numberOfDay = (int) baseDate.daysBetween(dayOfPayment);
        }
      }
      if (numberOfDay >= 0 && numberOfDay < nextDays) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
          // Zero, nothing to do.
        } else if (amount.compareTo(BigDecimal.ZERO) > 0) {
          debits[numberOfDay] = debits[numberOfDay].add(amount);
        } else {
          credits[numberOfDay] = credits[numberOfDay].add(amount);
        }
      }
      int numberOfDayExpected = 0;
      if (expectedDateOfPayment != null) {
        final PFDay expectedDayOfPayment = PFDay.from(expectedDateOfPayment); // not null
        if (baseDate.isBefore(expectedDayOfPayment) && !baseDate.isSameDay(expectedDayOfPayment)) {
          numberOfDayExpected = (int) baseDate.daysBetween(expectedDayOfPayment);
        }
      }
      if (numberOfDayExpected >= 0 && numberOfDayExpected < nextDays) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
          // Zero, nothing to do.
        } else if (amount.compareTo(BigDecimal.ZERO) > 0) {
          debitsExpected[numberOfDayExpected] = debitsExpected[numberOfDayExpected].add(amount);
        } else {
          creditsExpected[numberOfDayExpected] = creditsExpected[numberOfDayExpected].add(amount);
        }
      }
    }
  }

  private BigDecimal[] newBigDecimalArray(final int length) {
    final BigDecimal[] array = new BigDecimal[length];
    for (int i = 0; i < length; i++) {
      array[i] = BigDecimal.ZERO;
    }
    return array;
  }

  public void addAsExcelSheet(final MyExcelExporter exporter, final String sheetTitle) {
    final ExportSheet sheet = exporter.addSheet(sheetTitle);
    sheet.createFreezePane(0, 1);
    final ContentProvider sheetProvider = sheet.getContentProvider();

    sheet.addRow();
    sheet.setMergedRegion(0, 0, 1, 3, I18n.getString("plugins.liquidityplanning.entry.expectedDateOfPayment"));
    sheet.setMergedRegion(0, 0, 4, 6, I18n.getString("plugins.liquidityplanning.forecast.dueDate"));

    final ExportColumn[] cols = new ExportColumn[7];
    int colNo = 0;
    I18nExportColumn exportColumn = new I18nExportColumn("date", "date", 10);
    sheetProvider.putFormat(exportColumn, DateFormats.getExcelFormatString(DateFormatType.DATE));
    cols[colNo++] = exportColumn;

    exportColumn = new I18nExportColumn("creditsExpected", "plugins.liquidityplanning.common.credit");
    cols[colNo++] = exportColumn;
    exporter.putCurrencyFormat(sheetProvider, exportColumn);
    exportColumn = new I18nExportColumn("debitsExpected", "plugins.liquidityplanning.common.debit");
    cols[colNo++] = exportColumn;
    exporter.putCurrencyFormat(sheetProvider, exportColumn);
    exportColumn = new I18nExportColumn("balanceExpected", "plugins.liquidityplanning.forecast.balance");
    cols[colNo++] = exportColumn;
    exporter.putCurrencyFormat(sheetProvider, exportColumn);

    exportColumn = new I18nExportColumn("credits", "plugins.liquidityplanning.common.credit");
    cols[colNo++] = exportColumn;
    exporter.putCurrencyFormat(sheetProvider, exportColumn);
    exportColumn = new I18nExportColumn("debits", "plugins.liquidityplanning.common.debit");
    cols[colNo++] = exportColumn;
    exporter.putCurrencyFormat(sheetProvider, exportColumn);
    exportColumn = new I18nExportColumn("balance", "plugins.liquidityplanning.forecast.balance");
    cols[colNo++] = exportColumn;
    exporter.putCurrencyFormat(sheetProvider, exportColumn);

    // column property names
    sheet.setColumns(cols);

    final int firstDataRowNumber = sheet.getRowCounter() + 1;
    PFDay current = baseDate;
    PropertyMapping mapping = new PropertyMapping();
    mapping.add("balanceExpected", BigDecimal.ZERO);
    mapping.add("balance", new Formula("D" + firstDataRowNumber));
    sheet.addRow(mapping.getMapping(), 0);

    for (int i = 0; i < credits.length; i++) {
      final int rowNumber = sheet.getRowCounter();
      mapping.add("date", current);
      mapping.add("creditsExpected", NumberHelper.isZeroOrNull(creditsExpected[i]) ? "" : creditsExpected[i]);
      mapping.add("debitsExpected", NumberHelper.isZeroOrNull(debitsExpected[i]) ? "" : debitsExpected[i]);
      mapping.add("balanceExpected", new Formula("D" + rowNumber + "+SUM(B" + rowNumber + ":C" + rowNumber + ")"));
      mapping.add("credits", NumberHelper.isZeroOrNull(credits[i]) ? "" : credits[i]);
      mapping.add("debits", NumberHelper.isZeroOrNull(debits[i]) ? "" : debits[i]);
      mapping.add("balance", new Formula("G" + rowNumber + "+SUM(E" + rowNumber + ":F" + rowNumber + ")"));
      sheet.addRow(mapping.getMapping(), 0);
      current = current.plusDays(1);
    }
    mapping = new PropertyMapping();
    mapping.add("creditsExpected", new Formula("SUM(B" + firstDataRowNumber + ":B" + sheet.getRowCounter() + ")"));
    mapping.add("debitsExpected", new Formula("SUM(C" + firstDataRowNumber + ":C" + sheet.getRowCounter() + ")"));
    mapping.add("balanceExpected", new Formula("D" + firstDataRowNumber + "+SUM(B" + firstDataRowNumber + ":C" + sheet.getRowCounter() + ")"));
    mapping.add("credits", new Formula("SUM(E" + firstDataRowNumber + ":E" + sheet.getRowCounter() + ")"));
    mapping.add("debits", new Formula("SUM(F" + firstDataRowNumber + ":F" + sheet.getRowCounter() + ")"));
    mapping.add("balance", new Formula("G" + firstDataRowNumber + "+SUM(E" + firstDataRowNumber + ":F" + sheet.getRowCounter() + ")"));
    sheet.addRow(mapping.getMapping(), 0);
  }

  /**
   * @return the credits based on due dates.
   */
  public BigDecimal[] getCredits() {
    return credits;
  }

  /**
   * @return the creditsExpected based on expected dates of payment.
   */
  public BigDecimal[] getCreditsExpected() {
    return creditsExpected;
  }

  /**
   * @return the debits based on due dates.
   */
  public BigDecimal[] getDebits() {
    return debits;
  }

  /**
   * @return the debitsExpected based on expected dates of payment.
   */
  public BigDecimal[] getDebitsExpected() {
    return debitsExpected;
  }
}
