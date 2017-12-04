package org.projectforge.framework.persistence.hibernate;

import java.util.Collection;

import org.hibernate.CacheMode;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.search.FullTextSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.micromata.genome.util.bean.PrivateBeanUtils;

/**
 * Abstraction layer to wrapp Hibernate 3 to 5.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class HibernateCompatUtils
{
  private static final Logger log = LoggerFactory.getLogger(HibernateCompatUtils.class);

  public static Session openSession(SessionFactory sessionFactory, Interceptor interceptor)
  {
    if (EmptyInterceptor.INSTANCE != interceptor) {
      log.error("HIBERNATE5 not supported sessionFactory.openSession() with interceptor");
    }
    return sessionFactory.openSession();
  }

  public static void setClassMetaDataSetIdentifier(ClassMetadata classMetadata, Object entity, EntityMode mode)
  {
    PrivateBeanUtils.invokeMethod(classMetadata, "setIdentifier", entity, null, mode);
  }

  public static boolean isPersistenceSet(Collection<Object> srcColl)
  {
    return (srcColl instanceof PersistentSet);
  }

  public static boolean isPersistentCollection(Object source)
  {
    return source instanceof PersistentCollection;
  }

  public static void setFlushMode(FullTextSession fullTextSession, FlushMode flushMode)
  {
    fullTextSession.setFlushMode(flushMode);
  }

  public static void setCacheMode(FullTextSession fullTextSession, CacheMode flushMode)
  {
    fullTextSession.setCacheMode(flushMode);
  }

  public static void index(FullTextSession fullTextSession, Object dbObj)
  {
    fullTextSession.index(dbObj);

  }

}
