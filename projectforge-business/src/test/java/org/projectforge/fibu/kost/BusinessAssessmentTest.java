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

package org.projectforge.fibu.kost;

import static org.testng.AssertJUnit.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.kost.BuchungssatzDO;
import org.projectforge.business.fibu.kost.BusinessAssessment;
import org.projectforge.business.fibu.kost.BusinessAssessmentConfig;
import org.testng.annotations.Test;

public class BusinessAssessmentTest
{
  @Test
  public void testCalculations()
  {
    // <row no="1020" id="umsatzErloese" accountRange="4000-4799" priority="middle" title="Umsatzerlöse" />
    // <row no="1051" id="gesamtleistung" value="=umsatzErloese+bestVerdg+aktEigenleistungen" priority="high" title="Gesamtleistung" />
    // <row no='1220' id='kostenWarenabgabe' accountRange='6740' priority='low' title='Kosten Warenabgabe' />
    // <row no='1260' id='sonstigeKosten' accountRange='6300,6800-6855' priority='low' title='sonstige Kosten' />

    final BusinessAssessmentConfig bwaConfig = BusinessAssessmentConfigTest.getBusinessAssessmentConfig();
    final List<BuchungssatzDO> records = new ArrayList<BuchungssatzDO>();
    records.add(createRecord(8.08, 4000));
    records.add(createRecord(16.16, 4123));
    records.add(createRecord(32.32, 4799));
    records.add(createRecord(-1.01, 6740));
    records.add(createRecord(-2.02, 6740));
    records.add(createRecord(-1.01, 6300));
    records.add(createRecord(-2.02, 6800));
    records.add(createRecord(-4.04, 6855));
    records.add(createRecord(-8.08, 6805));
    final BusinessAssessment bwa = new BusinessAssessment(bwaConfig, records);
    assertEquals(new BigDecimal("56.56"), bwa.getRow("umsatzErloese").getAmount());
    assertEquals(new BigDecimal("-3.03"), bwa.getRow("1220").getAmount());
    assertEquals(new BigDecimal("-15.15"), bwa.getRow("1260").getAmount());
    assertEquals(new BigDecimal("56.56"), bwa.getRow("1051").getAmount()); // Revenue
    assertEquals(new BigDecimal("-18.18"), bwa.getRow("1280").getAmount()); // Total costs
    assertEquals(new BigDecimal("38.38"), bwa.getRow("1380").getAmount()); // profit
    assertEquals(new BigDecimal("67.86"), bwa.getRow("erfolgsquote").getAmount()); // profit * 100 / revenue
    assertEquals(new BigDecimal("0.68"), bwa.getRow("relativePerformance").getAmount()); // profit / revenue
  }

  private BuchungssatzDO createRecord(final double amount, final int accountNumber)
  {
    final BuchungssatzDO record = new BuchungssatzDO();
    final KontoDO konto = new KontoDO();
    konto.setNummer(accountNumber);
    record.setKonto(konto);
    record.setBetrag(new BigDecimal(amount));
    return record;
  }
}
