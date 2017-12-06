/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web.core;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler for storing objects in the user's http session. Use this handler instead of session.setAttribute(String,
 * Object);
 *
 * @author kai
 */
@Component
public class SessionStorage
{
  private final static Logger log = LoggerFactory.getLogger(SessionStorage.class);

  /**
   * Wrapper for HttpSession.setAttribute
   *
   * @param session The user's session.
   * @param key     The key for storing the object.
   * @param obj     The object to store.
   */
  public void putObject(final HttpSession session, final String key, final Object obj)
  {
    synchronized (session) {
      if (log.isDebugEnabled() == true) {
        log.debug("Storing object in the user's session " + session.getId() + " under key " + key + ": " + obj);
      }
      session.setAttribute(key, obj);
    }
  }

  /**
   * Wrapper for HttpSession.getAttribute
   *
   * @param session The user's session.
   * @param key     The key of the stored object.
   */
  public Object getObject(final HttpSession session, final String key)
  {
    synchronized (session) {
      final Object obj = session.getAttribute(key);
      if (obj == null) {
        log.debug("Object in user's session " + session.getId() + " under key " + key + " not found.");
      } else if (log.isDebugEnabled() == true) {
        log.debug("Getting object in the user's session " + session.getId() + " under key " + key + ": " + obj);
      }
      return obj;
    }
  }

  /**
   * Wrapper for HttpSession.getAttribute
   *
   * @param session The user's session.
   * @param key     The key of the stored object.
   */
  public void removeAttribute(final HttpSession session, final String key)
  {
    synchronized (session) {
      if (log.isDebugEnabled() == true) {
        log.debug("Removing object from the user's session " + session.getId() + " with key " + key);
      }
      session.removeAttribute(key);
    }
  }

  /**
   * Removes all registered attributes from the given session. Please note, the user will be logged out!
   */
  @SuppressWarnings("unchecked")
  public void clearSession(final HttpSession session)
  {
    synchronized (session) {
      if (log.isDebugEnabled() == true) {
        log.debug("Clearing session " + session.getId());
      }
      final Enumeration en = session.getAttributeNames();
      // Later: Next servlet specifications supports getting of whole map.
      final ArrayList<String> list = new ArrayList<String>();
      while (en.hasMoreElements() == true) {
        list.add((String) en.nextElement());
      }
      String attrName = null;
      for (final Iterator it = list.iterator(); it.hasNext() == true; ) {
        attrName = (String) it.next();
        session.removeAttribute(attrName);
        if (log.isDebugEnabled() == true) {
          log.debug("Removing session " + session.getId() + " attribute: " + attrName);
        }
      }
    }
  }

  SessionStorage()
  {
  }
}
