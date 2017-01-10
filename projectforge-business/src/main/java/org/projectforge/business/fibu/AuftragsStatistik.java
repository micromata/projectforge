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

package org.projectforge.business.fibu;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.projectforge.framework.utils.NumberHelper;

public class AuftragsStatistik implements Serializable
{
  private static final long serialVersionUID = -5486964211679100585L;

  protected BigDecimal nettoSum;

  protected BigDecimal akquiseSum;

  protected BigDecimal beauftragtSum;

  protected BigDecimal fakturiertSum;

  protected BigDecimal zuFakturierenSum;

  protected int counter;

  protected int counterAkquise;

  protected int counterBeauftragt;

  protected int counterZuFakturieren;

  protected int counterFakturiert;

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
        akquiseSum = add(akquiseSum, netto);
        counterAkquise++;
      } else if (auftrag.getAuftragsStatus().isIn(AuftragsStatus.LOI, AuftragsStatus.BEAUFTRAGT, AuftragsStatus.ESKALATION) == true) {
        beauftragtSum = add(beauftragtSum, auftrag.getBeauftragtNettoSumme());
        counterBeauftragt++;
      } else if (auftrag.getAuftragsStatus().isIn(AuftragsStatus.ABGESCHLOSSEN) == true && auftrag.isVollstaendigFakturiert() == false) {
        zuFakturierenSum = add(zuFakturierenSum, auftrag.getZuFakturierenSum());
        counterZuFakturieren++;
      }
    }
    final BigDecimal invoiced = auftrag.getFakturiertSum();
    if (NumberHelper.isNotZero(invoiced) == true) {
      fakturiertSum = add(fakturiertSum, invoiced);
      counterFakturiert++;
    } else if (auftrag.isVollstaendigFakturiert() == true) {
      counterFakturiert++;
    }
    counter++;
    nettoSum = add(nettoSum, netto);
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

  private BigDecimal add(BigDecimal sum, final BigDecimal amount)
  {
    if (amount == null) {
      return sum;
    }
    if (sum == null) {
      sum = BigDecimal.ZERO;
    }
    sum = sum.add(amount);
    sum.setScale(2, RoundingMode.HALF_UP);
    return sum;
  }
}
