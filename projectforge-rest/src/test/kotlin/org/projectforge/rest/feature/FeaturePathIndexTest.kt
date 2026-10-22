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


package org.projectforge.rest.feature

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.projectforge.feature.FeatureDefinition
import org.projectforge.feature.FeatureDerivation
import org.projectforge.feature.FeatureService
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.rest.config.AttachmentCategoryRequests
import org.projectforge.rest.feature.testplugin.TestPlugin
import org.projectforge.rest.feature.testplugin.TestPluginPublicRest
import org.projectforge.rest.feature.testplugin.TestPluginRest
import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.beans.factory.ObjectProvider
import org.springframework.web.bind.annotation.RestController

class FeaturePathIndexTest {
    private val definitions = listOf(
        FeatureDerivation.derive("ADDRESS_LIST", "next/address", restPaths = listOf("addressImport")),
        FeatureDerivation.derive("ADDRESSBOOK_LIST", "next/addressBook"),
        FeatureDerivation.derive("CALENDAR", "next/calendar", restPaths = listOf("teamEvent")),
        FeatureDerivation.derive("CALENDAR_LIST", "next/teamCal", restPaths = listOf("teamEvent")),
        FeatureDerivation.derive(
            "TASK_TREE", "next/taskTree", restPaths = listOf("task"), sharedRestPaths = listOf("task/favorites"),
        ),
        FeatureDerivation.derive("PERSONAL_BOX", "next/testplugin/-1", restPaths = listOf("testpluginpersonal")),
        FeatureDerivation.derive("MENU_ENTRY", "next/menuEntry", restPaths = listOf("menu/customizer")),
    )

    private fun index(vararg disabled: String, pluginActive: Boolean = true): FeaturePathIndex {
        val featureService = Mockito.mock(FeatureService::class.java)
        Mockito.`when`(featureService.definitions).thenReturn(definitions)
        Mockito.`when`(featureService.isEnabled(anyString())).thenAnswer { invocation ->
            val id = invocation.getArgument<String>(0)
            id !in disabled && (pluginActive || id != TestPlugin.ID)
        }
        val pluginAdminService = Mockito.mock(PluginAdminService::class.java)
        Mockito.`when`(pluginAdminService.availablePlugins).thenReturn(listOf(TestPlugin()))
        val applicationContext = Mockito.mock(ApplicationContext::class.java)
        Mockito.`when`(applicationContext.getBean(FeatureService::class.java)).thenReturn(featureService)
        Mockito.`when`(applicationContext.getBean(PluginAdminService::class.java)).thenReturn(pluginAdminService)
        Mockito.`when`(applicationContext.getBeansWithAnnotation(RestController::class.java)).thenReturn(
            mapOf("a" to TestPluginRest(), "b" to TestPluginPublicRest())
        )
        val index = FeaturePathIndex()
        FeaturePathIndex::class.java.getDeclaredField("applicationContext").also { it.isAccessible = true }
            .set(index, applicationContext)
        return index
    }

    @Test
    fun `paths of switched off features are blocked`() {
        val index = index("ADDRESS_LIST")
        Assertions.assertTrue(index.isBlocked("/rs/address/list"))
        Assertions.assertTrue(index.isBlocked("/rs/addressSelected/list"))
        Assertions.assertTrue(index.isBlocked("/rs/addressImport/upload"))
        Assertions.assertTrue(index.isBlocked("/next/address/"))
        Assertions.assertTrue(index.isBlocked("/next/address/42/index.txt"))
        // Whole segments, not string prefixes:
        Assertions.assertFalse(index.isBlocked("/rs/addressBook/list"))
        Assertions.assertFalse(index.isBlocked("/next/addressBook/"))
        // The lookups of the pickers:
        Assertions.assertFalse(index.isBlocked("/rs/address/autosearch"))
        Assertions.assertFalse(index.isBlocked("/rs/address/autocomplete"))
        // Attachments with the category in the path:
        Assertions.assertTrue(index.isBlocked("/rs/attachments/download/address/42"))
        Assertions.assertFalse(index.isBlocked("/rs/attachments/download/addressBook/42"))
        Assertions.assertTrue(index.isCategoryBlocked("address"))
    }

    @Test
    fun `core, assets and unknown paths stay reachable`() {
        val index = index("ADDRESS_LIST", "MENU_ENTRY")
        Assertions.assertFalse(index.isBlocked("/rs/userStatus"))
        Assertions.assertFalse(index.isBlocked("/rs/menu"))
        Assertions.assertTrue(index.isBlocked("/rs/menu/customizer"), "Longest prefix wins over the core.")
        Assertions.assertFalse(index.isBlocked("/rs/unknown/list"))
        Assertions.assertFalse(index.isBlocked("/next/_next/static/address/chunk.js"))
        Assertions.assertFalse(index.isBlocked("/next/public/_next/static/chunk.js"))
        Assertions.assertFalse(index.isBlocked("/next/"))
        Assertions.assertFalse(index.isBlocked("/wa/address"))
        Assertions.assertFalse(index.isBlocked("/rsPublic/systemStatus"))
    }

    @Test
    fun `shared ownership and shared paths`() {
        Assertions.assertFalse(index("CALENDAR").isBlocked("/rs/teamEvent/list"), "Still owned by CALENDAR_LIST.")
        Assertions.assertTrue(index("CALENDAR", "CALENDAR_LIST").isBlocked("/rs/teamEvent/list"))
        val index = index("TASK_TREE")
        Assertions.assertTrue(index.isBlocked("/rs/task/list"))
        Assertions.assertFalse(index.isBlocked("/rs/task/favorites/list"), "Used by the task picker.")
    }

    @Test
    fun `paths of plugins`() {
        Assertions.assertFalse(index().isBlocked("/rs/testplugin/list"))
        val inactive = index(pluginActive = false)
        Assertions.assertTrue(inactive.isBlocked("/rs/testplugin/list"))
        Assertions.assertTrue(inactive.isBlocked("/rsPublic/testpluginPublic"))
        Assertions.assertTrue(inactive.isBlocked("/next/testplugin/"))
        Assertions.assertTrue(inactive.isBlocked("/next/public/testplugin/token"))
        Assertions.assertFalse(inactive.isBlocked("/next/public/_next/static/chunk.js"))
        // Claimed by a menu entry, not by the plugin (the plugin is switched off with its entries in production).
        Assertions.assertFalse(inactive.isBlocked("/rs/testpluginpersonal"))
        val personalBoxOff = index("PERSONAL_BOX")
        Assertions.assertFalse(personalBoxOff.isBlocked("/next/testplugin/42"))
        Assertions.assertTrue(personalBoxOff.isBlocked("/next/testplugin/-1/"))
        Assertions.assertTrue(personalBoxOff.isBlocked("/rs/testpluginpersonal/list"))
    }

    @Test
    fun `next routes for the frontend`() {
        val routes = index("PERSONAL_BOX", "ADDRESS_LIST", pluginActive = false).nextRoutes()
        Assertions.assertTrue("address" in routes.disabled)
        Assertions.assertTrue("testplugin/-1" in routes.disabled)
        Assertions.assertTrue("testplugin" in routes.disabled)
        Assertions.assertFalse("addressBook" in routes.disabled)
        Assertions.assertTrue(routes.enabled.isEmpty(), "${routes.enabled}")
        val pluginOnly = index("ADDRESS_LIST", pluginActive = false).nextRoutes()
        Assertions.assertEquals(listOf("testplugin/-1"), pluginOnly.enabled, "Reachable below a blocked route.")
    }

    private fun filter(index: FeaturePathIndex): FeatureFilter {
        @Suppress("UNCHECKED_CAST")
        val provider = Mockito.mock(ObjectProvider::class.java) as ObjectProvider<FeaturePathIndex>
        Mockito.`when`(provider.getObject()).thenReturn(index)
        return FeatureFilter(provider)
    }

    private fun FeatureFilter.call(uri: String, body: ByteArray? = null): Pair<Int, MockFilterChain> {
        val request = MockHttpServletRequest(if (body == null) "GET" else "POST", uri.substringBefore('?'))
        if (uri.contains('?')) {
            request.queryString = uri.substringAfter('?')
            uri.substringAfter('?').split('&').forEach { request.addParameter(it.substringBefore('='), it.substringAfter('=')) }
        }
        body?.let {
            request.contentType = "application/json"
            request.setContent(it)
        }
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        doFilter(request, response, chain)
        return response.status to chain
    }

    private fun FeatureFilter.call(uri: String, body: String) = call(uri, body.toByteArray())

    @Test
    fun `filter answers 404 and checks the category of attachment bodies`() {
        val filter = filter(index("ADDRESS_LIST"))
        Assertions.assertEquals(404, filter.call("/rs/address/list").first)
        val (status, chain) = filter.call("/rs/addressBook/list")
        Assertions.assertEquals(200, status)
        Assertions.assertNotNull(chain.request)

        Assertions.assertEquals(404, filter.call("/rs/attachments/delete", """{"data":{"category":"address"}}""").first)
        val (bodyStatus, bodyChain) = filter.call("/rs/attachments/delete", """{"data":{"category":"addressBook"}}""")
        Assertions.assertEquals(200, bodyStatus)
        Assertions.assertEquals(
            """{"data":{"category":"addressBook"}}""",
            String(bodyChain.request!!.inputStream.readAllBytes()),
            "The body reaches the controller.",
        )
    }

    @Test
    fun `filter decides by the decoded path, as the controllers are mapped`() {
        val filter = filter(index("ADDRESS_LIST"))
        Assertions.assertEquals(404, filter.call("/rs/addr%65ss/list").first)
        Assertions.assertEquals(404, filter.call("/next/addr%65ss/").first)
        Assertions.assertEquals(
            404,
            filter.call("/rs/attachments/d%65lete", """{"data":{"category":"address"}}""").first,
        )
    }

    @Test
    fun `filter reads no body beyond the limit and none if nothing is switched off`() {
        val tooLarge = ByteArray(AttachmentCategoryRequests.MAX_BODY_SIZE + 1) { ' '.code.toByte() }
        Assertions.assertEquals(413, filter(index("ADDRESS_LIST")).call("/rs/attachments/delete", tooLarge).first)
        val (status, chain) = filter(index()).call("/rs/attachments/delete", tooLarge)
        Assertions.assertEquals(200, status)
        Assertions.assertFalse(chain.request is AttachmentCategoryRequests.CachedBodyRequest, "Not read at all.")
    }
}
