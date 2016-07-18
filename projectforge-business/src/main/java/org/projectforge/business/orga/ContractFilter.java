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

import org.projectforge.framework.persistence.api.BaseSearchFilter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XStreamAlias("ContractFilter")
public class ContractFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = -1220862880530785057L;

  protected int year;

  protected ContractStatus status;

  protected ContractType type;

  public ContractFilter()
  {
  }

  public ContractFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseSearchFilter#reset()
   */
  @Override
  public ContractFilter reset()
  {
    super.reset();
    year = -1;
    status = null;
    return this;
  }

  /**
   * Year of contracts to filter. "<= 0" means showing all years.
   * @return
   */
  public int getYear()
  {
    return year;
  }

  public ContractFilter setYear(final int year)
  {
    this.year = year;
    return this;
  }

  /**
   * @return the status
   */
  public ContractStatus getStatus()
  {
    return status;
  }

  /**
   * @param status the status to set
   * @return this for chaining.
   */
  public ContractFilter setStatus(final ContractStatus status)
  {
    this.status = status;
    return this;
  }

  /**
   * @return the type
   */
  public ContractType getType()
  {
    return type;
  }

  /**
   * @param type the type to set
   * @return this for chaining.
   */
  public ContractFilter setType(final ContractType type)
  {
    this.type = type;
    return this;
  }
}
