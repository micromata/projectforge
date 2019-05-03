package org.projectforge.rest

import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.address.AddressbookFilter
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.UserDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/addressBook")
class AddressBookRest() : AbstractStandardRest<AddressbookDO, AddressbookDO, AddressbookDao, AddressbookFilter>(AddressbookDao::class.java, AddressbookFilter::class.java, "addressbook.title") {

    @Autowired
    var groupService: GroupService? = null

    @Autowired
    var userDao: UserDao? = null

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
        val groupDOs = groupService?.sortedGroups
        val groups = mutableListOf<UISelectValue<Int>>()
        groupDOs?.forEach {
            groups.add(UISelectValue(it.id, it.name))
        }

       // val usersProvider = UsersProvider(userDao)
        //val userDOs = usersProvider.sortedUsers
        val users = mutableListOf<UISelectValue<Int>>()
        //userDOs?.forEach {
        //    groups.add(UISelectValue(it.id, it.fullname))
        //}

        val layout = super.createEditLayout(dataObject)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "title")
                                .add(lc, "description"))
                        .add(UICol()
                                .add(lc, "owner")))
                .add(UIRow()
                        .add(UICol()
                                .add(UIMultiSelect("userList", lc,
                                        values = users,
                                        labelProperty = "fullname",
                                        valueProperty = "id"))
                                .add(UIMultiSelect("userList", lc,
                                        values = users,
                                        labelProperty = "fullname",
                                        valueProperty = "id")))
                        .add(UICol()
                                .add(UIMultiSelect("userList", lc,
                                        values = groups,
                                        labelProperty = "name",
                                        valueProperty = "id"))
                                .add(UIMultiSelect("userList", lc,
                                        values = groups,
                                        labelProperty = "name",
                                        valueProperty = "id"))))
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
