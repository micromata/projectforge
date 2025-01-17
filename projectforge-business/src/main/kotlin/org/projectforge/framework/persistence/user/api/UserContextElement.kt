/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.user.api

import kotlin.coroutines.CoroutineContext

class UserContextElement(private val userContext: UserContext) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<UserContextElement>
    override val key: CoroutineContext.Key<*> = Key

    fun getUserContext() = userContext
}
/*
// Extension function to access UserContext in the Coroutine
val CoroutineContext.userContext: UserContext?
    get() = this[UserContextElement]?.getUserContext()

// Funktion zum Starten einer Coroutine mit UserContext
fun launchWithUserContext(userContext: UserContext, block: suspend () -> Unit) = kotlinx.coroutines.runBlocking {
    kotlinx.coroutines.withContext(UserContextElement(userContext)) {
        block()
    }
}

fun main() {
    val userContext = UserContext("John Doe")

    launchWithUserContext(userContext) {
        println("Running in coroutine with User: ${kotlin.coroutines.coroutineContext.userContext?.username}")
    }
}
 */
