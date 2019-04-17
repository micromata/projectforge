package org.projectforge.rest

import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressbookDao
import org.projectforge.rest.core.RestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * For uploading address immages.
 */
@Component
@Path("addressBook")
class AddressBookServicesRest() {

    private val log = org.slf4j.LoggerFactory.getLogger(AddressBookServicesRest::class.java)

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    private val restHelper = RestHelper()

    init {
        restHelper.add(AddressbookDO::class.java, AddressRest.AddressbookDOSerializer())
    }

    /**
     * Gets the autocomletion list for the given property and search string.
     * @param search
     * @return list of strings as json.
     */
    @GET
    @Path("ac")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAutoCompletion(@QueryParam("search") search: String?): Response {
        val addressBooks = addressbookDao.getAllAddressbooksWithFullAccess().toMutableList()
        addressBooks.sortBy({ it.title })
        addressBooks.removeIf { it.isDeleted || !it.title.contains(search ?: "", true) }
        return restHelper.buildResponse(addressBooks)
    }
}
