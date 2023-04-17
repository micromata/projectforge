package org.projectforge.rest.poll

import com.fasterxml.jackson.databind.ObjectMapper
import org.json.simple.JSONObject
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.VacationExportPageRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.poll.types.BaseType
import org.projectforge.rest.poll.types.Frage
import org.projectforge.ui.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/poll")
class PollPageRest : AbstractDTOPagesRest<PollDO, Poll, PollDao>(PollDao::class.java, "poll.title") {

    override fun transformForDB(dto: Poll): PollDO {
        val pollDO = PollDO()
        dto.copyTo(pollDO)
        if(dto.inputFields!= null){
            pollDO.inputFields = ObjectMapper().writeValueAsString(dto.inputFields)
        }
        return pollDO
    }


    //override fun transformForDB editMode not used
    override fun transformFromDB(obj: PollDO, editMode: Boolean): Poll {
        val poll = Poll()
        poll.copyFrom(obj)
        if(obj.inputFields!= null){
            var a = ObjectMapper().readValue(obj.inputFields, MutableList::class.java)
            poll.inputFields = a.map { Frage().toObject(ObjectMapper().writeValueAsString(it)) }.toMutableList()
        }
        return poll
    }

    override fun createListLayout(
        request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess
    ) {
        agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            userAccess = userAccess,
        ).add(lc, "title", "description", "location").add(lc, "owner").add(lc, "deadline")
        layout.add(
            MenuItem(
                "export",
                i18nKey = "poll.export.title",
                url = PagesResolver.getDynamicPageUrl(VacationExportPageRest::class.java),
                type = MenuItemTargetType.REDIRECT,
            )
        )
    }



    override fun createEditLayout(dto: Poll, userAccess: UILayout.UserAccess): UILayout {
        val lc = LayoutContext(PollDO::class.java)
        val obj = PollDO()
        dto.copyTo(obj)
        val layout = super.createEditLayout(dto, userAccess)
        layout.add(
            UIRow().add(
                UIFieldset(UILength(md = 6, lg = 4)).add(lc, "title", "description", "location", "owner", "deadline")
            )
        )
        layout.add(
            UIRow().add(
                UIFieldset(UILength(md = 6, lg = 4)).add(
                    UIButton.createAddButton(
                        responseAction = ResponseAction("${Rest.URL}/poll/add", targetType = TargetType.POST)
                    )
                ).add(
                    UISelect("questionType", values = BaseType.values().map { UISelectValue(it, it.name) })

                )
            )
        )
        addQuestionFieldset(layout, dto)


        layout.watchFields.addAll(
            arrayOf(
                "title", "description", "location", "deadline"
            )
        )
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override fun onWatchFieldsUpdate(
        request: HttpServletRequest, dto: Poll, watchFieldsTriggered: Array<String>?
    ): ResponseEntity<ResponseAction> {
        val title = dto.title
        val description = dto.description
        val location = dto.location
        val deadline = dto.deadline


        val userAccess = UILayout.UserAccess()
        val poll = PollDO()
        dto.copyTo(poll)
        checkUserAccess(poll, userAccess)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto).addVariable("ui", createEditLayout(dto, userAccess))
        )
    }

    @PostMapping("/addAntwort/{fieldId}")
    fun addAntwortFeld(
        @RequestBody postData: PostData<Poll>,
        @PathVariable("fieldId") fieldUid: String,
    ): ResponseEntity<ResponseAction> {
        val dto = postData.data
        val userAccess = UILayout.UserAccess(insert = true, update = true)

        val found = dto.inputFields?.find { it.uid == fieldUid }
        found?.antworten?.add("")

        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto).addVariable("ui", createEditLayout(dto, userAccess))
        )
    }

    // PostMapping add
    @PostMapping("/add")
    fun addFrageFeld(
        @RequestBody postData: PostData<Poll>,
    ): ResponseEntity<ResponseAction> {
        val userAccess = UILayout.UserAccess(insert = true, update = true)
        val dto = postData.data
        var type = BaseType.valueOf(dto.questionType ?: "FreiTextFrage")


        val poll = PollDO()
        if (dto.inputFields == null) {
            dto.inputFields = mutableListOf()
        }

        var frage = Frage(uid = UUID.randomUUID().toString(), type = type)
        if(type== BaseType.JaNeinFrage) {
            frage.antworten = mutableListOf("ja", "nein")
        }
        if(type== BaseType.DatumsAbfrage) {
            frage.antworten = mutableListOf("Ja", "Vielleicht", "Nein")
        }

        dto.inputFields!!.add(frage)

        dto.copyTo(poll)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto).addVariable("ui", createEditLayout(dto, userAccess))
        )
    }


    private fun addQuestionFieldset(layout: UILayout, dto: Poll) {

        dto.inputFields?.forEachIndexed { index, field ->
            val feld = UIRow()
            if (field.type == BaseType.JaNeinFrage) {
                val groupLayout = UIGroup()
                field.antworten?.forEach { antwort ->
                    groupLayout.add(
                        UIRadioButton(
                            "JaNeinRadio", antwort, label = antwort
                        )
                    )
                }
                feld.add(
                    UIFieldset(UILength(md = 6, lg = 4), title = field.type.toString()).add(UIInput("inputFields[${index}].question")).add
                        (groupLayout)
                )
            }

            if (field.type == BaseType.FreiTextFrage) {
                feld.add(
                    UIFieldset(UILength(md = 6, lg = 4), title = field.type.toString()).add(UIInput("inputFields[${index}].question"))
                )
            }

            if (field.type == BaseType.MultipleChoices || field.type == BaseType.DropDownFrage) {
                val f = UIFieldset(UILength(md = 6, lg = 4), title = field.type.toString())
                    .add(UIInput("inputFields[${index}].question", label = "Die Frage"))
                field.antworten?.forEachIndexed { i, _ ->
                    f.add(UIInput("inputFields[${index}].antworten[${i}]", label = "AntwortMöglichkeit ${i + 1}"))
                }
                f.add(
                    UIButton.createAddButton(
                        responseAction = ResponseAction(
                            "${Rest.URL}/poll/addAntwort/${field.uid}", targetType = TargetType.POST
                        )
                    )
                )
                if (field.type == BaseType.MultipleChoices) {
                    f.add(
                        UIInput(
                            "inputFields[${index}].numberOfSelect", dataType = UIDataType.INT, label = "Wie viele Sollen " +
                                    "angeklickt werden können "
                        )
                    )
                }
                feld.add(f)
            }
            if (field.type == BaseType.DatumsAbfrage) {
                feld.add(
                    UIFieldset(UILength(md = 6, lg = 4), title = field.type.toString()).add(
                        UIInput(
                            "inputFields[${index}].question",
                            label = "Hast du am ... Zeit?"
                        )
                    )

                )
            }
            layout.add(feld)
        }
    }

    // create a update layout funktion, welche das lyout nummr updatet und rurück gibt es soll für jeden Frage Basistyp eine eigene funktion haben


    /*dto.inputFields?.forEachIndexed { field, index ->
            if (field.type == msc) {
             layout.add() //
             "type[$index]"
              Id: name
            }
        }
        layout.add(UIRow().add(UIFieldset(UILength(md = 6, lg = 4))
            .add(lc, "name")))
        */
}