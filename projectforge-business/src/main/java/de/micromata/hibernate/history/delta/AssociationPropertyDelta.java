/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package de.micromata.hibernate.history.delta;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang3.ClassUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;

/**
 * @author Wolfgang Jung (w.jung@micromata.de)
 * 
 */
public class AssociationPropertyDelta extends PropertyDelta
{
  protected AssociationPropertyDelta()
  {
    // do nothing
  }

  public AssociationPropertyDelta(final SessionFactory factory, String propertyName, Class<?> propertyType,
      Object oldId, Object newId)
          throws HibernateException
  {
    this.propertyName = propertyName;
    this.propertyType = ClassUtils.getShortClassName(propertyType);
    ClassMetadata classMetadata = factory.getClassMetadata(propertyType);
    if (classMetadata != null) {
      this.oldValue = oldId == null ? null : ConvertUtils.convert(classMetadata.getIdentifier(oldId/*
                                                                                                    * , EntityMode.POJO
                                                                                                    */));
      this.newValue = newId == null ? null : ConvertUtils.convert(classMetadata.getIdentifier(newId/*
                                                                                                    * , EntityMode.POJO
                                                                                                    */));
    } else {
      this.newValue = ConvertUtils.convert(newId);
      this.oldValue = ConvertUtils.convert(oldId);
    }
  }

  @Override
  public String toString()
  {
    return "change in object " + propertyName + " old=" + oldValue + " new=" + newValue;
  }

  @Override
  public Object getNewObjectValue(Session session) throws HibernateException
  {
    return loadItem(propertyType, newValue, session);
  }

  @Override
  public Object getOldObjectValue(Session session) throws HibernateException
  {
    return loadItem(propertyType, oldValue, session);
  }

}
