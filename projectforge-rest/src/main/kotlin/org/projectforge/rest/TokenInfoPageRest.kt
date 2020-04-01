package org.projectforge.rest

import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UILayout
import org.projectforge.ui.UIReadOnlyField
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/tokenInfo")
class TokenInfoPageRest : AbstractDynamicPageRest() {

    class TokenInfoData(var info: String? = null)

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        // TODO REPLACE STRING
        val data = TokenInfoData("Test String")

        val layout = UILayout("info")

        layout.add(UIReadOnlyField(id = "info"))

        LayoutUtils.process(layout)

        return FormLayoutData(data, layout, createServerData(request))
    }

}
