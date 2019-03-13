package org.projectforge.sms

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration


@Configuration
open class SmsSenderConfig {
    enum class HttpMethodType {
        POST, GET
    }

    @Value("\${projectforge.sms.httpMethod}")
    open var httpMethodType: HttpMethodType? = null

    @Value("\${projectforge.sms.url}")
    open var url: String? = null

    @Value("#{\${projectforge.sms.httpParameters}}")
    open var httpParams: Map<String, String>? = null

    @Value("\${projectforge.sms.returnCodePattern.success}")
    open var smsReturnPatternSuccess: String? = null

    @Value("\${projectforge.sms.returnCodePattern.numberError}")
    open var smsReturnPatternNumberError: String? = null

    @Value("\${projectforge.sms.returnCodePattern.messageToLargeError}")
    open var smsReturnPatternMessageToLargeError: String? = null

    @Value("\${projectforge.sms.returnCodePattern.messageError}")
    open var smsReturnPatternMessageError: String? = null

    @Value("\${projectforge.sms.returnCodePattern.error}")
    open var smsReturnPatternError: String? = null

    @Value("\${projectforge.sms.smsMaxMessageLength}")
    open var smsMaxMessageLength = 160

    fun isSmsConfigured(): Boolean {
        return StringUtils.isNotBlank(url)
    }

    fun setHttpMethodType(httpMethodType: String): SmsSenderConfig {
        this.httpMethodType = if (StringUtils.equalsIgnoreCase("get", httpMethodType)) HttpMethodType.GET else HttpMethodType.POST
        return this
    }
}