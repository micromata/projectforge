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

package org.projectforge.business.ldap;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;

/**
 * Template for closing contexts and result properly.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class LdapTemplate
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LdapTemplate.class);

  private final LdapConnector ldapConnector;

  private static int openConnections = 0;

  //private static int openResults = 0;

  protected NamingEnumeration<SearchResult> results;

  protected DirContext ctx;

  public LdapTemplate(final LdapConnector ldapConnector)
  {
    this.ldapConnector = ldapConnector;
  }

  public Object excecute()
  {
    ctx = ldapConnector.createContext();
    if (ctx != null) {
      ++openConnections;
    }
    return internalExcecute();
  }

  public Object excecute(final String username, final String password)
  {
    try {
      ctx = ldapConnector.createContext(username, password);
    } catch (final NamingException ex) {
      log.error("While trying to connect LDAP initally: " + ex.getMessage(), ex);
      throw new RuntimeException(ex);
    }
    return internalExcecute();
  }

  private Object internalExcecute()
  {
    results = null;
    try {
      return call();
    } catch (final NameNotFoundException e) {
      // The base context was not found.
      // Just clean up and exit.
      log.error(e.getMessage(), e);
      return null;
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
      throw new RuntimeException(e);
    } finally {
      if (results != null) {
        try {
          results.close();
        } catch (final Exception e) {
          log.error(e.getMessage(), e);
          // Never mind this.
        }
      }
      if (ctx != null) {
        try {
          log.info("Closing LDAP connection (" + openConnections + " connections opened).");
          ctx.close();
          --openConnections;
        } catch (final Exception e) {
          log.error(e.getMessage(), e);
          // Never mind this.
        }
      }
    }
  }

  protected abstract Object call() throws NameNotFoundException, Exception;
}
