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

package org.projectforge.business.address;

import java.io.Serializable;

import org.projectforge.framework.persistence.api.BaseSearchFilter;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
public class AddressbookFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = 7410573665085873058L;

  public enum OwnerType
  {
    ALL, OWN, OTHERS, ADMIN
  }

  private boolean fullAccess, readonlyAccess, minimalAccess, adminAccess;

  protected OwnerType abOwner;

  public AddressbookFilter()
  {
    this(null);
  }

  public AddressbookFilter(final BaseSearchFilter filter)
  {
    super(filter);
    fullAccess = readonlyAccess = minimalAccess = true;
    abOwner = OwnerType.ALL;
  }

  /**
   * @return the filterType
   */
  public OwnerType getOwnerType()
  {
    return abOwner;
  }

  /**
   * @param calOwner the filterType to set
   * @return this for chaining.
   */
  public AddressbookFilter setOwnerType(final OwnerType calOwner)
  {
    this.abOwner = calOwner;
    return this;
  }

  /**
   * @return true if calOwner == {@link OwnerType#ADMIN}
   */
  public boolean isAdmin()
  {
    return abOwner == OwnerType.ADMIN;
  }

  /**
   * @return true if calOwner == {@link OwnerType#OWN}
   */
  public boolean isAll()
  {
    return abOwner == OwnerType.ALL;
  }

  /**
   * @return true if calOwner == {@link OwnerType#OWN}
   */
  public boolean isOwn()
  {
    return abOwner == OwnerType.OWN;
  }

  /**
   * @return true if calOwner == {@link OwnerType#OTHERS}
   */
  public boolean isOthers()
  {
    return abOwner == OwnerType.OTHERS;
  }

  /**
   * @return the readonlyAccess
   */
  public boolean isReadonlyAccess()
  {
    return readonlyAccess;
  }

  /**
   * @param readonlyAccess the readOnlyAccess to set
   * @return this for chaining.
   */
  public AddressbookFilter setReadonlyAccess(final boolean readonlyAccess)
  {
    this.readonlyAccess = readonlyAccess;
    return this;
  }

  /**
   * @return the minimalAccess
   */
  public boolean isMinimalAccess()
  {
    return minimalAccess;
  }

  /**
   * @param minimalAccess the minimalAccess to set
   * @return this for chaining.
   */
  public AddressbookFilter setMinimalAccess(final boolean minimalAccess)
  {
    this.minimalAccess = minimalAccess;
    return this;
  }

  /**
   * @return the fullAccess
   */
  public boolean isFullAccess()
  {
    return fullAccess;
  }

  /**
   * @param fullAccess the fullAccess to set
   * @return this for chaining.
   */
  public AddressbookFilter setFullAccess(final boolean fullAccess)
  {
    this.fullAccess = fullAccess;
    return this;
  }
}
