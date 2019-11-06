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

import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.statistics.IntAggregatedValues;

import java.io.Serializable;
import java.math.BigDecimal;

public class AbstractRechnungsStatistik<T extends AbstractRechnungDO> implements Serializable
{
  private static final long serialVersionUID = 3695426728243488756L;

  protected BigDecimal bruttoSum;

  protected BigDecimal nettoSum;

  protected BigDecimal gezahltSum;

  protected BigDecimal offenSum;

  protected BigDecimal ueberfaelligSum;

  protected BigDecimal skontoSum;

  protected long zahlungsZielSum;

  protected IntAggregatedValues tatsaechlichesZahlungsZiel = new IntAggregatedValues();

  protected int counterBezahlt;

  protected int counter;

  public AbstractRechnungsStatistik()
  {
    bruttoSum = nettoSum = gezahltSum = offenSum = ueberfaelligSum = skontoSum = BigDecimal.ZERO;
    counter = counterBezahlt = 0;
  }

  public void add(final T rechnung)
  {
    final BigDecimal netto = rechnung.getNetSum();
    final BigDecimal brutto = rechnung.getGrossSum();
    final BigDecimal gezahlt = rechnung.getZahlBetrag();
    this.nettoSum = NumberHelper.add(nettoSum, netto);
    this.bruttoSum = NumberHelper.add(bruttoSum, brutto);
    if (gezahlt != null) {
      gezahltSum = NumberHelper.add(gezahltSum, gezahlt);
      if (gezahlt.compareTo(brutto) < 0) {
        skontoSum = NumberHelper.add(skontoSum, brutto.subtract(gezahlt));
      }
    } else {
      offenSum = NumberHelper.add(offenSum, brutto);
      if (rechnung.isUeberfaellig()) {
        ueberfaelligSum = NumberHelper.add(ueberfaelligSum, brutto);
      }
    }
    final DayHolder datum = new DayHolder(rechnung.getDatum());
    final DayHolder faelligDatum = new DayHolder(rechnung.getFaelligkeit());
    zahlungsZielSum += datum.daysBetween(faelligDatum);
    if (rechnung.getBezahlDatum() != null) {
      final DayHolder bezahlDatum = new DayHolder(rechnung.getBezahlDatum());
      tatsaechlichesZahlungsZiel.add(datum.daysBetween(bezahlDatum), brutto.intValue());
      counterBezahlt++;
    }
    counter++;
  }

  public int getZahlungszielAverage()
  {
    if (counter == 0) {
      return 0;
    }
    return (int) (zahlungsZielSum / counter);
  }

  public int getTatsaechlichesZahlungzielAverage()
  {
    return tatsaechlichesZahlungsZiel.getWeightedAverage();
  }

  public BigDecimal getBrutto()
  {
    return bruttoSum;
  }

  public BigDecimal getNetto()
  {
    return nettoSum;
  }

  public BigDecimal getGezahlt()
  {
    return gezahltSum;
  }

  public BigDecimal getOffen()
  {
    return offenSum;
  }

  public BigDecimal getUeberfaellig()
  {
    return ueberfaelligSum;
  }

  public int getCounter()
  {
    return counter;
  }

  public int getCounterBezahlt()
  {
    return counterBezahlt;
  }

  /**
   * Fehlbeträge, die der Kunde weniger überwiesen hat und die akzeptiert wurden, d. h. die Rechnung gilt als bezahlt.
   */
  public BigDecimal getSkonto()
  {
    return skontoSum;
  }
}
