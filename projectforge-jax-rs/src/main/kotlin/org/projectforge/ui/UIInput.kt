package org.projectforge.ui

import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.rest.core.AbstractBaseRest
import org.springframework.util.ClassUtils

data class UIInput(val id: String,
                   @Transient
                   override val layoutContext: LayoutContext? = null,
                   var maxLength: Int? = null,
                   var required: Boolean? = null,
                   var focus: Boolean? = null,
                   var dataType: UIDataType = UIDataType.STRING,
                   override var label: String? = null,
                   override var additionalLabel: String? = null,
                   override var tooltip: String? = null)
    : UIElement(UIElementType.INPUT), UILabelledElement {
    var autoCompletionUrl: String? = null

    /**
     * Please note: Only enabled properties in [BaseDao] are available due to security reasons.
     * @return this for chaining.
     * @see BaseDao.isAutocompletionPropertyEnabled
     */
    fun enableAutoCompletion(services: AbstractBaseRest<*, *, *, *>):UIInput {
        if (!services.isAutocompletionPropertyEnabled(id)) {
            throw InternalErrorException("Development error: You must enable autocompletion properties explicit in '${ClassUtils.getUserClass(services.baseDao).simpleName}.isAutocompletionPropertyEnabled(String)' for property '$id' for security resasons first.")
        }
        autoCompletionUrl = "${services.getRestPath()}/ac?property=${id}&search="
        return this
    }
}
