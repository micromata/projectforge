package org.projectforge.business.jobs;

import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExternalCalendarSubscriptionJob
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(ExternalCalendarSubscriptionJob.class);

  @Autowired
  private TeamEventExternalSubscriptionCache teamEventExternalSubscriptionCache;

  //@Scheduled(cron = "0 */15 * * * *")
  @Scheduled(cron = "${projectforge.cron.externalCalendar}")
  public void execute()
  {
    log.info("External calendar subscriptions job started.");
    try {
      teamEventExternalSubscriptionCache.updateCache();
    } catch (final Throwable ex) {
      log.error("Exception while executing ExternalCalendarSubscriptionJob: " + ex.getMessage(), ex);
    }
    log.info("External calendar subscriptions job finished.");
  }

}
