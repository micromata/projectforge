package org.projectforge.rest

import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/birthdayListExporter")
class BirthdayListPageRest : AbstractDynamicPageRest() {

    @GetMapping("dynamic")
    fun getForm(
        request: HttpServletRequest,
        @RequestParam("userId") userIdString: String?,
    ): FormLayoutData {
        val layout = UILayout("BirthdayListExporter")

        val months = arrayOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
        val values = months?.map { UISelectValue(it, it) }
        layout.add(UISelect("value.textValue", values = values))

        layout.addAction(UIButton.createDownloadButton(
            id = "downloadAll",
            title = "Download all",
            responseAction = ResponseAction(
                RestResolver.getRestUrl(
                    this.javaClass,
                    "downloadAll/2"
                ), targetType = TargetType.DOWNLOAD
            ),
            default = true
        ))

        LayoutUtils.process(layout) // Macht i18n und so...

        val data = BirthdayListData()
        return FormLayoutData(data, layout, createServerData(request))
}
}