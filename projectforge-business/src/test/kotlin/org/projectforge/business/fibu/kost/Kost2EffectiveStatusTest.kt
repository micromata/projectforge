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

package org.projectforge.business.fibu.kost

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektStatus
import org.projectforge.business.test.AbstractTestBase

/**
 * Tests für die virtuelle Status-Vererbung von ProjektDO zu Kost2DO.
 *
 * Die Logik:
 * - Wenn projekt.status == ENDED oder projekt.deleted == true → Kost2 effectiveStatus = ENDED
 * - Sonst → Verwende den eigenen kostentraegerStatus der Kost2
 * - null wird wie bisher als ACTIVE behandelt
 */
class Kost2EffectiveStatusTest : AbstractTestBase() {

    @Test
    fun `effectiveStatus ohne Projekt entspricht eigenem Status`() {
        // Kein Projekt → effectiveStatus == own status
        val kost2 = Kost2DO()
        kost2.projekt = null
        kost2.kostentraegerStatus = KostentraegerStatus.ACTIVE

        assertEquals(KostentraegerStatus.ACTIVE, kost2.effectiveKostentraegerStatus)

        kost2.kostentraegerStatus = KostentraegerStatus.NONACTIVE
        assertEquals(KostentraegerStatus.NONACTIVE, kost2.effectiveKostentraegerStatus)

        kost2.kostentraegerStatus = KostentraegerStatus.ENDED
        assertEquals(KostentraegerStatus.ENDED, kost2.effectiveKostentraegerStatus)

        // null Status ohne Projekt
        kost2.kostentraegerStatus = null
        assertNull(kost2.effectiveKostentraegerStatus)
    }

    @Test
    fun `effectiveStatus mit aktivem Projekt entspricht eigenem Status`() {
        // Projekt ACTIVE (und andere nicht-ENDED Status) → effectiveStatus == own status
        val projekt = ProjektDO()
        projekt.status = ProjektStatus.PRODUCTIVE
        projekt.deleted = false

        val kost2 = Kost2DO()
        kost2.projekt = projekt
        kost2.kostentraegerStatus = KostentraegerStatus.ACTIVE

        assertEquals(KostentraegerStatus.ACTIVE, kost2.effectiveKostentraegerStatus)

        kost2.kostentraegerStatus = KostentraegerStatus.NONACTIVE
        assertEquals(KostentraegerStatus.NONACTIVE, kost2.effectiveKostentraegerStatus)

        kost2.kostentraegerStatus = null
        assertNull(kost2.effectiveKostentraegerStatus)
    }

    @Test
    fun `effectiveStatus mit beendetem Projekt ist ENDED unabhaengig vom eigenen Status`() {
        // Projekt ENDED → effectiveStatus == ENDED (unabhängig von own status)
        val projekt = ProjektDO()
        projekt.status = ProjektStatus.ENDED
        projekt.deleted = false

        val kost2 = Kost2DO()
        kost2.projekt = projekt

        // Eigener Status ACTIVE, aber Projekt ENDED → effectiveStatus = ENDED
        kost2.kostentraegerStatus = KostentraegerStatus.ACTIVE
        assertEquals(KostentraegerStatus.ENDED, kost2.effectiveKostentraegerStatus)

        // Eigener Status NONACTIVE, aber Projekt ENDED → effectiveStatus = ENDED
        kost2.kostentraegerStatus = KostentraegerStatus.NONACTIVE
        assertEquals(KostentraegerStatus.ENDED, kost2.effectiveKostentraegerStatus)

        // Eigener Status ENDED, Projekt ENDED → effectiveStatus = ENDED
        kost2.kostentraegerStatus = KostentraegerStatus.ENDED
        assertEquals(KostentraegerStatus.ENDED, kost2.effectiveKostentraegerStatus)

        // Eigener Status null, Projekt ENDED → effectiveStatus = ENDED
        kost2.kostentraegerStatus = null
        assertEquals(KostentraegerStatus.ENDED, kost2.effectiveKostentraegerStatus)
    }

    @Test
    fun `effectiveStatus mit geloeschtem Projekt ist ENDED`() {
        // Projekt deleted=true → effectiveStatus == ENDED
        val projekt = ProjektDO()
        projekt.status = ProjektStatus.PRODUCTIVE // Status ist nicht ENDED
        projekt.deleted = true // Aber Projekt ist gelöscht

        val kost2 = Kost2DO()
        kost2.projekt = projekt

        // Eigener Status ACTIVE, aber Projekt deleted → effectiveStatus = ENDED
        kost2.kostentraegerStatus = KostentraegerStatus.ACTIVE
        assertEquals(KostentraegerStatus.ENDED, kost2.effectiveKostentraegerStatus)

        // Eigener Status null, aber Projekt deleted → effectiveStatus = ENDED
        kost2.kostentraegerStatus = null
        assertEquals(KostentraegerStatus.ENDED, kost2.effectiveKostentraegerStatus)
    }

    @Test
    fun `effectiveStatus mit beendetem UND geloeschtem Projekt ist ENDED`() {
        // Beide Bedingungen erfüllt
        val projekt = ProjektDO()
        projekt.status = ProjektStatus.ENDED
        projekt.deleted = true

        val kost2 = Kost2DO()
        kost2.projekt = projekt
        kost2.kostentraegerStatus = KostentraegerStatus.ACTIVE

        assertEquals(KostentraegerStatus.ENDED, kost2.effectiveKostentraegerStatus)
    }

    @Test
    fun `effectiveStatus mit verschiedenen Projekt-Status (nicht ENDED)`() {
        // Verschiedene Projekt-Status durchgehen (außer ENDED)
        val projekt = ProjektDO()
        projekt.deleted = false

        val kost2 = Kost2DO()
        kost2.projekt = projekt
        kost2.kostentraegerStatus = KostentraegerStatus.ACTIVE

        // NONE
        projekt.status = ProjektStatus.NONE
        assertEquals(KostentraegerStatus.ACTIVE, kost2.effectiveKostentraegerStatus)

        // ACQUISITION
        projekt.status = ProjektStatus.ACQUISISTION
        assertEquals(KostentraegerStatus.ACTIVE, kost2.effectiveKostentraegerStatus)

        // ON_HOLD
        projekt.status = ProjektStatus.ON_HOLD
        assertEquals(KostentraegerStatus.ACTIVE, kost2.effectiveKostentraegerStatus)

        // BUILD
        projekt.status = ProjektStatus.BUILD
        assertEquals(KostentraegerStatus.ACTIVE, kost2.effectiveKostentraegerStatus)

        // PRODUCTIVE
        projekt.status = ProjektStatus.PRODUCTIVE
        assertEquals(KostentraegerStatus.ACTIVE, kost2.effectiveKostentraegerStatus)

        // MAINTENANCE
        projekt.status = ProjektStatus.MAINTENANCE
        assertEquals(KostentraegerStatus.ACTIVE, kost2.effectiveKostentraegerStatus)
    }
}
