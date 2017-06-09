package org.projectforge.business.fibu;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.criterion.Restrictions;
import org.projectforge.framework.i18n.RequiredFieldIsEmptyException;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DayHolder;

public class AbstractRechnungDaoHelper
{
  public static QueryFilter createQueryFilterWithDateRestriction(final RechnungFilter myFilter)
  {
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    final java.sql.Date fromDate = DateHelper.convertDateToSqlDateInTheUsersTimeZone(myFilter.getFromDate());
    final java.sql.Date toDate = DateHelper.convertDateToSqlDateInTheUsersTimeZone(myFilter.getToDate());

    if (fromDate != null && toDate != null) {
      queryFilter.add(Restrictions.between("datum", fromDate, toDate));
    } else if (fromDate != null) {
      queryFilter.add(Restrictions.ge("datum", fromDate));
    } else if (toDate != null) {
      queryFilter.add(Restrictions.le("datum", toDate));
    }

    return queryFilter;
  }

  public static void onSaveOrModify(final AbstractRechnungDO<?> rechnung)
  {
    checkAndCalculateFaelligkeit(rechnung);
    checkAndCalculateDiscountMaturity(rechnung);
    validateFaelligkeit(rechnung);
    validateBezahlDatumAndZahlBetrag(rechnung);
  }

  private static void checkAndCalculateFaelligkeit(final AbstractRechnungDO<?> rechnung)
  {
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

  private static void checkAndCalculateDiscountMaturity(final AbstractRechnungDO<?> rechnung)
  {
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

  private static void validateFaelligkeit(final AbstractRechnungDO<?> rechnung)
  {
    if (rechnung.getFaelligkeit() == null) {
      throw new RequiredFieldIsEmptyException("fibu.rechnung.faelligkeit");
    }
  }

  private static void validateBezahlDatumAndZahlBetrag(final AbstractRechnungDO<?> rechnung)
  {
    final Date bezahlDatum = rechnung.getBezahlDatum();
    final BigDecimal zahlBetrag = rechnung.getZahlBetrag();
    final boolean zahlBetragExists = (zahlBetrag != null && zahlBetrag.compareTo(BigDecimal.ZERO) != 0);

    if (bezahlDatum != null && zahlBetragExists == false) {
      throw new UserException("fibu.rechnung.error.bezahlDatumUndZahlbetragRequired");
    }
  }
}
