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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

/**
 * Repräsentiert einen Urlaub. Ein Urlaub ist einem ProjectForge-Mitarbeiter zugeordnet und enthält buchhalterische
 * Angaben.
 *
 * @author Florian Blumenstein
 */
@Entity
@Indexed
@Table(name = "t_employee_vacation",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_vacation_employee_id", columnList = "employee_id"),
        @javax.persistence.Index(name = "idx_fk_t_vacation_substitution_id", columnList = "substitution_id"),
        @javax.persistence.Index(name = "idx_fk_t_vacation_manager_id", columnList = "manager_id"),
        @javax.persistence.Index(name = "idx_fk_t_vacation_tenant_id", columnList = "tenant_id")
    })
@AUserRightId(value = "EMPLOYEE_VACATION", checkAccess = false)
public class VacationDO extends DefaultBaseDO
{
  private static final long serialVersionUID = -1208597049212394757L;

  @PropertyInfo(i18nKey = "vacation.employee")
  @IndexedEmbedded(includePaths = { "user.firstname", "user.lastname" })
  private EmployeeDO employee;

  @PropertyInfo(i18nKey = "vacation.startdate")
  private Date startDate;

  @PropertyInfo(i18nKey = "vacation.enddate")
  private Date endDate;

  @PropertyInfo(i18nKey = "vacation.substitution")
  private EmployeeDO substitution;

  @PropertyInfo(i18nKey = "vacation.manager")
  private EmployeeDO manager;

  @PropertyInfo(i18nKey = "vacation.status")
  private VacationStatus status;

  //TODO FB: Wird leider nur über dem Feld ausgewertewt und nicht an der Methode.
  //Feld wird eigentlich nicht benötigt
  @PropertyInfo(i18nKey = "vacation.vacationmode")
  private VacationMode vacationmode;

  @PropertyInfo(i18nKey = "vacation.isSpecial")
  private Boolean isSpecial;

  @PropertyInfo(i18nKey = "vacation.isHalfDay")
  private Boolean halfDay;

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
  @Column(name = "start_date", nullable = false)
  public Date getStartDate()
  {
    return startDate;
  }

  public void setStartDate(Date startDate)
  {
    this.startDate = startDate;
  }

  @Temporal(TemporalType.DATE)
  @Column(name = "end_date", nullable = false)
  public Date getEndDate()
  {
    return endDate;
  }

  public void setEndDate(Date endDate)
  {
    this.endDate = endDate;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "vacation_status", length = 30, nullable = false)
  public VacationStatus getStatus()
  {
    if (status == null) {
      return VacationStatus.IN_PROGRESS;
    }
    return status;
  }

  public void setStatus(final VacationStatus status)
  {
    this.status = status;
  }

  @Transient
  public VacationMode getVacationmode()
  {
    if (ThreadLocalUserContext.getUser().getPk().equals(employee.getUser().getPk())) {
      return VacationMode.OWN;
    }
    if (ThreadLocalUserContext.getUser().getPk().equals(manager.getUser().getPk())) {
      return VacationMode.MANAGER;
    }
    if (ThreadLocalUserContext.getUser().getPk().equals(substitution.getUser().getPk())) {
      return VacationMode.SUBSTITUTION;
    }
    return VacationMode.OTHER;
  }

  @Column(name = "is_special", nullable = false)
  public Boolean getIsSpecial()
  {
    return isSpecial;
  }

  public void setIsSpecial(Boolean special)
  {
    isSpecial = special;
  }

  @Column(name = "is_half_day")
  public Boolean getHalfDay()
  {
    return halfDay;
  }

  public void setHalfDay(final Boolean halfDay)
  {
    this.halfDay = halfDay;
  }
}
