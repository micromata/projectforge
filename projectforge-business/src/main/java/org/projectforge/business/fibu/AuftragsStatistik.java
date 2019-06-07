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

import java.io.Serializable;
import java.math.BigDecimal;

import org.projectforge.framework.utils.NumberHelper;

public class AuftragsStatistik implements Serializable
{
  private static final long serialVersionUID = -5486964211679100585L;

  /**
   * Sum of all nets.
   */
  private BigDecimal nettoSum;

  /**
   * Sum of the nets where the order is in POTENZIAL, IN_ERSTELLUNG or GELEGT.
   */
  private BigDecimal akquiseSum;

  /**
   * Sum of the "beauftragt" nets where the order is in LOI, BEAUFTRAGT or ESKALATION.
   */
  private BigDecimal beauftragtSum;

  /**
   * Sum of the "fakturiert sums" of all orders.
   */
  private BigDecimal fakturiertSum;

  /**
   * Sum of the "zu fakturieren sums" of the orders which are ABGESCHLOSSEN and not "vollstaendig fakturiert".
   */
  private BigDecimal zuFakturierenSum;

  /**
   * Count of orders considered in these statistics.
   */
  private int counter;

  private int counterAkquise;

  private int counterBeauftragt;

  private int counterZuFakturieren;

  private int counterFakturiert;

  public AuftragsStatistik()
  {
    nettoSum = akquiseSum = beauftragtSum = fakturiertSum = zuFakturierenSum = BigDecimal.ZERO;
    counter = counterBeauftragt = counterZuFakturieren = counterFakturiert = 0;
  }

  public void add(final AuftragDO auftrag)
  {
    final BigDecimal netto = auftrag.getNettoSumme();
    if (auftrag.getAuftragsStatus() != null) {
      if (auftrag.getAuftragsStatus().isIn(AuftragsStatus.POTENZIAL, AuftragsStatus.IN_ERSTELLUNG, AuftragsStatus.GELEGT) == true) {
        akquiseSum = NumberHelper.add(akquiseSum, netto);
        counterAkquise++;
      } else if (auftrag.getAuftragsStatus().isIn(AuftragsStatus.LOI, AuftragsStatus.BEAUFTRAGT, AuftragsStatus.ESKALATION) == true) {
        beauftragtSum = NumberHelper.add(beauftragtSum, auftrag.getBeauftragtNettoSumme());
        counterBeauftragt++;
      } else if (auftrag.getAuftragsStatus().isIn(AuftragsStatus.ABGESCHLOSSEN) == true && auftrag.isVollstaendigFakturiert() == false) {
        zuFakturierenSum = NumberHelper.add(zuFakturierenSum, auftrag.getZuFakturierenSum());
        counterZuFakturieren++;
      }
    }
    final BigDecimal invoiced = auftrag.getFakturiertSum();
    if (NumberHelper.isNotZero(invoiced) == true) {
      fakturiertSum = NumberHelper.add(fakturiertSum, invoiced);
      counterFakturiert++;
    } else if (auftrag.isVollstaendigFakturiert() == true) {
      counterFakturiert++;
    }
    counter++;
    nettoSum = NumberHelper.add(nettoSum, netto);
  }

  public BigDecimal getNettoSum()
  {
    return nettoSum;
  }

  public BigDecimal getAkquiseSum()
  {
    return akquiseSum;
  }

  public BigDecimal getBeauftragtSum()
  {
    return beauftragtSum;
  }

  public BigDecimal getFakturiertSum()
  {
    return fakturiertSum;
  }

  public BigDecimal getZuFakturierenSum()
  {
    return zuFakturierenSum;
  }

  public int getCounter()
  {
    return counter;
  }

  public int getCounterAkquise()
  {
    return counterAkquise;
  }

  public int getCounterBeauftragt()
  {
    return counterBeauftragt;
  }

  public int getCounterFakturiert()
  {
    return counterFakturiert;
  }

  public int getCounterZuFakturieren()
  {
    return counterZuFakturieren;
  }
}
