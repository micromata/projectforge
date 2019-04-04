package org.projectforge.rest

import com.google.gson.*
import org.apache.commons.lang3.StringUtils
import org.glassfish.jersey.media.multipart.FormDataMultiPart
import org.projectforge.business.address.*
import org.projectforge.business.image.ImageService
import org.projectforge.framework.i18n.translate
import org.projectforge.menu.MenuItem
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.rest.core.AbstractDORest.Companion.GEAR_MENU
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.ResultSet
import org.projectforge.sms.SmsSenderConfig
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.InputStream
import java.lang.reflect.Type
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
class AddressImageRest() {

    companion object {
        internal val SESSION_IMAGE_ATTR = "uploadedAddressImage"
    }

    private val log = org.slf4j.LoggerFactory.getLogger(AddressImageRest::class.java)

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var imageService: ImageService

    /**
     * If given and greater 0, the image will be added to the address with the given id (pk), otherwise the image is
     * stored in the user's session and will be used for the next update or save event.
     */
    @POST
    @Path("uploadImage/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadFile(@PathParam("id") id: Int, @Context request: HttpServletRequest, form: FormDataMultiPart): Response {
        val filePart = form.getField("file")
        val headerOfFilePart = filePart.getContentDisposition()
        val fileInputStream = filePart.getValueAs(InputStream::class.java)
        val filename = headerOfFilePart.getFileName()
        if (!filename.endsWith(".png", true)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Unsupported file: ${filename}. Only png files supported").build()
        }
        val bytes = fileInputStream.readBytes()
        if (id == null || id < 0) {
            val session = request.session
            ExpiringSessionAttributes.setAttribute(session, SESSION_IMAGE_ATTR, bytes, 1)
        } else {
            val address = addressDao.getById(id)
            if (address == null)
                return Response.status(Response.Status.NOT_FOUND).build()
            address.imageData = bytes
            addressDao.update(address)
            log.info("New image for address $id (${address.fullName}) saved.")
        }
        return Response.ok().build()
    }

    @GET
    @Path("image/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun getImage(@PathParam("id") id: Int): Response {
        val address = addressDao.getById(id)
        if (address?.imageData == null)
            return Response.status(Response.Status.NOT_FOUND).build()

        val builder = Response.ok(address.imageData)
        builder.header("Content-Disposition", "attachment; filename=ProjectForge-addressImage_$id.png")
        return builder.build()
    }

    @GET
    @Path("imagePreview/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun getImagePreview(@PathParam("id") id: Int): Response {
        val address = addressDao.getById(id)
        if (address?.imageData == null)
            return Response.status(Response.Status.NOT_FOUND).build()

        val builder = Response.ok(address.imageDataPreview)
        builder.header("Content-Disposition", "attachment; filename=ProjectForge-addressImagePreview_$id.png")
        return builder.build()
    }

    /**
     * If given and greater 0, the image will be deleted from the address with the given id (pk), otherwise the image is
     * removed from the user's session and will not be used for the next update or save event anymore.
     */

    @DELETE
    @Path("deleteImage/{id}")
    fun deleteImage(@Context request: HttpServletRequest, @PathParam("id") id: Int): Response {
        if (id == null || id < 0) {
            val session = request.session
            ExpiringSessionAttributes.removeAttribute(session, SESSION_IMAGE_ATTR)
        } else {
            val address = addressDao.getById(id)
            if (address == null)
                return Response.status(Response.Status.NOT_FOUND).build()
            address.imageData = null
            address.imageDataPreview = null
            addressDao.update(address)
            log.info("Image for address $id (${address.fullName}) deleted.")
        }
        return Response.ok().build()
    }
}
