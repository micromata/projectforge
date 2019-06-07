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

package org.projectforge.framework.persistence.history.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;
import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist;

/**
 * JPA entity for TB_TA_GATTR_DATA.
 *
 * @author roger
 */

@Entity
@Table(name = "t_pf_history_attr_data",
    indexes = {
        @Index(name = "IX_pf_HISTORY_A_D_MODAT", columnList = "MODIFIEDAT"),
        @Index(name = "IX_pf_HISTORY_A_D_PARENT", columnList = "PARENT_PK")
    })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@JpaXmlPersist(noStore = true)
public class PfHistoryAttrDataDO extends JpaTabAttrDataBaseDO<PfHistoryAttrDO, Long>
{

  /**
   * The Constant serialVersionUID.
   */
  private static final long serialVersionUID = -3845387843789907008L;

  /**
   * Instantiates a new history attr data do.
   */
  public PfHistoryAttrDataDO()
  {

  }

  /**
   * Instantiates a new history attr data do.
   *
   * @param parent the parent
   */
  public PfHistoryAttrDataDO(PfHistoryAttrDO parent)
  {
    super(parent);
  }

  /**
   * Instantiates a new history attr data do.
   *
   * @param parent the parent
   * @param value the value
   */
  public PfHistoryAttrDataDO(PfHistoryAttrDO parent, String value)
  {
    super(parent, value);
  }

  @Override
  @Id
  @Column(name = "pk")
  @GeneratedValue()
  public Long getPk()
  {
    return pk;
  }

  @Override
  @ManyToOne(optional = false)
  @JoinColumn(name = "PARENT_PK", referencedColumnName = "pk")
  public PfHistoryAttrDO getParent()
  {
    return super.getParent();
  }
}
