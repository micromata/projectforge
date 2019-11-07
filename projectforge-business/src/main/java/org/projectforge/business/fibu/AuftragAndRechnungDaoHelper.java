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

package org.projectforge.business.fibu;

import org.projectforge.framework.i18n.RequiredFieldIsEmptyException;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.impl.DBPredicate;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DayHolder;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

public class AuftragAndRechnungDaoHelper {
  public static Optional<DBPredicate> createCriterionForPeriodOfPerformance(final SearchFilterWithPeriodOfPerformance myFilter) {
    final String popBeginName = "periodOfPerformanceBegin";
    final String popEndName = "periodOfPerformanceEnd";

    final java.sql.Date startDate = DateHelper.convertDateToSqlDateInTheUsersTimeZone(myFilter.getPeriodOfPerformanceStartDate());
    final java.sql.Date endDate = DateHelper.convertDateToSqlDateInTheUsersTimeZone(myFilter.getPeriodOfPerformanceEndDate());

    if (startDate != null && endDate != null) {
      return Optional.of(
              QueryFilter.and(QueryFilter.ge(popEndName, startDate),
                      QueryFilter.le(popBeginName, endDate)
              )
      );
    }

    if (startDate != null) {
      return Optional.of(
              QueryFilter.ge(popEndName, startDate)
      );
    }

    if (endDate != null) {
      return Optional.of(
              QueryFilter.le(popBeginName, endDate)
      );
    }

    return Optional.empty();
  }

  public static QueryFilter createQueryFilterWithDateRestriction(final RechnungFilter myFilter) {
    final String dateName = "datum";

    final Date from = myFilter.getFromDate();
    final Date to = myFilter.getToDate();

    final java.sql.Date fromDate = from instanceof java.sql.Date ? (java.sql.Date) from : DateHelper.convertDateToSqlDateInTheUsersTimeZone(from);
    final java.sql.Date toDate = to instanceof java.sql.Date ? (java.sql.Date) to : DateHelper.convertDateToSqlDateInTheUsersTimeZone(to);

    final QueryFilter queryFilter = new QueryFilter(myFilter);

    if (fromDate != null && toDate != null) {
      queryFilter.add(QueryFilter.between(dateName, fromDate, toDate));
    } else if (fromDate != null) {
      queryFilter.add(QueryFilter.ge(dateName, fromDate));
    } else if (toDate != null) {
      queryFilter.add(QueryFilter.le(dateName, toDate));
    }

    return queryFilter;
  }

  public static void onSaveOrModify(final AbstractRechnungDO rechnung) {
    checkAndCalculateFaelligkeit(rechnung);
    checkAndCalculateDiscountMaturity(rechnung);
    validateFaelligkeit(rechnung);
    validateBezahlDatumAndZahlBetrag(rechnung);
  }

  private static void checkAndCalculateFaelligkeit(final AbstractRechnungDO rechnung) {
    final Integer zahlungsZiel = rechnung.getZahlungsZielInTagen();
    if (rechnung.getFaelligkeit() == null && zahlungsZiel != null) {
      final Date rechnungsDatum = rechnung.getDatum();
      if (rechnungsDatum != null) {
        final DayHolder day = new DayHolder(rechnungsDatum);
        day.add(Calendar.DAY_OF_YEAR, zahlungsZiel);
        rechnung.setFaelligkeit(day.getSQLDate());
      }
    }
  }

  private static void checkAndCalculateDiscountMaturity(final AbstractRechnungDO rechnung) {
    final Integer discountZahlungsZiel = rechnung.getDiscountZahlungsZielInTagen();
    if (rechnung.getDiscountMaturity() == null && discountZahlungsZiel != null) {
      final Date rechnungsDatum = rechnung.getDatum();
      if (rechnungsDatum != null) {
        final DayHolder day = new DayHolder(rechnungsDatum);
        day.add(Calendar.DAY_OF_YEAR, discountZahlungsZiel);
        rechnung.setDiscountMaturity(day.getSQLDate());
      }
    }
  }

  private static void validateFaelligkeit(final AbstractRechnungDO rechnung) {
    if (rechnung.getFaelligkeit() == null) {
      throw new RequiredFieldIsEmptyException("fibu.rechnung.faelligkeit");
    }
  }

  private static void validateBezahlDatumAndZahlBetrag(final AbstractRechnungDO rechnung) {
    final Date bezahlDatum = rechnung.getBezahlDatum();
    final BigDecimal zahlBetrag = rechnung.getZahlBetrag();
    final boolean zahlBetragExists = (zahlBetrag != null && zahlBetrag.compareTo(BigDecimal.ZERO) != 0);

    if (bezahlDatum != null && !zahlBetragExists) {
      throw new UserException("fibu.rechnung.error.zahlbetragRequired");
    }
    if (bezahlDatum == null && zahlBetragExists) {
      throw new UserException("fibu.rechnung.error.bezahlDatumRequired");
    }
  }
}
