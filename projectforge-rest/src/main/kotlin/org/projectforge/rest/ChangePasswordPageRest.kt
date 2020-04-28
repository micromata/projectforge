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
import java.text.MessageFormat
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/changePassword")
class ChangePasswordPageRest : AbstractDynamicPageRest() {

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private val userService: UserService? = null

    class PasswordData(
            var userId: Int? = null,
            var oldPassword: String? = null,
            var newPassword: String? = null,
            var passwordRepeat: String? = null
    )

    @PostMapping
    fun save(request: HttpServletRequest, @RequestBody postData: PostData<PasswordData>)
            : ResponseEntity<ResponseAction>? {
        validateCsrfToken(request, postData)?.let { return it }
        val data = postData.data
        check(ThreadLocalUserContext.getUserId() == data.userId) { "Oups, ChangePasswordPage is called with another than the logged in user!" }

        check(data.newPassword == data.passwordRepeat) {
            val validationErrors = mutableListOf<ValidationError>()
            validationErrors.add(ValidationError.create("user.error.passwordAndRepeatDoesNotMatch"))
            return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
        }

        log.info { "User wants to change his password." }

        val errorMsgKeys = userService!!.changePassword(userDao.getById(data.userId), data.oldPassword, data.newPassword)

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
        val data = PasswordData(userId)

        val layout = UILayout("user.changePassword.title")
        val oldPassword = UIInput("oldPassword",
                label = "user.changePassword.oldPassword",
                required = true,
                focus = true,
                dataType = UIDataType.PASSWORD,
                autoComplete = UIInput.AutoCompleteType.CURRENT_PASSWORD)
        val newPassword = UIInput("newPassword",
                label = "user.changePassword.newPassword",
                dataType = UIDataType.PASSWORD,
                required = true)
        val passwordRepeat = UIInput("passwordRepeat",
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