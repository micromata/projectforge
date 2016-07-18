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

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.projectforge.common.StringHelper;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.utils.Constants;

/**
 * Das monatliche Gehalt eines festangestellten Mitarbeiters.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_FIBU_EMPLOYEE_SALARY",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "employee_id", "year", "month" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_fibu_employee_salary_employee_id", columnList = "employee_id"),
        @javax.persistence.Index(name = "idx_fk_t_fibu_employee_salary_tenant_id", columnList = "tenant_id")
    })
public class EmployeeSalaryDO extends DefaultBaseDO
{
  private static final long serialVersionUID = -6854150096887750382L;

  @PropertyInfo(i18nKey = "fibu.employee")
  @IndexedEmbedded(depth = 2)
  private EmployeeDO employee;

  @PropertyInfo(i18nKey = "calendar.year")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  private Integer year;

  @PropertyInfo(i18nKey = "calendar.month")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  private Integer month;

  @PropertyInfo(i18nKey = "fibu.employee.salary.bruttoMitAgAnteil")
  private BigDecimal bruttoMitAgAnteil;

  @PropertyInfo(i18nKey = "comment")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String comment;

  @PropertyInfo(i18nKey = "fibu.employee.salary.type")
  private EmployeeSalaryType type;

  /**
   * @return Zugehöriger Mitarbeiter.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id", nullable = false)
  public EmployeeDO getEmployee()
  {
    return employee;
  }

  public void setEmployee(final EmployeeDO employee)
  {
    this.employee = employee;
  }

  @Transient
  public Integer getEmployeeId()
  {
    if (this.employee == null)
      return null;
    return employee.getId();
  }

  /**
   * @return Abrechnungsjahr.
   */
  @Column
  public Integer getYear()
  {
    return year;
  }

  public void setYear(final Integer year)
  {
    this.year = year;
  }

  /**
   * @return Abrechnungsmonat.
   */
  @Column
  public Integer getMonth()
  {
    return month;
  }

  public void setMonth(final Integer month)
  {
    this.month = month;
  }

  @Transient
  public String getFormattedMonth()
  {
    return StringHelper.format2DigitNumber(month + 1);
  }

  @Transient
  public String getFormattedYearAndMonth()
  {
    return String.valueOf(year) + "-" + StringHelper.format2DigitNumber(month + 1);
  }

  /**
   * Die Bruttoauszahlung an den Arbeitnehmer (inklusive AG-Anteil Sozialversicherungen).
   */
  @Column(name = "brutto_mit_ag_anteil", scale = 2, precision = 12)
  public BigDecimal getBruttoMitAgAnteil()
  {
    return bruttoMitAgAnteil;
  }

  public void setBruttoMitAgAnteil(final BigDecimal bruttoMitAgAnteil)
  {
    this.bruttoMitAgAnteil = bruttoMitAgAnteil;
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

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  public EmployeeSalaryType getType()
  {
    return type;
  }

  public void setType(final EmployeeSalaryType type)
  {
    this.type = type;
  }
}
