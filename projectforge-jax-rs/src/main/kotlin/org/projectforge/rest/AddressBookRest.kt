package org.projectforge.rest

import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.address.AddressbookFilter
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.ui.*
import org.springframework.stereotype.Component
import javax.ws.rs.Path

@Component
@Path("addressBook")
class AddressBookRest() : AbstractStandardRest<AddressbookDO, AddressbookDao, AddressbookFilter>(AddressbookDao::class.java, AddressbookFilter::class.java, "addressbook.title") {

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "title", "description", "owner", "accessright", "last modification"))
        layout.getTableColumnById("owner").formatter = Formatter.USER
        layout.getTableColumnById("last modification").formatter = Formatter.TIMESTAMP_MINUTES
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: AddressbookDO): UILayout {
        val layout = super.createEditLayout(dataObject)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "title")
                                .add(lc, "description"))
                        .add(UICol()
                                .add(lc, "owner")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "fullaccess_user")
                                .add(lc, "readaccess_user"))
                        .add(UICol()
                                .add(lc, "fullaccess_group")
                                .add(lc, "readaccess_group")))
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}