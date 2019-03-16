package org.projectforge.rest.ui

import org.projectforge.rest.JsonUtils
import org.springframework.stereotype.Controller
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Controller
@Path("layout")
open class LayoutRest {
    @GET
    @Path("list/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getListLayout(@PathParam("id") id: String?): Response {
        var layout = when (id) {
            "address" -> AddressLayout.createListLayout()
            "book" -> BookLayout.createListLayout()
            else -> null
        }
        val json = JsonUtils.toJson(layout)
        return Response.ok(json).build()
    }
}