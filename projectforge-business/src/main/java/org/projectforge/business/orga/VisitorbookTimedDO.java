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

package org.projectforge.business.orga;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.api.IdObject;

import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.TimeableBaseDO;

@Entity
@Indexed
@Table(name = "t_orga_visitorbook_timed",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "visitor_id", "group_name", "start_time" })
    },
    indexes = {
        @Index(name = "idx_orga_visitorbook_timed_start_time", columnList = "start_time")
    })
public class VisitorbookTimedDO extends TimeableBaseDO<VisitorbookTimedDO, Integer>
    implements TimeableAttrRow<Integer>, IdObject<Integer>
{

  @PropertyInfo(i18nKey = "orga.visitorbook")
  @IndexedEmbedded(depth = 2)
  private VisitorbookDO visitor;

  @Id
  @GeneratedValue
  @Column(name = "pk")
  @Override
  public Integer getPk()
  {
    return pk;
  }

  @Override
  @Transient
  public Integer getId()
  {
    return pk;
  }

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", targetEntity = VisitorbookTimedAttrDO.class,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @MapKey(name = "propertyName")
  @Override
  public Map<String, JpaTabAttrBaseDO<VisitorbookTimedDO, Integer>> getAttributes()
  {
    return super.getAttributes();
  }

  /**
   * @return Zugehöriger Mitarbeiter.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "visitor_id", nullable = false)
  public VisitorbookDO getVisitor()
  {
    return visitor;
  }

  public void setVisitor(final VisitorbookDO visitor)
  {
    this.visitor = visitor;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<VisitorbookTimedDO, ? extends Serializable>> getAttrEntityClass()
  {
    return VisitorbookTimedAttrDO.class;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<VisitorbookTimedDO, ? extends Serializable>> getAttrEntityWithDataClass()
  {
    return VisitorbookTimedAttrWithDataDO.class;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrDataBaseDO<? extends JpaTabAttrBaseDO<VisitorbookTimedDO, Integer>, Integer>> getAttrDataEntityClass()
  {
    return VisitorbookTimedAttrDataDO.class;
  }

  @Override
  public JpaTabAttrBaseDO<VisitorbookTimedDO, Integer> createAttrEntity(String key, char type, String value)
  {
    return new VisitorbookTimedAttrDO(this, key, type, value);
  }

  @Override
  public JpaTabAttrBaseDO<VisitorbookTimedDO, Integer> createAttrEntityWithData(String key, char type, String value)
  {
    return new VisitorbookTimedAttrWithDataDO(this, key, type, value);
  }

}
