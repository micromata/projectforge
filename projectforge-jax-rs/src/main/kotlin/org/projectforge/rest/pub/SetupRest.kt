package org.projectforge.rest.pub

import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.GlobalConfiguration
import org.projectforge.framework.persistence.database.DatabaseService
import org.projectforge.rest.config.Rest
import org.projectforge.ui.UILabel
import org.projectforge.ui.UILayout
import org.projectforge.ui.UINamedContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * This rest service should be available without login (public).
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/setup")
open class SetupRest {
    data class LoginData(var username: String? = null, var password: String? = null, var stayLoggedIn: Boolean? = null)

    private val log = org.slf4j.LoggerFactory.getLogger(SetupRest::class.java)

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var databaseService: DatabaseService

    @GetMapping("layout")
    fun getLayout(): UILayout {
        val layout = UILayout("administration.setup.title")
        if (databaseService.databaseTablesWithEntriesExists()) {
            log.error("Data-base isn't empty: SetupPage shouldn't be used...")
            return layout
            // throw RestartResponseException(SetupPage::class.java!!)
        }
        layout
                .addTranslations("username", "password", "login.stayLoggedIn", "login.stayLoggedIn.tooltip")
        //.addTranslation("messageOfTheDay")
        layout.add(UINamedContainer("messageOfTheDay").add(UILabel(label = GlobalConfiguration.getInstance().getStringValue(ConfigurationParam.MESSAGE_OF_THE_DAY))))
        return layout
    }
}
