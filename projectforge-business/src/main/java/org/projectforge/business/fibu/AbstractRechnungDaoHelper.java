package org.projectforge.business.fibu;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.projectforge.framework.i18n.RequiredFieldIsEmptyException;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.time.DayHolder;

public class AbstractRechnungDaoHelper
{
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
