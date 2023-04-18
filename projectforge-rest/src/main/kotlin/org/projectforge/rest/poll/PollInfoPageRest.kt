package org.projectforge.rest.poll

import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest


@RestController
@RequestMapping("${Rest.URL}/pollInfo")
class PollInfoPageRest {
    class Data(
        var user: String? = null,
        var password: String? = null,
        var server: String? = null,
        var serverPath: String? = null,
        var ios: String? = null,
        var thunderbird: String? = null
    )

        @Autowired
        private lateinit var authenticationsService: UserAuthenticationsService

        @GetMapping("dynamic")
        fun getForm(request: HttpServletRequest): FormLayoutData {
            val username = ThreadLocalUserContext.user?.username ?: "?????"
            val layout = UILayout("poll.infopage")
                .add(UILabel(""" Eine Poll """))
                layout.add(UICol()
                        .add(UIReadOnlyField("user", label = "user")))


            LayoutUtils.process(layout)

            val data = Data(
                user = username,
                password = authenticationsService.getToken(ThreadLocalUserContext.userId!!, UserTokenType.DAV_TOKEN),
                server = request.serverName,
                serverPath = "/users/${username}/addressBooks/default",
                ios = "CardDAV account",
                thunderbird = "CardDAV account"
            )

            return FormLayoutData(data, layout, null)
        }

}