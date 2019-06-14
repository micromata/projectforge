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
