/////////////////////////////////////////////////////////////////////////////
//
// $RCSfile: HibernateProxyHelper.java,v $
//
// Project   Hibernate3History
//
// Author    Wolfgang Jung (w.jung@micromata.de)
// Created   Jan 6, 2006
// Copyright Micromata Jan 6, 2006
//
// $Id: HibernateProxyHelper.java,v 1.1 2007/03/08 22:50:50 wolle Exp $
// $Revision: 1.1 $
// $Date: 2007/03/08 22:50:50 $
//
/////////////////////////////////////////////////////////////////////////////
package org.projectforge.framework.persistence.xstream;

import org.hibernate.proxy.HibernateProxy;

/** Helper for initializing lazy-loading Proxies. 
 * 
 * @author Wolfgang Jung (w.jung@micromata.de)
 *
 */
public class HibernateProxyHelper
{
  /**
   * get the implementation behind object if it proxied, otherwise the object itself 
   * @param <T> the type of the Object 
   * @param object an object, might facaded by HibernateProxy
   * @return the initialized object behind the proxy
   */
  @SuppressWarnings("unchecked")
  public static <T> T get(T object) {
    if (object instanceof HibernateProxy) {
      HibernateProxy proxy = (HibernateProxy) object;
      return (T) proxy.getHibernateLazyInitializer().getImplementation();
    }
    return object;
  }
}
