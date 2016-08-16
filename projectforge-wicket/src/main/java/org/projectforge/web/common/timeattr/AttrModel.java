/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.common.timeattr;

import org.apache.wicket.model.IObjectClassAwareModel;

import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;

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
