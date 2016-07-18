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

package org.projectforge.rest.objects;

import java.lang.reflect.Field;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.projectforge.rest.AbstractBaseObject;

/**
 * For documentation please refer the ProjectForge-API: TeamCalDO object.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class CalendarObject extends AbstractBaseObject
{
  private String title;

  private boolean owner, fullAccess, readonlyAccess, minimalAccess;

  private String description;

  private boolean externalSubscription;

  public String getTitle()
  {
    return title;
  }

  public CalendarObject setTitle(final String title)
  {
    this.title = title;
    return this;
  }

  public boolean isOwner()
  {
    return owner;
  }

  public CalendarObject setOwner(final boolean owner)
  {
    this.owner = owner;
    return this;
  }

  public boolean isFullAccess()
  {
    return fullAccess;
  }

  public CalendarObject setFullAccess(final boolean fullAccess)
  {
    this.fullAccess = fullAccess;
    return this;
  }

  public boolean isReadonlyAccess()
  {
    return readonlyAccess;
  }

  public CalendarObject setReadonlyAccess(final boolean readonlyAccess)
  {
    this.readonlyAccess = readonlyAccess;
    return this;
  }

  public boolean isMinimalAccess()
  {
    return minimalAccess;
  }

  public CalendarObject setMinimalAccess(final boolean minimalAccess)
  {
    this.minimalAccess = minimalAccess;
    return this;
  }

  public String getDescription()
  {
    return description;
  }

  public CalendarObject setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  public boolean isExternalSubscription()
  {
    return externalSubscription;
  }

  public CalendarObject setExternalSubscription(final boolean externalSubscription)
  {
    this.externalSubscription = externalSubscription;
    return this;
  }

  @Override
  public String toString()
  {
    return new ReflectionToStringBuilder(this) {
      @Override
      protected boolean accept(final Field f)
      {
        return super.accept(f);
      }
    }.toString();
  }
}
