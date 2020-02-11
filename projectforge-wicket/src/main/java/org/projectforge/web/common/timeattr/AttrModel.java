/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.common.timeattr;

import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;
import org.apache.wicket.model.IObjectClassAwareModel;

/**
 * Wrapps an Model for an Attr.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
public class AttrModel<T> implements IObjectClassAwareModel<T>
{
  private static final long serialVersionUID = -3315148127877191137L;

  private final EntityWithAttributes entity;

  private final String propertyName;

  private final Class<T> type;

  public AttrModel(final EntityWithAttributes entity, final String propertyName, final Class<T> type)
  {
    super();
    this.entity = entity;
    this.propertyName = propertyName;
    this.type = type;
  }

  /**
   * @see org.apache.wicket.model.IDetachable#detach()
   */
  @Override
  public void detach()
  {

  }

  /**
   * @see org.apache.wicket.model.IModel#getObject()
   */
  @Override
  public T getObject()
  {
    return entity.getAttribute(propertyName, type);

  }

  /**
   * @see org.apache.wicket.model.IModel#setObject(java.lang.Object)
   */
  @Override
  public void setObject(final T object)
  {
    entity.putAttribute(propertyName, object);
  }

  /**
   * @see org.apache.wicket.model.IObjectClassAwareModel#getObjectClass()
   */
  @Override
  public Class<T> getObjectClass()
  {
    return type;
  }

  public EntityWithAttributes getEntity()
  {
    return entity;
  }

}
