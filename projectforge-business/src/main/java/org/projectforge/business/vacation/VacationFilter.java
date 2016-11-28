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

package org.projectforge.business.vacation;

import java.io.Serializable;

import org.projectforge.business.vacation.model.VacationMode;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.framework.persistence.api.BaseSearchFilter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Florian Blumenstein
 */
@XStreamAlias("VacationFilter")
public class VacationFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = 8567712340637887786L;

  private Integer employeeId;

  private VacationStatus vacationstatus;

  private VacationMode vacationmode;

  public VacationFilter(Integer employeeId)
  {
    this.employeeId = employeeId;
  }

  public VacationFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  public Integer getEmployeeId()
  {
    return employeeId;
  }

  public void setEmployeeId(Integer employeeId)
  {
    this.employeeId = employeeId;
  }

  public VacationStatus getVacationstatus()
  {
    return vacationstatus;
  }

  public void setVacationstatus(VacationStatus vacationstatus)
  {
    this.vacationstatus = vacationstatus;
  }

  public VacationMode getVacationmode()
  {
    return vacationmode;
  }

  public void setVacationmode(VacationMode vacationmode)
  {
    this.vacationmode = vacationmode;
  }

  @Override
  public BaseSearchFilter reset()
  {
    super.reset();
    this.vacationstatus = null;
    this.vacationmode = null;
    return this;
  }
}
