/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest.poll

import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletRequest


@RestController
@RequestMapping("${Rest.URL}/pollInfo")
class PollInfoPageRest : AbstractDynamicPageRest() {

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {

        val layout = UILayout("poll.infopage")
        val field = UIFieldset()
            .add(
                UILabel("poll.manual.title")
            )

        field.add(
            UICol()
                .add(UIReadOnlyField("title", label = "title", value = translate("poll.title")))
        )
        field.add(
            UICol()
                .add(UIReadOnlyField("description", label = "description", value = translate("poll.description")))
        )
        field.add(
            UICol()
                .add(UIReadOnlyField("location", label = "poll.location", value = translate("poll.location")))
        )
        field.add(
            UICol()
                .add(UIReadOnlyField("owner", label = "poll.owner", value = translate("poll.owner")))
        )
        field.add(
            UICol()
                .add(UIReadOnlyField("deadline", label = "deadline", value = translate("poll.deadline")))
        )

        field.add(
            UIRow().add(
                UICol().add(
                    UILabel("poll.manual.questions")
                )
            )
        )

        layout.add(field)

        layout.add(
            UIFieldset().add(UILabel(translate("poll.question.singleResponseQuestionTitle"))).add(
                UICol()
                    .add(
                        UIReadOnlyField(
                            "question",
                            label = "poll.question.single",
                            value = translate("poll.manual.singleResponse")
                        )
                    )
            )
        )
        layout.add(
            UIFieldset().add(UILabel(translate("poll.question.multiResponseQuestionTitle"))).add(
                UICol()
                    .add(
                        UIReadOnlyField(
                            "question",
                            label = "poll.question.multi",
                            value = translate("poll.manual.multiResponse")
                        )
                    )
            )
        )
        layout.add(
            UIFieldset().add(UILabel(translate("poll.question.textQuestionTitle"))).add(
                UICol()
                    .add(
                        UIReadOnlyField(
                            "question",
                            label = "poll.question.text",
                            value = translate("poll.manual.textQuestion")
                        )
                    )

            )
        )


        LayoutUtils.process(layout)

        return FormLayoutData(null, layout, createServerData(request))
    }
}