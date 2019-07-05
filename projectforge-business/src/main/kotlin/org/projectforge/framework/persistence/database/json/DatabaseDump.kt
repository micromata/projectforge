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

package org.projectforge.framework.persistence.database.json

import org.hibernate.SessionFactory
import org.projectforge.framework.persistence.jpa.PfEmgrFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream
import java.io.OutputStream
import javax.annotation.PostConstruct

/**
 * WIP: Trying to export and import whole database as json (for db migration and for creating test data).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
class DatabaseDump {
    private val log = LoggerFactory.getLogger(DatabaseDump::class.java)

    @Autowired
    private lateinit var emf: PfEmgrFactory

    @Autowired
    private lateinit var sessionFactory: SessionFactory

    @PostConstruct
    fun init() {
    }

    /**
     * @param filename virtual filename: If the filename suffix is "gz" then the dump will be compressed.
     * @param out
     */
    fun dump(out: OutputStream) {
        DatabaseWriter(emf, sessionFactory).dump(out)
    }

    /**
     * @param filename virtual filename: If the filename suffix is "gz" then the dump will be compressed.
     * @param out
     */
    fun restore(inputStream: InputStream) {
        DatabaseWriter(emf, sessionFactory).restore(inputStream)
    }
}
