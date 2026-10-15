/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.time.PFDay;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Kai Reinhard
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
