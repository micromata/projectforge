package org.projectforge.rest.poll

import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.framework.i18n.translate
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
        return pollDO
    }

    override fun transformFromDB(obj: PollDO, editMode: Boolean): Poll {
        val poll = Poll()
        poll.copyFrom(obj)
        return poll
    }

    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            userAccess = userAccess,
        )
            .add(lc, "title", "description", "location")
            .add(lc, "owner")
            .add(lc, "deadline")
        layout.add(
            MenuItem(
                "export",
                i18nKey = "poll.export.title",
                url = PagesResolver.getDynamicPageUrl(VacationExportPageRest::class.java),
                type = MenuItemTargetType.REDIRECT,
            )
        )
    }

    private fun addQuestionFieldset(dto: Poll): List<UIRow>{

        val rows = mutableListOf<UIRow>()
        dto.inputFields?.forEach { field ->
            if(field.type == null){
                return rows
            }

            var feld = UIRow()
                    if(field.type == BaseType.JaNeinFrage){
                       feld.add(
                           UIFieldset(UILength(md = 6, lg = 4))
                           .add(UIInput (field.uid+"question"))
                           .add(UICheckbox(field.uid+"antworten1", label = "Ja"))
                           .add(UICheckbox(field.uid+"antworten2", label = "Nein")))}

                    if(field.type == BaseType.FreiTextFrage){
                    feld.add(UIFieldset(UILength(md = 6, lg = 4))
                        .add(UIInput (field.uid+"question"))
                        .add(UIInput(field.uid+"antworten")
                    ))}

                    if(field.type == BaseType.MultipleChoices){
                    feld.add(UIFieldset(UILength(md = 6, lg = 4))
                        .add(lc, "question","antworten")
                        .add( UIButton.createAddButton(
                            responseAction = ResponseAction("${Rest.URL}/poll/addAntwortmöglichkeite", targetType = TargetType.POST)
                        )))
                    }


            rows.add(feld)
        }
        return rows
    }

    override fun createEditLayout(dto: Poll, userAccess: UILayout.UserAccess): UILayout {
        val lc = LayoutContext(PollDO::class.java)
        val obj = PollDO()
        dto.copyTo(obj)
        val layout = super.createEditLayout(dto, userAccess)
        layout.add(
            UIRow()
                .add(
                    UIFieldset(UILength(md = 6, lg = 4))
                        .add(lc, "title", "description", "location", "owner", "deadline")
                )
        )
        layout.add(
            UIRow()
                .add(
                    UIFieldset(UILength(md = 6, lg = 4))
                .add(
                UIButton.createAddButton(
                    responseAction = ResponseAction("${Rest.URL}/poll/add", targetType = TargetType.POST)
                ))
                .add(
                    UISelect(
                        "questionType",
                        values = BaseType.values().map {UISelectValue(it, it.name)}
                    )

        )))
        addQuestionFieldset(dto).forEach(layout::add)


        layout.watchFields.addAll(
            arrayOf(
                "title",
                "description",
                "location",
                "deadline"
            )
        )
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override fun onWatchFieldsUpdate(
        request: HttpServletRequest,
        dto: Poll,
        watchFieldsTriggered: Array<String>?
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
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("data", dto)
                .addVariable("ui", createEditLayout(dto, userAccess))
        )
    }

    // PostMapping add
    @PostMapping("/add")
    fun abc(
        @RequestBody postData: PostData<Poll>,
        ): ResponseEntity<ResponseAction> {
        val userAccess = UILayout.UserAccess(insert = true, update = true)
        val dto = postData.data
        var type = BaseType.valueOf(dto.questionType?:"FreiTextFrage")


        val poll = PollDO()
        if (dto.inputFields == null) {
            dto.inputFields = mutableListOf()
        }
        dto.inputFields!!.add(Frage(uid = UUID.randomUUID().toString(), type = type))

        dto.copyTo(poll)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("data", dto)
                .addVariable("ui", createEditLayout(dto, userAccess))
        )
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