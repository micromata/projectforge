package org.projectforge.framework.i18n

fun translate(i18nKey: String?): String {
    if (i18nKey == null) return "???"
    return I18nHelper.getLocalizedMessage(i18nKey)
}

fun translateMsg(ex: UserException): String {
    if (ex.msgParams.isNullOrEmpty()) {
        return translate(ex.i18nKey)
    }
    return I18nHelper.getLocalizedMessage(ex.i18nKey, ex.msgParams)
}

fun translateMsg(i18nKey: String, vararg params: Any): String {
    return I18nHelper.getLocalizedMessage(i18nKey, *params)
}

/**
 * If the given string starts with ', the title without ' is returned, otherwise [translate] will be called.
 */
fun autoTranslate(text: String?): String {
    if (text == null) return "???"
    if (text.startsWith("'") == true)
        return text.substring(1)
    return translate(text)
}

fun addTranslations(vararg i18nKeys: String, translations: MutableMap<String, String> = mutableMapOf()): MutableMap<String, String> {
    i18nKeys.forEach {
        translations.put(it, translate(it))
    }
    return translations
}
