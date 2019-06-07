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

import java.util.Objects;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Repräsentiert einee Position innerhalb eines Auftrags als Übersichtsobject (value object) zur Verwendung z. B. im TaskTree.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class AuftragsPositionVO implements Comparable<AuftragsPositionVO>, Serializable
{
  private static final long serialVersionUID = 5425767060569257885L;

  private short number;

  private Integer auftragId;

  private Integer auftragNummer;

  private String auftragTitle;

  private AuftragsStatus auftragsStatus;

  private BigDecimal auftragsPersonDays;

  private Integer taskId;

  private AuftragsPositionsArt art;

  private AuftragsPositionsStatus status;

  private String titel;

  private BigDecimal nettoSumme;

  private BigDecimal personDays;

  private boolean vollstaendigFakturiert;

  public AuftragsPositionVO(final AuftragsPositionDO auftragsPosition)
  {
    final AuftragDO auftrag = auftragsPosition.getAuftrag();
    this.number = auftragsPosition.getNumber();
    if (auftrag != null) { // Should be always true.
      this.auftragId = auftrag.getId();
      this.auftragNummer = auftrag.getNummer();
      this.auftragTitle = auftrag.getTitel();
      this.auftragsStatus = auftrag.getAuftragsStatus();
      this.auftragsPersonDays = auftrag.getPersonDays();
    }
    this.taskId = auftragsPosition.getTaskId();
    this.art = auftragsPosition.getArt();
    this.status = auftragsPosition.getStatus();
    this.titel = auftragsPosition.getTitel();
    this.nettoSumme = auftragsPosition.getNettoSumme();
    this.personDays = auftragsPosition.getPersonDays();
    if (this.personDays == null) {
      this.personDays = BigDecimal.ZERO;
    }
    this.vollstaendigFakturiert = auftragsPosition.getVollstaendigFakturiert();
  }

  public short getNumber()
  {
    return number;
  }

  public BigDecimal getNettoSumme()
  {
    return nettoSumme;
  }

  public BigDecimal getPersonDays()
  {
    return personDays;
  }

  public AuftragsPositionsStatus getStatus()
  {
    return status;
  }

  public AuftragsPositionsArt getArt()
  {
    return art;
  }

  public String getTitel()
  {
    return titel;
  }

  public boolean isVollstaendigFakturiert()
  {
    return vollstaendigFakturiert;
  }

  public Integer getAuftragId()
  {
    return auftragId;
  }

  public Integer getAuftragNummer()
  {
    return auftragNummer;
  }

  /**
   * @see AuftragDO#getTitel()
   */
  public String getAuftragTitle()
  {
    return auftragTitle;
  }

  /**
   * @see AuftragDO#getAuftragsStatus()
   */
  public AuftragsStatus getAuftragsStatus()
  {
    return auftragsStatus;
  }

  /**
   * @see AuftragDO#getPersonDays()
   */
  public BigDecimal getAuftragsPersonDays()
  {
    return auftragsPersonDays;
  }

  public Integer getTaskId()
  {
    return taskId;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof AuftragsPositionVO) {
      AuftragsPositionVO other = (AuftragsPositionVO) o;
      if (Objects.equals(this.getNumber(), other.getNumber()) == false)
        return false;
      if (Objects.equals(this.getAuftragId(), other.getAuftragId()) == false)
        return false;
      return true;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(getNumber());
    hcb.append(getAuftragId());
    return hcb.toHashCode();
  }

  @Override
  public int compareTo(final AuftragsPositionVO o)
  {
    if (this.auftragNummer.equals(o.auftragNummer) == false) {
      return this.auftragNummer.compareTo(o.auftragNummer);
    }
    if (this.number < o.number) {
      return -1;
    } else if (this.number == o.number) {
      return 0;
    } else {
      return +1;
    }
  }
}
