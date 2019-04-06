package org.projectforge.rest

import org.apache.commons.collections.CollectionUtils
import org.apache.commons.io.IOUtils
import org.projectforge.business.address.*
import org.projectforge.framework.time.DateHelper
import org.projectforge.rest.core.*
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * For uploading address immages.
 */
@Component
@Path("address")
class AddressServicesRest() {

    companion object {
        internal val SESSION_IMAGE_ATTR = "uploadedAddressImage"
        private val APPLE_SCRIPT_DIR = "misc/"
        private val APPLE_SCRIPT_FOR_ADDRESS_BOOK = "AddressBookRemoveNotesOfClassWork.scpt"
    }

    private val log = org.slf4j.LoggerFactory.getLogger(AddressServicesRest::class.java)

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var listFilterService : ListFilterService

    @Autowired
    private lateinit var addressRest : AddressRest

    @Autowired
    private lateinit var addressExport : AddressExport

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    private val restHelper = RestHelper()

    @GET
    @Path("exportFavoritesVCards")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun exportFavoritesVCards(): Response {
        log.info("Exporting personal address book as vcards.")
        val list = addressDao.favoriteVCards
        if (CollectionUtils.isEmpty(list) == true) {
            return restHelper.buildResponse(ResponseData("address.book.hasNoVCards", messageType = MessageType.TOAST, style = UIStyle.WARNING))
        }
        val filename = ("ProjectForge-PersonalAddressBook_" + DateHelper.getDateAsFilenameSuffix(Date())
                + ".vcf")
        val writer = StringWriter()
        addressDao.exportFavoriteVCards(writer, list)
        val builder = Response.ok(writer.toString())
        builder.header("Content-Disposition", "attachment; filename=$filename")
        return builder.build()
    }

    /**
     * Exports all the addresses with the last used filter. If the user works with different browser windows and devices, the result may not match
     * the current displayed list. The recent search result is used (stored in [AbstractStandardRest.getList] or [AbstractStandardRest.getInitialList].
     */
    @GET
    @Path("exportFavoritesExcel")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun exportFavoritesExcel(@Context request: HttpServletRequest): Response {
        log.info("Exporting personal address book as Excel file.")
        var storedFilter = listFilterService.getSearchFilter(request.session, AddressFilter::class.java)
        val list = addressDao.getList(storedFilter)
        val resultSet = ResultSet<Any>(list, list.size)
        addressRest.processResultSetBeforeExport(resultSet)

        val personalAddressMap = personalAddressDao.getPersonalAddressByAddressId()

        val xls = addressExport.export(list, personalAddressMap)
        if (xls == null || xls.size == 0) {
            return restHelper.buildResponse(ResponseData("address.book.hasNoVCards", messageType = MessageType.TOAST, style = UIStyle.WARNING))
        }
        val filename = ("ProjectForge-AddressExport_" + DateHelper.getDateAsFilenameSuffix(Date())
                + ".xls")
        val builder = Response.ok(xls)
        builder.header("Content-Disposition", "attachment; filename=$filename")
        return builder.build()
    }

    @GET
    @Path("exportFavoritePhoneList")
    @Produces(MediaType.TEXT_PLAIN)
    fun exportFavoritePhoneList(): Response {
        log.info("Exporting personal phone list as txt file.")
        val list = addressDao.favoritePhoneEntries
        if (CollectionUtils.isEmpty(list) == true) {
            return restHelper.buildResponse(ResponseData("address.book.hasNoPhoneNumbers", messageType = MessageType.TOAST, style = UIStyle.WARNING))
        }
        val filename = ("ProjectForge-PersonalPhoneList_" + DateHelper.getDateAsFilenameSuffix(Date())
                + ".txt")
        val writer = StringWriter()
        addressDao.exportFavoritePhoneList(writer, list)
        val builder = Response.ok(writer.toString())
        builder.header("Content-Disposition", "attachment; filename=$filename")
        return builder.build()
    }

    @GET
    @Path("downloadAppleScript")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadAppleScript(): Response {
        log.info("Downloading AppleScript.")
        var content: ByteArray?
        val file = APPLE_SCRIPT_DIR + APPLE_SCRIPT_FOR_ADDRESS_BOOK
        try {
            val cLoader = this.javaClass.classLoader
            val inputStream = cLoader.getResourceAsStream(file)
            if (inputStream == null) {
                log.error("Could not find script in resource path: '$file'.")
            }
            content = IOUtils.toByteArray(inputStream!!)
        } catch (ex: IOException) {
            log.error("Could not load script '" + file + "'." + ex.message, ex)
            throw RuntimeException(ex)
        }
        val filename = (APPLE_SCRIPT_FOR_ADDRESS_BOOK)
        val builder = Response.ok(content)
        builder.header("Content-Disposition", "attachment; filename=$filename")
        return builder.build()
    }

    @GET
    @Path("exportVCard/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun exportVCard(@PathParam("id") id: Int): Response {
        val address = addressDao.getById(id)
        val filename = ("ProjectForge-" + ReplaceUtils.encodeFilename(address.getFullName()) + "_"
                + DateHelper.getDateAsFilenameSuffix(Date()) + ".vcf")
        val writer = StringWriter()
        addressDao.exportVCard(PrintWriter(writer), address)
        val builder = Response.ok(writer.toString())
        builder.header("Content-Disposition", "attachment; filename=$filename")
        return builder.build()
    }
}
