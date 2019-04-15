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

package org.projectforge.business.book;

import java.io.Serializable;

import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.api.BaseSearchFilter;


/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class BookFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = -4397408137924906520L;

  @PropertyInfo(i18nKey = "book.status.present")
  private boolean present = true;

  @PropertyInfo(i18nKey = "book.status.missed")
  private boolean missed = false;

  @PropertyInfo(i18nKey = "book.status.disposed")
  private boolean disposed = false;

  public BookFilter()
  {
  }

  public BookFilter(final BaseSearchFilter filter)
  {
    super(filter);
    missed = disposed = true;
  }

  @Override
  public BookFilter reset()
  {
    super.reset();
    setPresent(true);
    setMissed(false);
    setDisposed(false);
    return this;
  }

  public boolean isPresent()
  {
    return present;
  }
  
  public void setPresent(boolean present)
  {
    this.present = present;
  }
  
  public boolean isMissed()
  {
    return missed;
  }
  
  public void setMissed(boolean missed)
  {
    this.missed = missed;
  }
  
  public boolean isDisposed()
  {
    return disposed;
  }
  
  public void setDisposed(boolean disposed)
  {
    this.disposed = disposed;
  }
}
