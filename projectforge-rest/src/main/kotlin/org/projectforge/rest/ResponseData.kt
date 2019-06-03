package org.projectforge.rest

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.ui.UIStyle

/**
 * Contains response information for the client, such as the result info an information for displaying e. g. in a
 * client's toast.
 */
class ResponseData(
        val i18nKey: String? = null,
        /**
         * The message to display for the user.
         */
        var message: String? = null,
        /** The (technical) message. */
        var technicalMessage: String? = null,
        var messageType: MessageType = MessageType.TEXT,
        var style: UIStyle? = null,
        vararg messageParams: String) {
    init {
        if (message == null && i18nKey != null) {
            if (messageParams.isNotEmpty())
                message = translateMsg(i18nKey, messageParams)
            else
                message = translate(i18nKey)
        }
    }
}

enum class MessageType { TEXT, TOAST }
