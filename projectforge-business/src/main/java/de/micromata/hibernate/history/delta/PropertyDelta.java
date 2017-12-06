/////////////////////////////////////////////////////////////////////////////
//
// $RCSfile: PropertyDelta.java,v $
//
// Project   BaseApp
//
// Author    Wolfgang Jung (w.jung@micromata.de)
// Created   Mar 7, 2005
//
// $Id: PropertyDelta.java,v 1.1 2007/03/08 22:50:48 wolle Exp $
// $Revision: 1.1 $
// $Date: 2007/03/08 22:50:48 $
//
/////////////////////////////////////////////////////////////////////////////
package de.micromata.hibernate.history.delta;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;

/**
 * Legacy used for XML persistence of DB.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public abstract class PropertyDelta
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PropertyDelta.class);

  private Integer id;

  protected String propertyName;

  protected String propertyType;

  protected String oldValue;

  protected String newValue;

  public final Integer getId()
  {
    return id;
  }

  public final void setId(Integer id)
  {
    this.id = id;
  }

  /**
   * @return Returns the newValue.
   */
  public String getNewValue()
  {
    return newValue;
  }

  /**
   * @return Returns the oldValue.
   */
  public String getOldValue()
  {
    return oldValue;
  }

  /**
   * @return Returns the propertyName.
   */
  public String getPropertyName()
  {
    return propertyName;
  }

  /**
   * @return Returns the propertyType.
   */
  public String getPropertyType()
  {
    return propertyType;
  }

  /**
   * @param newValue The newValue to set.
   */
  public void setNewValue(String newValue)
  {
    this.newValue = newValue;
  }

  /**
   * @param oldValue The oldValue to set.
   */
  public void setOldValue(String oldValue)
  {
    this.oldValue = oldValue;
  }

  /**
   * @param propertyName The propertyName to set.
   */
  public void setPropertyName(String propertyName)
  {
    this.propertyName = propertyName;
  }

  /**
   * @param propertyType The propertyType to set.
   */
  public void setPropertyType(String propertyType)
  {
    this.propertyType = propertyType;
  }

  @SuppressWarnings("unchecked")
  protected Object loadItem(String type, String id, Session session)
  {
    if (StringUtils.isBlank(id)) {
      return null;
    }
    try {
      Query query = session.createQuery("select o from " + type + " o where o.id = :pk");
      SessionFactory factory = session.getSessionFactory();
      Map<String, ClassMetadata> map = factory.getAllClassMetadata();
      ClassMetadata meta = null;
      for (String entry : map.keySet()) {
        if (entry.endsWith(type) == true) {
          meta = factory.getClassMetadata(entry);
          break;
        }
      }
      if (meta == null) {
        log.warn("Oups, no metadata found for entity: " + type);
        return null;
      }
      Class<?> pkType = meta.getIdentifierType().getReturnedClass();
      if (ClassUtils.isAssignable(pkType, Number.class) == true) {
        if (pkType == Integer.class) {
          Integer pk = Integer.parseInt(id);
          query.setInteger("pk", pk);
        } else {
          Long pk = Long.parseLong(id);
          query.setLong("pk", pk);
        }
      } else {
        query.setString("pk", id);
      }
      query.setCacheable(true);
      query.setMaxResults(1);
      List<?> list = query.list();
      if (list.size() > 0) {
        return list.get(0);
      }
      return null;
    } catch (HibernateException ex) {
      return null;
    }
  }

  public abstract Object getOldObjectValue(Session session) throws HibernateException;

  public abstract Object getNewObjectValue(Session session) throws HibernateException;

  public boolean anyChangeDetected()
  {
    return !StringUtils.equals(getOldValue(), getNewValue());
  }

}