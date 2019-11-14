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

import de.micromata.genome.db.jpa.history.entities.HistoryMasterBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;
import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist;
import de.micromata.mgc.jpa.hibernatesearch.api.HibernateSearchInfo;
import de.micromata.mgc.jpa.hibernatesearch.bridges.HistoryMasterClassBridge;
import org.hibernate.search.annotations.*;

import javax.persistence.*;
import javax.persistence.Index;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Stores history.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Entity()
@Table(name = "t_pf_history", indexes = {
    @Index(name = "ix_pf_history_ent", columnList = "ENTITY_ID,ENTITY_NAME"),
    @Index(name = "ix_pf_history_mod", columnList = "MODIFIEDAT")
})
@Indexed()
@ClassBridge(impl = HistoryMasterClassBridge.class)
@HibernateSearchInfo(param = "oldValue")
@JpaXmlPersist(beforePersistListener = PfHistoryMasterXmlBeforePersistListener.class)
public class PfHistoryMasterDO extends HistoryMasterBaseDO<PfHistoryMasterDO, Long>
{

  @Override
  @Id
  @Column(name = "pk")
  @GeneratedValue()
  public Long getPk()
  {
    return pk;
  }

  @Field(analyze = Analyze.NO, store = Store.NO)
  @Override
  @Transient
  public String getEntityName()
  {
    return super.getEntityName();
  }

  @Field(analyze = Analyze.NO, store = Store.YES, index = org.hibernate.search.annotations.Index.YES)
  @Transient
  @Override
  public Long getEntityId()
  {
    return super.getEntityId();
  }

  @Field(analyze = Analyze.NO, store = Store.NO, index = org.hibernate.search.annotations.Index.YES)
  @Transient
  @Override
  public String getModifiedBy()
  {
    return super.getModifiedBy();
  }

  @Field(store = Store.NO)
  @DateBridge(resolution = Resolution.MILLISECOND, encoding = EncodingType.STRING)
  @Transient
  @Override
  public Date getModifiedAt()
  {
    return super.getModifiedAt();
  }

  /**
   * Used to mark for full text search
   * 
   * @return
   */
  @Transient
  @Field(analyze = Analyze.YES, store = Store.NO, index = org.hibernate.search.annotations.Index.YES)
  public String getOldValue()
  {
    return "";
  }

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", targetEntity = PfHistoryAttrDO.class, orphanRemoval = true,
      fetch = FetchType.EAGER)
  @MapKey(name = "propertyName")
  @Override
  public Map<String, JpaTabAttrBaseDO<PfHistoryMasterDO, Long>> getAttributes()
  {
    return super.getAttributes();
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<PfHistoryMasterDO, ? extends Serializable>> getAttrEntityClass()
  {
    return PfHistoryAttrDO.class;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<PfHistoryMasterDO, ? extends Serializable>> getAttrEntityWithDataClass()
  {
    return PfHistoryAttrWithDataDO.class;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrDataBaseDO<? extends JpaTabAttrBaseDO<PfHistoryMasterDO, Long>, Long>> getAttrDataEntityClass()
  {
    return PfHistoryAttrDataDO.class;
  }

  @Override
  public JpaTabAttrBaseDO<PfHistoryMasterDO, Long> createAttrEntity(String key, char type, String value)
  {
    return new PfHistoryAttrDO(this, key, type, value);
  }

  @Override
  public JpaTabAttrBaseDO<PfHistoryMasterDO, Long> createAttrEntityWithData(String key, char type, String value)
  {
    return new PfHistoryAttrWithDataDO(this, key, type, value);
  }
}
