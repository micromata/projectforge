/////////////////////////////////////////////////////////////////////////////
// Project   hibernate-history
//
// Author    Roger Kommer
// Created   06.11.2015
// Copyright Micromata 06.11.2015
//

//
/////////////////////////////////////////////////////////////////////////////
package org.projectforge.framework.persistence.history;

/**
 * The Class HistoryPropertiesEntry.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 */
public class HistoryPropertiesEntry
{

  /**
   * The old value.
   */
  private Object oldValue;

  /**
   * The new value.
   */
  private Object newValue;

  /**
   * Instantiates a new history properties entry.
   */
  public HistoryPropertiesEntry()
  {

  }

  /**
   * Instantiates a new history properties entry.
   *
   * @param oldValue the old value
   * @param newValue the new value
   */
  public HistoryPropertiesEntry(Object oldValue, Object newValue)
  {

    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  /**
   * Gets the old value.
   *
   * @return the old value
   */
  public Object getOldValue()
  {
    return oldValue;
  }

  /**
   * Sets the old value.
   *
   * @param oldValue the new old value
   */
  public void setOldValue(Object oldValue)
  {
    this.oldValue = oldValue;
  }

  /**
   * Gets the new value.
   *
   * @return the new value
   */
  public Object getNewValue()
  {
    return newValue;
  }

  /**
   * Sets the new value.
   *
   * @param newValue the new new value
   */
  public void setNewValue(Object newValue)
  {
    this.newValue = newValue;
  }

}
