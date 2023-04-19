package org.projectforge.rest.poll

import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest


@RestController
@RequestMapping("${Rest.URL}/pollInfo")
class PollInfoPageRest: AbstractDynamicPageRest() {

        @GetMapping("dynamic")
        fun getForm(request: HttpServletRequest): FormLayoutData {

            val layout = UILayout("poll.infopage")
                val field = UIFieldset()
                .add(UILabel(""" Anleitung um eine Umfrage zu erstellen 
                    | Als erstes werden die Parameter einer Umfrage angelegt.
                """.trimMargin()))

            field.add(UICol()
                .add(UIReadOnlyField("title", label = "title", value = "Test")))
            field.add(UICol()
                .add(UIReadOnlyField("description", label = "description", value = "description")))
            field.add(UICol()
                .add(UIReadOnlyField("location", label = "location", value = "location")))
            field.add(UICol()
                .add(UIReadOnlyField("owner", label = "owner", value = "owner")))
            field.add(UICol()
                .add(UIReadOnlyField("deadline", label = "deadline", value = "deadline")))

            field.add(UIRow().add(UICol().add(UILabel("""  Anschließend werden die Fragen der Umfrage angelegt.
                    Die Fragen können aus verschiedenen Typen bestehen.
                """))))

            layout.add(field)

            layout.add(UIFieldset().add(UILabel("YesNoQuestion")).add(
                UICol()
                    .add(UIReadOnlyField("question", label = "Question", value = "Eine Frage die mit Ja oder Nein Beantwortet werden kann"))
            ))
            layout.add(UIFieldset().add(UILabel("MultipleChoiceQuestion")).add(
                UICol()
                    .add(UIReadOnlyField("question", label = "Question", value = "Eine Frage die mit mehreren Antworten Beantwortet werden kann"))
            ))
            layout.add(UIFieldset().add(UILabel("TextQuestion")).add(
                UICol()
                    .add(UIReadOnlyField("question", label = "Question", value = """Eine Frage die mit einer Freitext Antwort Beantwortet werden kann""".trimMargin()))

            ))
            layout.add(UIFieldset().add(UILabel("DateQuestion")).add(
                UICol()
                    .add(UIReadOnlyField("question", label = "Question",
                        value = """Eine Frage ob an einem Tag (Uhrzeit) die Teilnehmer zeit haben. Die einem Ja, Nein oder Vielleicht 
                            Beantwortet werden kann""".trimMargin()
            ))))
            layout.add(UIFieldset().add(UILabel("Dropdown")).add(
                UICol()
                    .add(UIReadOnlyField("question", label = "Question", value = """ Eine Frage die mit einem Dropdown Beantwortet werden
                        | kann""".trimMargin()
            ))))

            layout.add(
                UIRow().add(
                UIButton.createBackButton(
                    responseAction = ResponseAction(
                        PagesResolver.getEditPageUrl(
                            PollPageRest::class.java,
                            absolute = true,
                        ), targetType = TargetType.REDIRECT
                    ),
                    default = true
                )
                )
            )

            LayoutUtils.process(layout)

            return FormLayoutData(null, layout, createServerData(request))


        }

}