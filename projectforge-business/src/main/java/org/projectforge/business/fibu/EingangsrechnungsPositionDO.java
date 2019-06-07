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

import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.hibernate.search.annotations.Indexed;
import org.projectforge.business.fibu.kost.KostZuweisungDO;

import javax.persistence.*;
import java.util.List;

/**
 * Repräsentiert eine Position innerhalb einer Eingangsrechnung.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "t_fibu_eingangsrechnung_position",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "eingangsrechnung_fk", "number" })
    },
    indexes = {
        @Index(name = "idx_fk_t_fibu_eingangsrechnung_position_eingangsrechnung_fk",
            columnList = "eingangsrechnung_fk"),
        @Index(name = "idx_fk_t_fibu_eingangsrechnung_position_tenant_id", columnList = "tenant_id")
    })
public class EingangsrechnungsPositionDO extends AbstractRechnungsPositionDO
{
  private static final long serialVersionUID = -3803069266469066395L;

  private EingangsrechnungDO eingangsrechnung;

  @JsonManagedReference
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "eingangsrechnung_fk", nullable = false)
  public EingangsrechnungDO getEingangsrechnung()
  {
    return eingangsrechnung;
  }

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "eingangsrechnungs_pos_fk")
  @OrderColumn(name = "index")
  @Override
  public List<KostZuweisungDO> getKostZuweisungen()
  {
    return this.kostZuweisungen;
  }

  public EingangsrechnungsPositionDO setEingangsrechnung(final EingangsrechnungDO eingangsrechnung)
  {
    this.eingangsrechnung = eingangsrechnung;
    return this;
  }

  @Transient
  @Override
  protected AbstractRechnungDO<?> getRechnung()
  {
    return getEingangsrechnung();
  }

  @Override
  protected EingangsrechnungsPositionDO setRechnung(final AbstractRechnungDO<?> rechnung)
  {
    setEingangsrechnung((EingangsrechnungDO) rechnung);
    return this;
  }

  @Transient
  @Override
  protected void setThis(KostZuweisungDO kostZuweisung)
  {
    kostZuweisung.setEingangsrechnungsPosition(this);
  }

  @Override
  protected AbstractRechnungsPositionDO newInstance()
  {
    return new EingangsrechnungsPositionDO();
  }
}
