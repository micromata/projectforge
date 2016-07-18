package org.projectforge.framework.persistence.api;

import java.io.Serializable;

import org.projectforge.framework.access.AccessException;

import de.micromata.genome.jpa.MarkDeletableRecord;
import de.micromata.genome.util.bean.PrivateBeanUtils;

/**
 * 
 * JPA Core persistence services.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 * @param <O>
 */
public interface ICorePersistenceService<PK extends Serializable, ENT extends MarkDeletableRecord<PK>>
{

  Class<ENT> getEntityClass();

  default ENT newInstance()
  {
    ENT ret = PrivateBeanUtils.createInstance(getEntityClass());
    return ret;
  }

  /**
   * Inserts into db.
   * 
   * @param obj
   * @return
   * @throws AccessException
   */
  PK insert(ENT obj) throws AccessException;

  /**
   *
   * @param obj
   * @return the generated identifier.
   * @throws AccessException
   */
  default PK save(ENT obj) throws AccessException
  {
    return insert(obj);
  }

  ENT selectByPkDetached(PK pk) throws AccessException;

  @Deprecated
  default ENT getById(final Serializable id) throws AccessException
  {
    return selectByPkDetached((PK) id);
  }

  /**
   * @param obj
   * @throws AccessException
   * @return true, if modifications were done, false if no modification detected.
   * @see #internalUpdate(ExtendedBaseDO, boolean)
   */
  ModificationStatus update(ENT obj) throws AccessException;

  /**
   * @param obj
   * @throws AccessException
   * @return true, if modifications were done, false if no modification detected.
   * @see #internalUpdate(ExtendedBaseDO, boolean)
   */
  default ModificationStatus update(ENT obj, String... strings) throws AccessException {
    return null;
  }

  /**
   * Object will be marked as deleted (boolean flag), therefore undelete is always possible without any loss of data.
   * 
   * @param obj
   */
  void markAsDeleted(ENT obj) throws AccessException;

  /**
   * Object will be marked as deleted (booelan flag), therefore undelete is always possible without any loss of data.
   * 
   * @param obj
   */
  void undelete(ENT obj) throws AccessException;

  /**
   * Object will be deleted finally out of the data base.
   * 
   * @param obj
   */
  void delete(ENT obj) throws AccessException;

  @Deprecated
  boolean isHistorizable();
}
