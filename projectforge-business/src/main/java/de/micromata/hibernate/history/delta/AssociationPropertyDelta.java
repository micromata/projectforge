/////////////////////////////////////////////////////////////////////////////
//
// $RCSfile: AssociationPropertyDelta.java,v $
//
// Project   BaseApp
//
// Author    Wolfgang Jung (w.jung@micromata.de)
// Created   Mar 7, 2005
//
// $Id: AssociationPropertyDelta.java,v 1.1 2007/03/08 22:50:48 wolle Exp $
// $Revision: 1.1 $
// $Date: 2007/03/08 22:50:48 $
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
