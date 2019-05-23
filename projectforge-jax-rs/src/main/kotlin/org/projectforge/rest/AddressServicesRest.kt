package org.projectforge.rest

import org.apache.commons.collections.CollectionUtils
import org.apache.commons.io.IOUtils
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressExport
import org.projectforge.business.address.AddressFilter
import org.projectforge.business.address.PersonalAddressDao
import org.projectforge.framework.time.DateHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.rest.core.ListFilterService
import org.projectforge.rest.core.ReplaceUtils
import org.projectforge.rest.core.ResultSet
import org.projectforge.ui.UIStyle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import javax.servlet.http.HttpServletRequest

/**
 * For uploading address immages.
 */
@RestController
@RequestMapping("${Rest.URL}/address")
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
    private lateinit var listFilterService: ListFilterService

    @Autowired
    private lateinit var addressRest: AddressRest

    @Autowired
    private lateinit var addressExport: AddressExport

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    @GetMapping("exportFavoritesVCards")
    fun exportFavoritesVCards(): ResponseEntity<Any> {
        log.info("Exporting personal address book as vcards.")
        val list = addressDao.favoriteVCards
        if (CollectionUtils.isEmpty(list) == true) {
            return ResponseEntity(ResponseData("address.book.hasNoVCards", messageType = MessageType.TOAST, style = UIStyle.WARNING), HttpStatus.NOT_FOUND)
        }
        val filename = ("ProjectForge-PersonalAddressBook_" + DateHelper.getDateAsFilenameSuffix(Date())
                + ".vcf")
        val writer = StringWriter()
        addressDao.exportFavoriteVCards(writer, list)

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
                .body(writer.toString());
    }

    /**
     * Exports all the addresses with the last used filter. If the user works with different browser windows and devices, the result may not match
     * the current displayed list. The recent search result is used (stored in [AbstractDORest.getList] or [AbstractDORest.getInitialList].
     */
    @GetMapping("exportFavoritesExcel")
    fun exportFavoritesExcel(request: HttpServletRequest): ResponseEntity<Any> {
        log.info("Exporting personal address book as Excel file.")
        var storedFilter = listFilterService.getSearchFilter(request.session, AddressFilter::class.java)
        val list = addressDao.getList(storedFilter)
        val resultSet = ResultSet<Any>(list, list.size)
        addressRest.processResultSetBeforeExport(resultSet)

        val personalAddressMap = personalAddressDao.getPersonalAddressByAddressId()

        val xls = addressExport.export(list, personalAddressMap)
        if (xls == null || xls.size == 0) {
            return ResponseEntity(ResponseData("address.book.hasNoVCards", messageType = MessageType.TOAST, style = UIStyle.WARNING), HttpStatus.NOT_FOUND)
        }
        val filename = ("ProjectForge-AddressExport_" + DateHelper.getDateAsFilenameSuffix(Date())
                + ".xls")

        val resource = ByteArrayResource(xls)
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
                .body(resource);
    }

    @GetMapping("exportFavoritePhoneList")
    fun exportFavoritePhoneList(): ResponseEntity<Any> {
        log.info("Exporting personal phone list as txt file.")
        val list = addressDao.favoritePhoneEntries
        if (CollectionUtils.isEmpty(list) == true) {
            return ResponseEntity(ResponseData("address.book.hasNoPhoneNumbers", messageType = MessageType.TOAST, style = UIStyle.WARNING), HttpStatus.NOT_FOUND)
        }
        val filename = ("ProjectForge-PersonalPhoneList_" + DateHelper.getDateAsFilenameSuffix(Date())
                + ".txt")
        val writer = StringWriter()
        addressDao.exportFavoritePhoneList(writer, list)
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
                .body(writer.toString());
    }

    @GetMapping("downloadAppleScript")
    fun downloadAppleScript(): ResponseEntity<Any> {
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
        val resource = ByteArrayResource(content!!)
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
                .body(resource);
    }

    @GetMapping("exportVCard/{id}")
    fun exportVCard(@PathVariable("id") id: Int?): ResponseEntity<Any> {
        val address = addressDao.getById(id)
        if (address == null)
            return ResponseEntity(HttpStatus.NOT_FOUND)
        val filename = ("ProjectForge-" + ReplaceUtils.encodeFilename(address.fullName) + "_"
                + DateHelper.getDateAsFilenameSuffix(Date()) + ".vcf")
        val writer = StringWriter()
        addressDao.exportVCard(PrintWriter(writer), address)
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
                .body(writer.toString());
    }
}
