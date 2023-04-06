package org.projectforge.rest.poll

import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.mail.MailAttachment
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.VacationExportPageRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.*
import org.projectforge.rest.poll.Detail.View.PollDetailRest
import org.projectforge.rest.poll.Exel.ExcelExport
import org.projectforge.ui.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import javax.servlet.http.HttpServletRequest
import kotlin.collections.ArrayList

@RestController
@RequestMapping("${Rest.URL}/poll")
class PollPageRest : AbstractDTOPagesRest<PollDO, Poll, PollDao>(PollDao::class.java, "poll.title") {


    private val log: Logger = LoggerFactory.getLogger(PollDetailRest::class.java)

    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollMailService: PollMailService

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

    override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
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
                ))
            .add(UIButton.createAddButton(responseAction = ResponseAction("${Rest.URL}/poll/add", targetType = TargetType.POST)))

                layout.watchFields.addAll(
            arrayOf(
                "title",
                "description",
                "location",
                "owner",
                "deadline"
            )
        )
        updateStats(dto)
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
        /*
                val owner = dto.owner
        */
        val deadline = dto.deadline

        updateStats(dto)
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

    private fun updateStats(dto: Poll) {

        val title = dto.title
        val description = dto.description
        val location = dto.location
        /*
                val owner = dto.owner
        */
        val deadline = dto.deadline

        val pollDO = PollDO()
        dto.copyTo(pollDO)
    }

    @Scheduled(fixedRate = 50000)
    fun cronJobSch() {
        // check if poll end in Future
        val polls = pollDao.internalLoadAll()
        var mail = "";
        var header = "";
        // erstell mir eine funktion, die alles deadlines mir gibt die in der zukunft liegen
        val pollsInFuture = polls.filter { it.deadline?.isAfter(LocalDate.now()) ?: false}
        pollsInFuture.forEach{
            val daysDifference = ChronoUnit.DAYS.between(LocalDate.now(), it.deadline)
            if(daysDifference == 1L || daysDifference == 7L){
                header = "Umfrage Endet in $daysDifference Tage"
                mail ="""
                    Sehr geehrter Teilnehmer,wir laden Sie herzlich dazu ein, an unserer Umfrage zum Thema ${it.title} teilzunehmen. 
                    Ihre Meinung ist uns sehr wichtig und wir würden uns freuen, wenn Sie uns dabei helfen könnten,
                    unsere Forschungsergebnisse zu verbessern. Für diese Umfrage ist ${it ///owmer
                }zuständig.
                    Bei Fragen oder Anmerkungen können Sie sich gerne an ihn wenden.
                    Bitte beachten Sie, dass das Enddatum für die Teilnahme an dieser Umfrage der ${it.deadline.toString()} ist.
                    Wir würden uns freuen, wenn Sie sich die Zeit nehmen könnten, um diese Umfrage auszufüllen.
                    Vielen Dank im Voraus für Ihre Unterstützung.
                    
                    Mit freundlichen Grüßen,${it  ///owmer
                }
                     """.trimMargin()
            }
        }

        // check if state ist open or closed
        val list = ArrayList<MailAttachment>()

        val ihkExporter = ExcelExport()
        val exel = ihkExporter
            .getExcel()



        val attachment = object : MailAttachment {
            override fun getFilename(): String {
                return "test"+ "_" + LocalDateTime.now().year + ".xlsx"
            }

            override fun getContent(): ByteArray? {
                return exel
            }
        }
        list.add(attachment)

        if(mail.isNotEmpty()){
            pollMailService.sendMail("Die Umfrage ","Sehr geehrter Teilnehmer,wir laden Sie herzlich dazu ein, an unserer Umfrage zum Thema [Titel der Umfrage] teilzunehmen. Ihre Meinung ist uns sehr wichtig und wir würden uns freuen, wenn Sie uns dabei helfen könnten, unsere Forschungsergebnisse zu verbessern.Für diese Umfrage ist [Name des Ansprechpartners] zuständig. Bei Fragen oder Anmerkungen können Sie sich gerne an ihn wenden.Bitte beachten Sie, dass das Enddatum für die Teilnahme an dieser Umfrage der [Enddatum] ist. Wir würden uns freuen, wenn Sie sich die Zeit nehmen könnten, um diese Umfrage auszufüllen.Vielen Dank im Voraus für Ihre Unterstützung.Mit freundlichen Grüßen,[Name des Absenders]",list)
        }
    }




    @Scheduled(fixedRate = 50000) //cron = "0 0 1 * * *" // 1am
    fun cronDeletePolls() {
        // check if poll end in Future
        val polls = pollDao.internalLoadAll()
        val pollsMoreThanOneYearPast = polls.filter { it.created?.before(Date.from(LocalDate.now().minusYears(1).atStartOfDay(
            ZoneId.systemDefault()
        ).toInstant())) ?: false }
        pollsMoreThanOneYearPast.forEach {
            pollDao.delete(it)
        }
    }



    @PostMapping("Export")
    fun export(request: HttpServletRequest) : ResponseEntity<Resource>? {
        val ihkExporter = ExcelExport()
        val bytes: ByteArray? = ihkExporter
            .getExcel()
        val filename = ("test.xlsx")

        if (bytes == null || bytes.size == 0) {
            log.error("Oups, xlsx has zero s <ize. Filename: $filename")
            return null;
        }
        return RestUtils.downloadFile(filename, bytes)
    }
    // PostMapping add
    @PostMapping("/add")
    fun abc(){

    }


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