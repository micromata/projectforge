/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.business.ldap.LdapPosixGroupsUtils
import org.projectforge.business.ldap.LdapUserDao
import org.projectforge.business.login.Login
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Group
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/group")
class GroupPagesRest : AbstractDTOPagesRest<GroupDO, Group, GroupDao>(GroupDao::class.java, "group.title") {

  @Autowired
  private lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var ldapPosixGroupsUtils: LdapPosixGroupsUtils

  @Autowired
  private lateinit var ldapUserDao: LdapUserDao

  @Autowired
  private lateinit var userService: UserService

  override fun transformFromDB(obj: GroupDO, editMode: Boolean): Group {
    val group = Group()
    group.copyFrom(obj)
    group.assignedUsers?.forEach {
      val user = userService.getUser(it.id)
      if (user != null) {
        it.username = user.username
        it.firstname = user.firstname
        it.lastname = user.lastname
      }
    }
    return group
  }

  override fun transformForDB(dto: Group): GroupDO {
    val groupDO = GroupDO()
    dto.copyTo(groupDO)
    return groupDO
  }

  override val classicsLinkListUrl: String?
    get() = "wa/groupList"

  /**
   * LAYOUT List page
   */
  override fun createListLayout(request: HttpServletRequest, magicFilter: MagicFilter): UILayout {
    val adminAccess = accessChecker.isLoggedInUserMemberOfAdminGroup
    val layout = super.createListLayout(request, magicFilter)
    val agGrid = agGridSupport.prepareUIGrid4ListPage(
      request,
      layout,
      magicFilter,
      this,
    )
      .add(lc, "name", "organization")
      .add(lc, "description", wrapText = true)
      .add(lc, "assignedUsers", formatter = UIAgGridColumnDef.Formatter.SHOW_LIST_OF_DISPLAYNAMES, wrapText = true)
    if (adminAccess && Login.getInstance().hasExternalUsermanagementSystem()) {
      agGrid.add(lc, "ldapValues")
    }
    return LayoutUtils.processListPage(layout, this)
  }

  override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
    elements.add(
      UIFilterListElement("type", label = translate("status"), defaultFilter = true, multi = false)
        .buildValues(GroupTypeFilter.TYPE::class.java)
    )
  }

  override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<GroupDO>> {
    val filters = mutableListOf<CustomResultFilter<GroupDO>>()
    val localGroupFilterEntry = source.entries.find { it.field == "type" }
    if (localGroupFilterEntry != null) {
      localGroupFilterEntry.synthetic = true
      val values = localGroupFilterEntry.value.values
      if (!values.isNullOrEmpty() && values.size == 1) {
        val value = values[0]
        try {
          GroupTypeFilter.TYPE.valueOf(value).let {
            filters.add(GroupTypeFilter(it))
          }
        } catch (ex: IllegalArgumentException) {
          log.warn { "Oups, can't convert '$value': ${ex.message}" }
        }
      }
    }
    return filters
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: Group, userAccess: UILayout.UserAccess): UILayout {
    val layout = super.createEditLayout(dto, userAccess)
      .add(
        UIRow()
          .add(
            UICol(lg = 6)
              .add(lc, "name", "organization", "description", "localGroup", "groupOwner")
          )
          .add(
            UICol(lg = 6)
              .add(UISelect.createUserSelect(lc, "assignedUsers", true, "group.assignedUsers"))
          )
      )
    val adminAccess = accessChecker.isLoggedInUserMemberOfAdminGroup
    if (adminAccess && Login.getInstance().hasExternalUsermanagementSystem() && ldapUserDao.isPosixAccountsConfigured) {
      // Ldap values
      layout.add(UIFieldset(title = "ldap")
        .)
    }
    dto.emails = "dsfkajsdlfads"
    layout.add(UIReadOnlyField("emails", label = "address.emails"))
    return LayoutUtils.processEditPage(layout, dto, this)
  }

  override val autoCompleteSearchFields = arrayOf("name", "organization")
}
