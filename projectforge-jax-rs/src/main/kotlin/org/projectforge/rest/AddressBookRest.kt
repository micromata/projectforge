package org.projectforge.rest

import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.address.AddressbookFilter
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.service.UserService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.Addressbook
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/addressBook")
class AddressBookRest() : AbstractDTORest<AddressbookDO, Addressbook, AddressbookDao, AddressbookFilter>(
        AddressbookDao::class.java,
        AddressbookFilter::class.java,
        "addressbook.title"
) {
    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userService: UserService

    // Needed to use as dto.
    override fun transformDO(obj: AddressbookDO): Addressbook {
        val addressbook = Addressbook()
        addressbook.copyFrom(obj)
        // Group names needed by React client (for ReactSelect):
        addressbook.fullAccessGroups?.forEach { it.name = groupService.getGroupname(it.id) }
        addressbook.readonlyAccessGroups?.forEach { it.name = groupService.getGroupname(it.id) }
        // Usernames needed by React client (for ReactSelect):
        addressbook.fullAccessUsers?.forEach { it.fullname = userService.getUser(it.id)?.fullname }
        addressbook.readonlyAccessUsers?.forEach { it.fullname = userService.getUser(it.id)?.fullname }
        return addressbook
    }

    // Needed to use as dto.
    override fun transformDTO(dto: Addressbook): AddressbookDO {
        val addressbookDO = AddressbookDO()
        dto.copyTo(addressbookDO)
        return addressbookDO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "title", "description", "owner", "accessright", "last_update"))
        layout.getTableColumnById("owner").formatter = Formatter.USER
        layout.getTableColumnById("last_update").formatter = Formatter.TIMESTAMP_MINUTES
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: AddressbookDO): UILayout {
        val allGroups = mutableListOf<UISelectValue<Int>>()
        groupService.sortedGroups?.forEach {
            allGroups.add(UISelectValue(it.id, it.name))
        }

        val allUsers = mutableListOf<UISelectValue<Int>>()
        userService.sortedUsers?.forEach {
            allUsers.add(UISelectValue(it.id, it.fullname))
        }

        val layout = super.createEditLayout(dataObject)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "title")
                                .add(lc, "description"))
                        .add(UICol()
                                .add(lc, "owner")))
                .add(UIRow()
                        .add(UICol()
                                .add(UIMultiSelect("fullAccessUsers", lc,
                                        label = "addressbook.fullAccess",
                                        additionalLabel = "access.users",
                                        values = allUsers,
                                        labelProperty = "fullname",
                                        valueProperty = "id"))
                                .add(UIMultiSelect("readonlyAccessUsers", lc,
                                        label = "addressbook.readonlyAccess",
                                        additionalLabel = "access.users",
                                        values = allUsers,
                                        labelProperty = "fullname",
                                        valueProperty = "id")))
                        .add(UICol()
                                .add(UIMultiSelect("fullAccessGroups", lc,
                                        label = "addressbook.fullAccess",
                                        additionalLabel = "access.groups",
                                        values = allGroups,
                                        labelProperty = "name",
                                        valueProperty = "id"))
                                .add(UIMultiSelect("readonlyAccessGroups", lc,
                                        label = "addressbook.readonlyAccess",
                                        additionalLabel = "access.groups",
                                        values = allGroups,
                                        labelProperty = "name",
                                        valueProperty = "id"))))
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
