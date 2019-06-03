package org.projectforge.rest

import org.projectforge.rest.config.Rest
import org.projectforge.ui.UILayout
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("${Rest.URL}/index")
class IndexRest {
    @GetMapping
    fun getTranslations(): UILayout {
        val layout = UILayout("")
        layout.addTranslations("goreact.index.classics.header",
                "goreact.index.classics.body1",
                "goreact.index.classics.body2",
                "goreact.index.react.header",
                "goreact.index.react.body1",
                "goreact.index.react.body2",
                "goreact.index.both.header",
                "goreact.index.both.body1",
                "goreact.index.both.body2")
        return layout
    }
}
