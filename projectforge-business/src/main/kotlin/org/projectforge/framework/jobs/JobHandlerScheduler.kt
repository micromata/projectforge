package org.projectforge.framework.jobs

import org.projectforge.Constants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class JobHandlerScheduler {
  @Autowired
  private lateinit var jobHandler: JobHandler

  // Runs every minute
  @Scheduled(fixedDelay = Constants.MILLIS_PER_MINUTE, initialDelay = Constants.MILLIS_PER_MINUTE)
  fun execute() {
    jobHandler.tidyUp()
  }
}
