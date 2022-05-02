package org.projectforge.rest.admin

import org.projectforge.common.logging.LogSubscription
import org.projectforge.rest.config.Rest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Log viewer to view and search last 10.000 log events for all users, to browse
 * own [LogSubscription] queues.
 */
@RestController
@RequestMapping("${Rest.URL}/adminLogViewer")
open class AdminLogViewerPageRest : LogViewerPageRest() {
  init {
    adminLogViewer = true
  }
}
