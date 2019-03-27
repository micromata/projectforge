package org.projectforge.framework.i18n

fun translate(i18nKey: String?): String {
    if (i18nKey == null) return "???"
    return I18nHelper.getLocalizedMessage(i18nKey)
}

fun translateMsg(i18nKey: String, vararg params: Any): String {
    return I18nHelper.getLocalizedMessage(i18nKey, *params)
}
