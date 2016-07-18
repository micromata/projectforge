package org.projectforge.framework.persistence.api;

import java.io.Serializable;

import org.projectforge.framework.access.AccessException;

import de.micromata.genome.jpa.DbRecord;
import de.micromata.genome.jpa.MarkDeletableRecord;

/**
 * Stores genericly via JPA.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Deprecated
public interface JpaPfGenericPersistenceService
{
  Serializable insert(DbRecord<?> obj) throws AccessException;

  ModificationStatus update(DbRecord<?> obj) throws AccessException;

  /**
   * Object will be marked as deleted (boolean flag), therefore undelete is always possible without any loss of data.
   * 
   * @param obj
   */
  void markAsDeleted(MarkDeletableRecord<?> obj) throws AccessException;

  /**
   * Object will be marked as deleted (booelan flag), therefore undelete is always possible without any loss of data.
   * 
   * @param obj
   */
  void undelete(MarkDeletableRecord<?> obj) throws AccessException;

  IUserRightId getUserRight(BaseDO<?> baseDo);
}
