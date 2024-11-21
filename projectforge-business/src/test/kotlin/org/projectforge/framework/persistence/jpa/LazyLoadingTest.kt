/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.jpa

import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.projectforge.ProjectForgeApp
import org.projectforge.business.test.TestConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional


@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
@Component
open class LazyLoadingTest {
    init {
        System.setProperty(ProjectForgeApp.CONFIG_PARAM_BASE_DIR, "/Users/kai/ProjectForge")
    }

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveEntities() {
        val em = entityManagerFactory.createEntityManager()
        em.transaction.begin()
        val owner = BicycleOwner().apply { id = 1; name = "John Doe" }
        em.persist(owner)
        val bicycle = Bicycle().apply { id = 1; name = "Bike"; this.owner = owner }
        em.persist(bicycle)
        em.transaction.commit()
        em.close()
    }

    @Test
    fun testLazyLoading() {
        saveEntities()
        val bicycle = entityManagerFactory.createEntityManager().let { em ->
            val query = em.createQuery("SELECT b FROM Bicycle b WHERE b.id = :id", Bicycle::class.java)
            query.setParameter("id", 1L)
            val bicycle = query.singleResult
            //val bicycle = em.find(Bicycle::class.java, 1L)
            em.close() // EntityManager schließen, um Lazy-Loading zu testen
            bicycle
        }
        bicycle.name = "Bike 2"
        entityManagerFactory.createEntityManager().let { em ->
            em.find(Bicycle::class.java, 1L).let {
                Assertions.assertEquals("Bike", it.name, "Name should not be updated yet, bike was detached.")
                Assertions.assertEquals("John Doe", it.owner?.name)
            }
            em.close()
        }
    }
}
