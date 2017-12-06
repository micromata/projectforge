package org.projectforge.framework.persistence.jpa.impl;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.JpaPfGenericPersistenceService;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.micromata.genome.jpa.DbRecord;
import de.micromata.genome.jpa.EntityCopyStatus;
import de.micromata.genome.jpa.MarkDeletableRecord;
import de.micromata.genome.util.runtime.ClassUtils;

/**
 * JPA base implementation.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Service
public class JpaPfPersistenceServiceImpl implements JpaPfGenericPersistenceService
{
  private static final Logger LOG = LoggerFactory.getLogger(JpaPfPersistenceServiceImpl.class);
  @Autowired
  private PfEmgrFactory emf;

  @Autowired
  private UserRightService userRights;

  @Override
  public Serializable insert(DbRecord<?> obj) throws AccessException
  {
    Validate.notNull(obj);
    return internalSave(obj);
  }

  public Serializable internalSave(DbRecord<?> obj)
  {
    Validate.notNull(obj);
    emf.runInTrans((emgr) -> {
      emgr.insert(obj);
      LOG.info("New object added (" + obj.getPk() + "): " + obj.toString());
      return obj.getPk();
    });

    return obj.getPk();
  }

  @Override
  public ModificationStatus update(final DbRecord<?> obj) throws AccessException
  {
    EntityCopyStatus status = emf.runInTrans((emgr) -> {
      EntityCopyStatus s = emgr.update(obj.getClass(), obj.getClass(), obj, true);
      LOG.info("Object updated: " + obj.toString());
      return s;
    });
    return ModificationStatus.fromEntityCopyStatus(status);
  }

  @Override
  public IUserRightId getUserRight(BaseDO<?> baseDo)
  {
    List<AUserRightId> annots = ClassUtils.findClassAnnotations(baseDo.getClass(), AUserRightId.class);
    if (annots.isEmpty() == true) {
      LOG.warn("Cannot find anot UserRightAnot on " + baseDo.getClass().getName());
      throw new IllegalArgumentException("Cannot find anot UserRightAnot on " + baseDo.getClass().getName());
    }
    String id = annots.get(0).value();
    return userRights.getRightId(id);
  }

  @Override
  public void markAsDeleted(MarkDeletableRecord<?> rec) throws AccessException
  {
    // special Pf handling see PfEmgr
    emf.runInTrans((emgr) -> emgr.markDeleted(rec));
  }

  @Override
  public void undelete(MarkDeletableRecord<?> rec) throws AccessException
  {
    // special Pf handling see PfEmgr
    emf.runInTrans((emgr) -> emgr.markUndeleted(rec));
  }

}
