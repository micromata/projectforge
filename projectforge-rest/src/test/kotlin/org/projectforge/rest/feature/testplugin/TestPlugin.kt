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


package org.projectforge.rest.feature.testplugin

import org.projectforge.plugins.core.AbstractPlugin
import org.projectforge.rest.config.Rest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

class TestPlugin : AbstractPlugin(ID, "Test", "Plugin of FeaturePathIndexTest.") {
    override fun initialize() {}

    override val featureNextRoutes: List<String>
        get() = listOf("public/testplugin")

    companion object {
        const val ID = "testplugin"
    }
}

@RestController
@RequestMapping("${Rest.URL}/testplugin")
class TestPluginRest {
    @GetMapping("list")
    fun list(): String = ""
}

@RestController
@RequestMapping(Rest.PUBLIC_URL)
class TestPluginPublicRest {
    @GetMapping("testpluginPublic")
    fun get(): String = ""
}
