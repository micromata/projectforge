package org.projectforge.rest

import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.GroupFilter
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.Group
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/group")
class GroupRest() : AbstractDTORest<GroupDO, Group, GroupDao, GroupFilter>(GroupDao::class.java, GroupFilter::class.java, "group.title") {
    override fun transformDO(obj: GroupDO, editMode : Boolean): Group {
        val group = Group()
        group.copyFrom(obj)
        return group
    }

    override fun transformDTO(dto: Group): GroupDO {
        val groupDO = GroupDO()
        dto.copyTo(groupDO)
        return groupDO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "name", "organization", "description", "assignedUsers", "ldapValues"))
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: GroupDO): UILayout {
        val layout = super.createEditLayout(dataObject)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "name", "organization", "description"))
                        .add(UICol()
                                .add(lc, "assignedUsers")))
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}