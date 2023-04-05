package org.projectforge.rest.poll

import org.checkerframework.checker.guieffect.qual.UIType
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.scripting.I18n
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.business.vacation.service.ConflictingVacationsCache
import org.projectforge.framework.persistence.api.BaseDao
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
import org.springframework.http.ResponseEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import javax.servlet.http.HttpServletRequest

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

        val layout = super.createEditLayout(dto, userAccess).add(UILabel("poll.create"))

        layout.addAction(
            UIButton.createDefaultButton(
                id = "download_button",
                title = I18n.getString("download"),
                responseAction = ResponseAction(
                    RestResolver.getRestUrl(
                        this::class.java,
                        "Export"
                    ), targetType = TargetType.POST
                )))
    /*   // dto.inputFields?.forEachIndexed { field, index ->
            if (field.type == UIDataType.BOOLEAN) {
 // layout.add() // ID: type[]
                // "type[$index]"
                // Id: name
            }
        }*/
        /*layout.add(UIRow().add(UIFieldset(UILength(md = 6, lg = 4))
            .add(lc, "name")))
        */

        layout.add(
            UIRow()
                .add(
                    UIFieldset(UILength(md = 6, lg = 4))
                        .add(lc, "title", "description", "location", "owner", "deadline")
                ))

        layout.watchFields.addAll(
            arrayOf(
                "title",
                "description",
                "location",
                "owner",
                "deadline"
            )
        )
        // layout.addAction() // Button mit Rest-Endpunkt add
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    @Scheduled(fixedRate = 5000)
    fun cronJobSch() {
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

           val list = mutableListOf<MailAttachment>()
           list.add(attachment)
       pollMailService.sendMail("test","user",list)
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
}