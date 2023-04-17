package org.projectforge.rest.poll

import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.poll.types.BaseType
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest


@RestController
@RequestMapping("${Rest.URL}/poll/antwort")
class ResponsePageRest : AbstractDynamicPageRest() {




    @GetMapping("dynamic")
    fun test(request: HttpServletRequest, @RequestParam("pollId") pollId: Int?): FormLayoutData {

        val layout = UILayout("poll.antwort.title")
        val lc = LayoutContext(PollDO::class.java)
        val data = PollDao().getById(1)
        val dto = Poll()
        dto.copyFrom(data)


        layout.add(
            UIRow().add(
                UIFieldset(UILength(md = 6, lg = 4)).add(lc, "title", "description", "location", "owner", "deadline")
            )
        )
            addQuestions(layout, dto)

        return FormLayoutData(data, layout, createServerData(request))
    }

    private fun addQuestions(layout: UILayout, dto: Poll) {



        dto.inputFields?.forEachIndexed { index, field ->
            val feld = UIRow()
            feld.add(UIFieldset(UILength(md = 6, lg = 4), title = field.type.toString()))
                .add(UICol(6).add(UILabel(field.question)))

            if (field.type == BaseType.FreiTextFrage) {
                feld.add(UIInput("antwort",))
            }
            if (field.type == BaseType.JaNeinFrage) {
                feld.add(UICheckbox("antwort",))
                feld.add(UICheckbox("antwort",))
            }
            if (field.type == BaseType.DatumsAbfrage){

                feld.add(UICheckbox("antwort",))
                feld.add(UICheckbox("antwort",))
                feld.add(UICheckbox("antwort",))
            }
            if (field.type == BaseType.DropDownFrage){
                feld.add(UISelect("questionType", values = field.antworten?.map { UISelectValue(it,it) }))
            }
            if( field.type == BaseType.MultipleChoices){
                field.antworten?.forEachIndexed{ index, s ->
                    feld.add(UICheckbox("antwort${index}", label =field.antworten?.get(index) ?: ""))
                }
            }
            layout.add(feld)
        }
    }

}