package org.projectforge.rest.dto

import org.projectforge.ui.UILayout

/**
 * Contains the layout data returned for the frontend regarding form pages.
 * @param variables Additional variables / data provided for the form page.
 */
class FormLayoutData(val data: Any?,
                     val ui: UILayout?,
                     var serverData: ServerData?,
                     var variables: Map<String, Any>? = null)
