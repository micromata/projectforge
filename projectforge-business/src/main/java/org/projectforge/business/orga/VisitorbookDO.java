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

package org.projectforge.business.orga;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.jpa.impl.BaseDaoJpaAdapter;

import de.micromata.genome.db.jpa.history.api.HistoryProperty;
import de.micromata.genome.db.jpa.history.impl.TimependingHistoryPropertyConverter;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithConfigurableAttr;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.jpa.ComplexEntity;
import de.micromata.genome.jpa.ComplexEntityVisitor;
import de.micromata.mgc.jpa.hibernatesearch.bridges.TimeableListFieldBridge;

@Entity
@Indexed
@Table(name = "t_orga_visitorbook", indexes = { @javax.persistence.Index(name = "idx_fk_t_orga_visitorbook_tenant_id", columnList = "tenant_id") })
@AUserRightId("ORGA_VISITORBOOK")
public class VisitorbookDO extends DefaultBaseDO implements EntityWithTimeableAttr<Integer, VisitorbookTimedDO>, ComplexEntity, EntityWithConfigurableAttr
{
  private static final long serialVersionUID = -1208597049289694757L;

  private static final Logger LOG = Logger.getLogger(VisitorbookDO.class);

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @PropertyInfo(i18nKey = "orga.visitorbook.lastname")
  private String lastname;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @PropertyInfo(i18nKey = "orga.visitorbook.firstname")
  private String firstname;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @PropertyInfo(i18nKey = "orga.visitorbook.company")
  private String company;

  @PropertyInfo(i18nKey = "orga.visitorbook.contactPerson")
  private Set<EmployeeDO> contactPersons;

  @PropertyInfo(i18nKey = "orga.visitorbook.visitortype")
  private VisitorType visitortype;

  @Field(store = Store.YES)
  @FieldBridge(impl = TimeableListFieldBridge.class)
  @IndexedEmbedded(depth = 2)
  private List<VisitorbookTimedDO> timeableAttributes = new ArrayList<>();

  @Column(name = "lastname", length = 30, nullable = false)
  public String getLastname()
  {
    return lastname;
  }

  public void setLastname(String lastname)
  {
    this.lastname = lastname;
  }

  @Column(name = "firstname", length = 30, nullable = false)
  public String getFirstname()
  {
    return firstname;
  }

  public void setFirstname(String firstname)
  {
    this.firstname = firstname;
  }

  @Column(name = "company")
  public String getCompany()
  {
    return company;
  }

  public void setCompany(String company)
  {
    this.company = company;
  }

  @ManyToMany(targetEntity = EmployeeDO.class, cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
  @JoinTable(name = "T_ORGA_VISITORBOOK_EMPLOYEE", joinColumns = @JoinColumn(name = "VISITORBOOK_ID"),
      inverseJoinColumns = @JoinColumn(name = "EMPLOYEE_ID"),
      indexes = {
          @javax.persistence.Index(name = "idx_fk_t_orga_visitorbook_employee_id", columnList = "visitorbook_id"),
          @javax.persistence.Index(name = "idx_fk_t_orga_employee_employee_id", columnList = "employee_id")
      })
  public Set<EmployeeDO> getContactPersons()
  {
    return contactPersons;
  }

  public void setContactPersons(Set<EmployeeDO> contactPersons)
  {
    this.contactPersons = contactPersons;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "visitor_type", nullable = false)
  public VisitorType getVisitortype()
  {
    return visitortype;
  }

  public void setVisitortype(VisitorType visitortype)
  {
    this.visitortype = visitortype;
  }

  @Override
  public ModificationStatus copyValuesFrom(final BaseDO<? extends Serializable> source, final String... ignoreFields)
  {
    ModificationStatus modificationStatus = super.copyValuesFrom(source, "timeableAttributes");
    final VisitorbookDO src = (VisitorbookDO) source;
    modificationStatus = modificationStatus
        .combine(BaseDaoJpaAdapter.copyTimeableAttribute(this, src));
    return modificationStatus;
  }

  @Override
  public void visit(ComplexEntityVisitor visitor)
  {
    for (VisitorbookTimedDO et : timeableAttributes) {
      et.visit(visitor);
    }
  }

  @Override
  @Transient
  public String getAttrSchemaName()
  {
    return "visitorbook";
  }

  @Override
  public void addTimeableAttribute(VisitorbookTimedDO row)
  {
    row.setVisitor(this);
    timeableAttributes.add(row);
  }

  @Override
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "visitor")
  @Fetch(FetchMode.SELECT)
  @HistoryProperty(converter = TimependingHistoryPropertyConverter.class)
  public List<VisitorbookTimedDO> getTimeableAttributes()
  {
    return timeableAttributes;
  }

  public void setTimeableAttributes(final List<VisitorbookTimedDO> timeableAttributes)
  {
    this.timeableAttributes = timeableAttributes;
  }

}
