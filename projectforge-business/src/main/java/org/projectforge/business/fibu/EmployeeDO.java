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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.common.anots.StringAlphanumericSort;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO;
import org.projectforge.framework.persistence.attr.impl.HibernateSearchAttrSchemaFieldInfoProvider;
import org.projectforge.framework.persistence.history.ToStringFieldBridge;
import org.projectforge.framework.persistence.jpa.impl.BaseDaoJpaAdapter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.Constants;

import de.micromata.genome.db.jpa.history.api.HistoryProperty;
import de.micromata.genome.db.jpa.history.impl.TabAttrHistoryPropertyConverter;
import de.micromata.genome.db.jpa.history.impl.TimependingHistoryPropertyConverter;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithConfigurableAttr;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;
import de.micromata.genome.jpa.ComplexEntity;
import de.micromata.genome.jpa.ComplexEntityVisitor;
import de.micromata.mgc.jpa.hibernatesearch.api.HibernateSearchInfo;
import de.micromata.mgc.jpa.hibernatesearch.bridges.TimeableListFieldBridge;

/**
 * Repräsentiert einen Mitarbeiter. Ein Mitarbeiter ist einem ProjectForge-Benutzer zugeordnet und enthält
 * buchhalterische Angaben.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@HibernateSearchInfo(fieldInfoProvider = HibernateSearchAttrSchemaFieldInfoProvider.class, param = "employee")
@Table(name = "t_fibu_employee",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "tenant_id" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_fibu_employee_kost1_id", columnList = "kost1_id"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_employee_user_id", columnList = "user_id"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_employee_tenant_id", columnList = "tenant_id")
    })
@AUserRightId("HR_EMPLOYEE")
public class EmployeeDO extends DefaultBaseWithAttrDO<EmployeeDO>
    implements EntityWithTimeableAttr<Integer, EmployeeTimedDO>, ComplexEntity, EntityWithConfigurableAttr
{

  private static final long serialVersionUID = -1208597049289694757L;
  private static final Logger LOG = Logger.getLogger(EmployeeDO.class);

  @PropertyInfo(i18nKey = "fibu.employee.user")
  @IndexedEmbedded(depth = 1, includePaths = { "firstname", "lastname", "description", "organization" })
  private PFUserDO user;

  @PropertyInfo(i18nKey = "fibu.kost1")
  @IndexedEmbedded(depth = 1)
  private Kost1DO kost1;

  @PropertyInfo(i18nKey = "status")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private EmployeeStatus status;

  @PropertyInfo(i18nKey = "address.positionText")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String position;

  @PropertyInfo(i18nKey = "fibu.employee.eintrittsdatum")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date eintrittsDatum;

  @PropertyInfo(i18nKey = "fibu.employee.austrittsdatum")
  @Field(index = Index.YES, analyze = Analyze.NO/* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date austrittsDatum;

  @PropertyInfo(i18nKey = "fibu.employee.division")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String abteilung;

  @PropertyInfo(i18nKey = "fibu.employee.staffNumber")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  @StringAlphanumericSort
  private String staffNumber;

  @PropertyInfo(i18nKey = "fibu.employee.urlaubstage")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  @FieldBridge(impl = ToStringFieldBridge.class)
  private Integer urlaubstage;

  @Field(store = Store.YES)
  @FieldBridge(impl = TimeableListFieldBridge.class)
  @IndexedEmbedded(depth = 2)
  private List<EmployeeTimedDO> timeableAttributes = new ArrayList<>();

  @PropertyInfo(i18nKey = "fibu.employee.wochenstunden")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  @FieldBridge(impl = ToStringFieldBridge.class)
  private BigDecimal weeklyWorkingHours;

  @PropertyInfo(i18nKey = "fibu.employee.birthday")
  @Field(index = Index.YES, analyze = Analyze.NO)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date birthday;

  @PropertyInfo(i18nKey = "fibu.employee.accountHolder")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String accountHolder;

  @PropertyInfo(i18nKey = "fibu.employee.iban")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String iban;

  @PropertyInfo(i18nKey = "fibu.employee.bic")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String bic;

  @PropertyInfo(i18nKey = "gender")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  @Convert(converter = GenderConverter.class) // use the GenderConverter instead of @Enumerated to persist the correct ISO/IEC 5218 integer representation of the gender
  private Gender gender;

  @PropertyInfo(i18nKey = "fibu.employee.street")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String street;

  @PropertyInfo(i18nKey = "fibu.employee.zipCode")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String zipCode;

  @PropertyInfo(i18nKey = "fibu.employee.city")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String city;

  @PropertyInfo(i18nKey = "fibu.employee.country")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String country;

  @PropertyInfo(i18nKey = "fibu.employee.state")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String state;

  @PropertyInfo(i18nKey = "comment")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String comment;

  @Override
  public ModificationStatus copyValuesFrom(final BaseDO<? extends Serializable> source, final String... ignoreFields)
  {
    ModificationStatus modificationStatus = super.copyValuesFrom(source, "timeableAttributes");
    final EmployeeDO src = (EmployeeDO) source;
    modificationStatus = modificationStatus
        .combine(BaseDaoJpaAdapter.copyTimeableAttribute(this, src));
    return modificationStatus;
  }

  @Override
  public void visit(ComplexEntityVisitor visitor)
  {
    super.visit(visitor);
    for (EmployeeTimedDO et : timeableAttributes) {
      et.visit(visitor);
    }
  }

  @Override
  @Transient
  public String getAttrSchemaName()
  {
    return "employee";
  }

  @Override
  public void addTimeableAttribute(EmployeeTimedDO row)
  {
    row.setEmployee(this);
    timeableAttributes.add(row);
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "employee_status", length = 30)
  public EmployeeStatus getStatus()
  {
    return status;
  }

  public void setStatus(final EmployeeStatus status)
  {
    this.status = status;
  }

  /**
   * Dem Benutzer zugeordneter Kostenträger Kost1 für den Monatsreport.
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "kost1_id", nullable = true)
  public Kost1DO getKost1()
  {
    return kost1;
  }

  public void setKost1(final Kost1DO kost1)
  {
    this.kost1 = kost1;
  }

  @Transient
  public Integer getKost1Id()
  {
    if (this.kost1 == null) {
      return null;
    }
    return kost1.getId();
  }

  /**
   * The ProjectForge user assigned to this employee.
   *
   * @return the user
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", nullable = false)
  public PFUserDO getUser()
  {
    return user;
  }

  /**
   * @param user the user to set
   */
  public void setUser(final PFUserDO user)
  {
    this.user = user;
  }

  @Transient
  public Integer getUserId()
  {
    if (this.user == null) {
      return null;
    }
    return user.getId();
  }

  @Column
  public Integer getUrlaubstage()
  {
    return urlaubstage;
  }

  public void setUrlaubstage(final Integer urlaubstage)
  {
    this.urlaubstage = urlaubstage;
  }

  @Column(name = "weekly_working_hours", scale = 5, precision = 10)
  public BigDecimal getWeeklyWorkingHours()
  {
    return weeklyWorkingHours;
  }

  public void setWeeklyWorkingHours(final BigDecimal weeklyWorkingHours)
  {
    this.weeklyWorkingHours = weeklyWorkingHours;
  }

  @Column
  public Date getBirthday()
  {
    return birthday;
  }

  public void setBirthday(final Date birthday)
  {
    this.birthday = birthday;
  }

  @Column(length = 255, name = "account_holder")
  public String getAccountHolder()
  {
    return accountHolder;
  }

  public void setAccountHolder(String accountHolder)
  {
    this.accountHolder = accountHolder;
  }

  @Column(length = 50)
  public String getIban()
  {
    return iban;
  }

  public void setIban(String iban)
  {
    this.iban = iban;
  }

  @Column(length = 11)
  public String getBic()
  {
    return bic;
  }

  public void setBic(String bic)
  {
    this.bic = bic;
  }

  @Column
  public Gender getGender()
  {
    return gender;
  }

  public void setGender(Gender gender)
  {
    this.gender = gender;
  }

  @Column(length = 255)
  public String getStreet()
  {
    return street;
  }

  public void setStreet(String street)
  {
    this.street = street;
  }

  @Column(length = 255)
  public String getZipCode()
  {
    return zipCode;
  }

  public void setZipCode(String zipCode)
  {
    this.zipCode = zipCode;
  }

  @Column(length = 255)
  public String getCity()
  {
    return city;
  }

  public void setCity(String city)
  {
    this.city = city;
  }

  @Column(length = 255)
  public String getCountry()
  {
    return country;
  }

  public void setCountry(String country)
  {
    this.country = country;
  }

  @Column(length = 255)
  public String getState()
  {
    return state;
  }

  public void setState(String state)
  {
    this.state = state;
  }

  @Column(length = 255)
  public String getStaffNumber()
  {
    return staffNumber;
  }

  public void setStaffNumber(String staffNumber)
  {
    this.staffNumber = staffNumber;
  }

  @Column(name = "eintritt")
  public Date getEintrittsDatum()
  {
    return eintrittsDatum;
  }

  public void setEintrittsDatum(final Date eintrittsDatum)
  {
    this.eintrittsDatum = eintrittsDatum;
  }

  @Column(name = "austritt")
  public Date getAustrittsDatum()
  {
    return austrittsDatum;
  }

  public void setAustrittsDatum(final Date austrittsDatum)
  {
    this.austrittsDatum = austrittsDatum;
  }

  @Column(name = "position_text", length = 244)
  public String getPosition()
  {
    return position;
  }

  public void setPosition(final String position)
  {
    this.position = position;
  }

  @Column(length = Constants.COMMENT_LENGTH)
  public String getComment()
  {
    return comment;
  }

  public void setComment(final String comment)
  {
    this.comment = comment;
  }

  @Column(length = 255)
  public String getAbteilung()
  {
    return abteilung;
  }

  public void setAbteilung(final String abteilung)
  {
    this.abteilung = abteilung;
  }

  @Override
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "employee")
  // fetch mode, only returns one
  @Fetch(FetchMode.SELECT)
  // unfortunatelly this does work. Date is not valid for order (only integral types)
  //  @OrderColumn(name = "startTime")
  @HistoryProperty(converter = TimependingHistoryPropertyConverter.class)
  public List<EmployeeTimedDO> getTimeableAttributes()
  {
    return timeableAttributes;
  }

  public void setTimeableAttributes(final List<EmployeeTimedDO> timeableAttributes)
  {
    this.timeableAttributes = timeableAttributes;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<EmployeeDO, Integer>> getAttrEntityClass()
  {
    return EmployeeAttrDO.class;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<EmployeeDO, Integer>> getAttrEntityWithDataClass()
  {
    return EmployeeAttrWithDataDO.class;
  }

  @Override
  @Transient
  public Class<? extends JpaTabAttrDataBaseDO<? extends JpaTabAttrBaseDO<EmployeeDO, Integer>, Integer>> getAttrDataEntityClass()
  {
    return EmployeeAttrDataDO.class;
  }

  @Override
  public JpaTabAttrBaseDO<EmployeeDO, Integer> createAttrEntity(String key, char type, String value)
  {
    return new EmployeeAttrDO(this, key, type, value);
  }

  @Override
  public JpaTabAttrBaseDO<EmployeeDO, Integer> createAttrEntityWithData(String key, char type, String value)
  {
    return new EmployeeAttrWithDataDO(this, key, type, value);
  }

  @Override
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", targetEntity = EmployeeAttrDO.class, orphanRemoval = true,
      fetch = FetchType.EAGER)
  @MapKey(name = "propertyName")
  @HistoryProperty(converter = TabAttrHistoryPropertyConverter.class)
  public Map<String, JpaTabAttrBaseDO<EmployeeDO, Integer>> getAttrs()
  {
    return super.getAttrs();
  }

}
