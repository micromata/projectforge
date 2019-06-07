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
