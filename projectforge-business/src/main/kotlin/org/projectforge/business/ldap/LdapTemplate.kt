/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.ldap

import mu.KotlinLogging
import javax.naming.NameNotFoundException
import javax.naming.NamingEnumeration
import javax.naming.NamingException
import javax.naming.directory.DirContext
import javax.naming.directory.SearchResult

private val log = KotlinLogging.logger {}

/**
 * Template for closing contexts and result properly.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
abstract class LdapTemplate(private val ldapConnector: LdapConnector) {
  //private static int openResults = 0;
  protected var results: NamingEnumeration<SearchResult>? = null

  @JvmField
  protected var ctx: DirContext? = null

  fun excecute(): Any? {
    ctx = ldapConnector.createContext()
    if (ctx != null) {
      ++openConnections
    }
    return internalExcecute()
  }

  fun excecute(username: String?, password: CharArray?): Any? {
    ctx = try {
      ldapConnector.createContext(username, password)
    } catch (ex: NamingException) {
      log.error("While trying to connect LDAP initially: " + ex.message, ex)
      throw RuntimeException(ex)
    }
    return internalExcecute()
  }

  private fun internalExcecute(): Any? {
    results = null
    return try {
      call()
    } catch (e: NameNotFoundException) {
      // The base context was not found.
      // Just clean up and exit.
      log.error(e.message, e)
      null
    } catch (e: Exception) {
      log.error(e.message, e)
      throw RuntimeException(e)
    } finally {
      results?.let {
        try {
          it.close()
        } catch (e: Exception) {
          log.error(e.message, e)
          // Never mind this.
        }
      }
      ctx?.let { context ->
        try {
          log.info("Closing LDAP connection ($openConnections connections opened).")
          context.close()
          --openConnections
        } catch (e: Exception) {
          log.error(e.message, e)
          // Never mind this.
        }
      }
    }
  }

  @Throws(NameNotFoundException::class, Exception::class)
  protected abstract fun call(): Any?

  companion object {
    private var openConnections = 0
  }
}
