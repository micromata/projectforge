package org.projectforge.rest.poll

import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.Constants
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.poll.PollResponseDO
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.poll.types.*
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID
import javax.servlet.http.HttpServletRequest


@RestController
@RequestMapping("${Rest.URL}/antwort")
class ResponsePageRest : AbstractDynamicPageRest() {

    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    @GetMapping("dynamic")
    fun test(request: HttpServletRequest, @RequestParam("id") pollStringId: String?): FormLayoutData {
        //val lc = LayoutContext(PollDO::class.java)
        val id = NumberHelper.parseInteger(pollStringId) ?: throw IllegalArgumentException("id not given.")
        val pollData = pollDao.internalGetById(id) ?: PollDO()
        val pollDto = transformPollFromDB(pollData)

        val layout = UILayout("poll.antwort.title")
        val fieldSet = UIFieldset(12, title = pollDto.title)
        fieldSet
            .add(UIReadOnlyField(value = pollDto.description, label = "Description"))
            .add(UIReadOnlyField(value = pollDto.location, label = "Location"))
            .add(UIReadOnlyField(value = pollDto.owner?.displayName, label = "Owner"))
            .add(UIReadOnlyField(value = pollDto.deadline.toString(), label = "Deadline"))

        layout.add(fieldSet)

        val pollResponse = PollResponse()
        pollResponse.poll = pollData

        pollResponseDao.internalLoadAll().firstOrNull { response ->
            response.owner == ThreadLocalUserContext.user
                    && response.poll?.id == pollData.id
        }?.let {
                pollResponse.copyFrom(it)
            }

        //addQuestions(layout, pollDto)
        pollDto.inputFields?.forEachIndexed { index, field ->
            val fieldSet2 = UIFieldset(title = field.question)
            val answer = Answer()
            answer.uid = UUID.randomUUID().toString()
            answer.questionUid = field.uid
            pollResponse.responses?.firstOrNull() {
                it.questionUid==field.uid
            }.let {
                if (it == null)
                    pollResponse.responses?.add(answer)
            }

            val col = UICol()

            if (field.type == BaseType.FreiTextFrage) {
                col.add(UITextArea("responses[$index].antworten[0]"))
            }
            if (field.type == BaseType.JaNeinFrage) {
                col.add(UIRadioButton("responses[$index].antworten[0]", value = field.antworten!![0], label = field.antworten?.get(0) ?: ""))
                col.add(UIRadioButton("responses[$index].antworten[0]", value = field.antworten!![1], label = field.antworten?.get(1) ?: ""))
            }
            if (field.type == BaseType.DatumsAbfrage) {
                col.add(UITextArea("responses[$index].antworten[0]"))
            }
            if (field.type == BaseType.DropDownFrage) {
                col.add(UISelect("responses[$index].antworten[0]", values = field.antworten?.map { UISelectValue(it, it) }))
            }
            if (field.type == BaseType.MultipleChoices) {
                //for (i in 1 until field.antworten!!.size)
                    //answer.antworten?.add("")


                field.antworten?.forEachIndexed { index2, _ ->
                    col.add(UICheckbox("responses[$index].antworten[$index2]", label = field.antworten?.get(index2) ?: ""))
                }
            }
            fieldSet2.add(UIRow().add(col))
            layout.add(fieldSet2)
        }

        layout.add(UIButton.createDefaultButton(
            id = "doResponse",
            title = "response",
            responseAction = ResponseAction(
                RestResolver.getRestUrl(
                    this::class.java,
                    "doResponse"
                ), targetType = TargetType.POST
            )
        ))

        return FormLayoutData(pollResponse, layout, createServerData(request))
    }

    @PostMapping("doResponse")
    fun doResponse(
        request: HttpServletRequest,
        @RequestBody postData: PostData<PollResponse>
    ): ResponseAction {

        val pollResponseDO = PollResponseDO()
        postData.data.copyTo(pollResponseDO)
        pollResponseDO.owner = ThreadLocalUserContext.user

        pollResponseDao.internalLoadAll().firstOrNull { pollResponse ->
            pollResponse.owner == ThreadLocalUserContext.user
                    && pollResponse.poll?.id == postData.data.poll?.id }
            ?.let {
                it.responses = pollResponseDO.responses
                pollResponseDao.update(it)
                return ResponseAction(targetType = TargetType.REDIRECT, url = "")
            }

        pollResponseDao.saveOrUpdate(pollResponseDO)
        return ResponseAction(targetType = TargetType.REDIRECT, url = PagesResolver.getListPageUrl(PollPageRest::class.java, absolute = true))
    }



    private fun transformPollFromDB(obj: PollDO): Poll {
        val poll = Poll()
        poll.copyFrom(obj)
        if (obj.inputFields != null) {
            val a = ObjectMapper().readValue(obj.inputFields, MutableList::class.java)
            poll.inputFields = a.map { Frage().toObject(ObjectMapper().writeValueAsString(it)) }.toMutableList()
        }
        return poll
    }
}