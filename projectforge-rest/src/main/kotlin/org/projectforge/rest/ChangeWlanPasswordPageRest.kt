package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.Const
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/changeWlanPassword")
class ChangeWlanPasswordPageRest : AbstractDynamicPageRest() {

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private val userService: UserService? = null

    class WlanPasswordData(
            var userId: Int? = null,
            var loginPassword: String? = null,
            var newWlanPassword: String? = null,
            var wlanPasswordRepeat: String? = null
    )

    @PostMapping
    fun save(request: HttpServletRequest, @RequestBody postData: PostData<WlanPasswordData>)
            : ResponseEntity<ResponseAction>? {
        validateCsrfToken(request, postData)?.let { return it }
        val data = postData.data
        check(ThreadLocalUserContext.getUserId() == data.userId) { "Oups, ChangeWlanPasswordPage is called with another than the logged in user!" }

        check(data.newWlanPassword == data.wlanPasswordRepeat) { val validationErrors = mutableListOf<ValidationError>()
            validationErrors.add(ValidationError.create("user.error.passwordAndRepeatDoesNotMatch"))
            return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
        }

        log.info { "User wants to change his WLAN password." }

        val errorMsgKeys = userService!!.changeWlanPassword(userDao.getById(data.userId), data.loginPassword, data.newWlanPassword)

        check(errorMsgKeys.isEmpty()) {
            val validationErrors = mutableListOf<ValidationError>()
            for (errorMsgKey in errorMsgKeys){
                validationErrors.add(ValidationError.create(errorMsgKey.key))
            }
            return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
        }

        return ResponseEntity(ResponseAction("/${Const.REACT_APP_PATH}calendar", message = ResponseAction.Message("user.changePassword.msg.passwordSuccessfullyChanged")), HttpStatus.OK)
    }

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val userId = ThreadLocalUserContext.getUserId()
        val data = WlanPasswordData(userId)

        val layout = UILayout("user.changeWlanPassword.title")
        val oldPassword = UIInput("loginPassword",
                label = "user.changeWlanPassword.loginPassword",
                required = true,
                focus = true,
                dataType = UIDataType.PASSWORD,
                autoComplete = UIInput.AutoCompleteType.CURRENT_PASSWORD)
        val newPassword = UIInput("newWlanPassword",
                label = "user.changeWlanPassword.newWlanPassword",
                dataType = UIDataType.PASSWORD,
                required = true)
        val passwordRepeat = UIInput("wlanPasswordRepeat",
                label = "passwordRepeat",
                dataType = UIDataType.PASSWORD,
                required = true)

        layout.add(UIRow()
                .add(UICol()
                        .add(oldPassword)
                        .add(newPassword)
                        .add(passwordRepeat)
                        .add(UIButton("update",
                                translate("update"),
                                UIColor.SUCCESS,
                                responseAction = ResponseAction(RestResolver.getRestUrl(this::class.java), targetType = TargetType.POST),
                                default = true)
                        )))

        LayoutUtils.process(layout)

        layout.postProcessPageMenu()

        return FormLayoutData(data, layout, createServerData(request))
    }
}