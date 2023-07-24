/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest


@RestController
@RequestMapping("${Rest.URL}/pollInfo")
class PollInfoPageRest : AbstractDynamicPageRest() {

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {

        val layout = UILayout("poll.infopage")
        val field = UIFieldset()
            .add(
                UILabel(
                    """ Anleitung, um eine Umfrage zu erstellen 
                    | Als erstes werden die Parameter einer Umfrage angelegt.
                """.trimMargin()
                )
            )

        field.add(
            UICol()
                .add(UIReadOnlyField("title", label = "title", value = "Test"))
        )
        field.add(
            UICol()
                .add(UIReadOnlyField("description", label = "description", value = "description"))
        )
        field.add(
            UICol()
                .add(UIReadOnlyField("location", label = "location", value = "location"))
        )
        field.add(
            UICol()
                .add(UIReadOnlyField("owner", label = "owner", value = "owner"))
        )
        field.add(
            UICol()
                .add(UIReadOnlyField("deadline", label = "deadline", value = "deadline"))
        )

        field.add(
            UIRow().add(
                UICol().add(
                    UILabel(
                        """Anschließend werden die Fragen der Umfrage angelegt.
                    Die Fragen können aus verschiedenen Typen bestehen. """
                    )
                )
            )
        )

        layout.add(field)

        layout.add(
            UIFieldset().add(UILabel("YesNoQuestion")).add(
                UICol()
                    .add(
                        UIReadOnlyField(
                            "question",
                            label = "Question",
                            value = "Eine Frage, die mit Ja oder Nein beantwortet werden kann."
                        )
                    )
            )
        )
        layout.add(
            UIFieldset().add(UILabel("MultipleChoiceQuestion")).add(
                UICol()
                    .add(
                        UIReadOnlyField(
                            "question",
                            label = "Question",
                            value = "Eine Frage, die mit mehreren Antworten beantwortet werden kann."
                        )
                    )
            )
        )
        layout.add(
            UIFieldset().add(UILabel("TextQuestion")).add(
                UICol()
                    .add(
                        UIReadOnlyField(
                            "question",
                            label = "Question",
                            value = "Eine Frage, die mit einer Freitext Antwort beantwortet werden kann."
                        )
                    )

            )
        )
        layout.add(
            UIFieldset().add(UILabel("Dropdown")).add(
                UICol()
                    .add(
                        UIReadOnlyField(
                            "question", label = "Question",
                            value = "Eine Frage, die mit einem Dropdown beantwortet werden kann."
                        )
                    )
            )
        )


        LayoutUtils.process(layout)

        return FormLayoutData(null, layout, createServerData(request))
    }
}
