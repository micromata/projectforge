/////////////////////////////////////////////////////////////////////////////
//
// Project   Micromata Genome 
//
// Author    r.kommer.extern@micromata.de
// Created   18.02.2013
// Copyright Micromata 2013
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
