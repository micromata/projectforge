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
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import de.micromata.genome.db.jpa.history.entities.HistoryAttrBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;
import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist;

/**
 * JPA entity for History.
 *
 * @author roger
 *
 */

@Entity
@Table(name = "t_pf_history_attr", indexes = {
    @Index(name = "ix_pf_history_attr_mod", columnList = "MODIFIEDAT"),
    @Index(name = "ix_pf_history_attr_masterpk", columnList = "MASTER_FK"),

})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "WITHDATA", discriminatorType = DiscriminatorType.CHAR)
@DiscriminatorValue("0")
@JpaXmlPersist(noStore = true)
public class PfHistoryAttrDO extends HistoryAttrBaseDO<PfHistoryMasterDO, Long>
{

  /**
   * The Constant serialVersionUID.
   */
  private static final long serialVersionUID = -5490342158738541970L;

  /**
   * Instantiates a new history attr do.
   */
  public PfHistoryAttrDO()
  {

  }

  /**
   * Instantiates a new history attr do.
   *
   * @param parent the parent
   */
  public PfHistoryAttrDO(PfHistoryMasterDO parent)
  {
    super(parent);
  }

  /**
   * Instantiates a new history attr do.
   *
   * @param parent the parent
   * @param propertyName the property name
   * @param type the type
   * @param value the value
   */
  public PfHistoryAttrDO(PfHistoryMasterDO parent, String propertyName, char type, String value)
  {
    super(parent, propertyName, type, value);
  }

  @Override
  public JpaTabAttrDataBaseDO<?, Long> createData(String data)
  {
    return new PfHistoryAttrDataDO(this, data);
  }

  @Override
  @Transient
  public int getMaxDataLength()
  {
    return JpaTabAttrDataBaseDO.DATA_MAXLENGTH;
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
  @JoinColumn(name = "MASTER_FK", referencedColumnName = "pk")
  public PfHistoryMasterDO getParent()
  {
    return super.getParent();
  }

  //  @Field(analyze = Analyze.NO, store = Store.YES)
  @Transient
  @Override
  public String getPropertyTypeClass()
  {
    return super.getPropertyTypeClass();
  }

  //  @Field(analyze = Analyze.NO, store = Store.YES)
  @Transient
  @Override
  public String getPropertyName()
  {
    return super.getPropertyName();
  }

  //  @Field(analyze = Analyze.YES, store = Store.YES)
  @Transient
  @Override
  public String getStringData()
  {
    return super.getStringData();
  }

}
