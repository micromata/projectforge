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

package org.projectforge.reporting.impl;

import java.math.BigDecimal;
import java.util.Date;

import org.projectforge.business.fibu.kost.BuchungssatzDO;
import org.projectforge.business.fibu.kost.SHType;
import org.projectforge.common.StringHelper;
import org.projectforge.reporting.Buchungssatz;
import org.projectforge.reporting.Konto;
import org.projectforge.reporting.Kost1;
import org.projectforge.reporting.Kost2;


/**
 * Proxy for BuchungssatzDO;
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see BuchungssatzDO
 */
public class BuchungssatzImpl implements Buchungssatz
{
  private BuchungssatzDO buchungssatz;

  private Konto konto;

  private Konto gegenKonto;

  private Kost1 kost1;

  private Kost2 kost2;

  public BuchungssatzImpl(BuchungssatzDO buchungssatz)
  {
    this.buchungssatz = buchungssatz;
    this.konto = new KontoImpl(this.buchungssatz.getKonto());
    this.gegenKonto = new KontoImpl(this.buchungssatz.getGegenKonto());
    this.kost1 = new Kost1Impl(this.buchungssatz.getKost1());
    this.kost2 = new Kost2Impl(this.buchungssatz.getKost2());
  }
  
  public Integer getId()
  {
    return buchungssatz.getId();
  }

  public String getBeleg()
  {
    return buchungssatz.getBeleg();
  }

  public BigDecimal getBetrag()
  {
    return buchungssatz.getBetrag();
  }

  public String getComment()
  {
    return buchungssatz.getComment();
  }

  public Date getDatum()
  {
    return buchungssatz.getDatum();
  }

  public Konto getGegenKonto()
  {
    return gegenKonto;
  }

  public Konto getKonto()
  {
    return konto;
  }

  public Kost1 getKost1()
  {
    return kost1;
  }

  public Kost2 getKost2()
  {
    return kost2;
  }

  public String getMenge()
  {
    return buchungssatz.getMenge();
  }

  public Integer getMonth()
  {
    return buchungssatz.getMonth();
  }
  
  public String getFormattedMonth()
  {
    return StringHelper.format2DigitNumber(buchungssatz.getMonth() + 1);
  }

  public Integer getSatznr()
  {
    return buchungssatz.getSatznr();
  }

  public SHType getSh()
  {
    return buchungssatz.getSh();
  }

  public String getText()
  {
    return buchungssatz.getText();
  }

  public Integer getYear()
  {
    return buchungssatz.getYear();
  }
}
