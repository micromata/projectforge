/////////////////////////////////////////////////////////////////////////////
//
// $RCSfile: DeltaSet.java,v $
//
// Project   BaseApp
//
// Author    Wolfgang Jung (w.jung@micromata.de)
// Created   Mar 7, 2005
//
// $Id: DeltaSet.java,v 1.2 2007-06-13 09:00:26 wolle Exp $
// $Revision: 1.2 $
// $Date: 2007-06-13 09:00:26 $
//
/////////////////////////////////////////////////////////////////////////////
package de.micromata.hibernate.history.delta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Wolfgang Jung (w.jung@micromata.de)
 * 
 */
public class DeltaSet implements java.io.Serializable
{
  /**
   * 
   */
  private static final long serialVersionUID = 3887433882538488773L;

  private List<PropertyDelta> deltas = new ArrayList<PropertyDelta>();

  private Class<?> entity;

  private Serializable id;

  private transient Set<String> deltaPropertyNames = new HashSet<String>();

  public void addDelta(PropertyDelta delta)
  {
    if (delta.anyChangeDetected()) {
      deltas.add(delta);
      deltaPropertyNames.add(delta.getPropertyName());
    }
  }

  public List<PropertyDelta> getDeltas()
  {
    return Collections.unmodifiableList(deltas);
  }

  public boolean wasDelta(String propertyName)
  {
    return deltaPropertyNames.contains(propertyName);
  }

  public Class<?> getEntity()
  {
    return entity;
  }

  public Serializable getId()
  {
    return id;
  }

  public void setEntity(Class<?> entity)
  {
    this.entity = entity;
  }

  public void setId(Serializable id)
  {
    this.id = id;
  }

  void clear()
  {
    deltas.clear();
  }

  @Override
  public String toString()
  {
    return "deltas: " + deltas;
  }
}