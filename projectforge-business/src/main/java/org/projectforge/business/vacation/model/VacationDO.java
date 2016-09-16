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

package org.projectforge.business.vacation.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.search.annotations.Indexed;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.attr.impl.HibernateSearchAttrSchemaFieldInfoProvider;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.mgc.jpa.hibernatesearch.api.HibernateSearchInfo;

/**
 * Repräsentiert einen Urlaub. Ein Urlaub ist einem ProjectForge-Mitarbeiter zugeordnet und enthält buchhalterische
 * Angaben.
 *
 * @author Florian Blumenstein
 */
@Entity
@Indexed
@HibernateSearchInfo(fieldInfoProvider = HibernateSearchAttrSchemaFieldInfoProvider.class, param = "vacation")
@Table(name = "t_vacation",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "employee_id", "tenant_id" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_vacation_employee_id", columnList = "employee_id"),
        @javax.persistence.Index(name = "idx_fk_t_vacation_tenant_id", columnList = "tenant_id")
    })
@AUserRightId("VACATION")
public class VacationDO extends DefaultBaseDO
{
  private static final long serialVersionUID = -1208597049212394757L;

  @PropertyInfo(i18nKey = "vacation.employee")
  private EmployeeDO employee;

  @PropertyInfo(i18nKey = "vacation.startdate")
  private Date startDate;

  @PropertyInfo(i18nKey = "vacation.enddate")
  private Date endDate;

  @PropertyInfo(i18nKey = "vacation.substitution")
  private EmployeeDO substitution;

  @PropertyInfo(i18nKey = "vacation.manager")
  private EmployeeDO manager;

  /**
   * The employee.
   *
   * @return the user
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "employee_id", nullable = false)
  public EmployeeDO getEmployee()
  {
    return employee;
  }

  /**
   * @param employee the employee to set
   */
  public void setEmployee(final EmployeeDO employee)
  {
    this.employee = employee;
  }

  /**
   * The substitution.
   *
   * @return the substitution
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "substitution_id", nullable = false)
  public EmployeeDO getSubstitution()
  {
    return substitution;
  }

  /**
   * @param substitution the substitution to set
   */
  public void setSubstitution(final EmployeeDO substitution)
  {
    this.substitution = substitution;
  }

  /**
   * The manager.
   *
   * @return the manager
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "manager_id", nullable = false)
  public EmployeeDO getManager()
  {
    return manager;
  }

  /**
   * @param manager the manager to set
   */
  public void setManager(final EmployeeDO manager)
  {
    this.manager = manager;
  }

  @Temporal(TemporalType.DATE)
  @Column(name = "start_date")
  public Date getStartDate()
  {
    return startDate;
  }

  public void setStartDate(Date startDate)
  {
    this.startDate = startDate;
  }

  @Temporal(TemporalType.DATE)
  @Column(name = "end_date")
  public Date getEndDate()
  {
    return endDate;
  }

  public void setEndDate(Date endDate)
  {
    this.endDate = endDate;
  }

}
