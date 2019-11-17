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

package org.projectforge.framework.persistence.utils

import org.projectforge.framework.persistence.jpa.PfEmgrFactory
import javax.persistence.EntityManager

/**
 * Some helper methods ...
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object PFTransactionTemplate {
    @JvmStatic
    fun runInTrans(emf: PfEmgrFactory, run: (em: EntityManager) -> Any): Any? {
        var cause: java.lang.Exception? = null
        var em: EntityManager? = null
        var result: Any? = null
        try {
            em = emf.entityManagerFactory.createEntityManager()
            em.transaction.begin()
            result = run(em)
            em.transaction.commit()
        } catch (ex: Exception) {
            cause = ex
            em?.transaction?.rollback()
        } finally {
            try {
                if (em?.isOpen == true) {
                    em.close()
                }
            } catch (ex: Exception) {
                cause = ex
            }
            if (cause != null) throw cause
            return result
        }
    }
}
