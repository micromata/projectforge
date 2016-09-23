package org.projectforge.business.jobs;

import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExternalCalendarSubscriptionJob
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(ExternalCalendarSubscriptionJob.class);

  @Autowired
  private TeamEventExternalSubscriptionCache teamEventExternalSubscriptionCache;

  @Scheduled(cron = "0 */15 * * * *")
  public void doSomething()
  {
    log.info("Update external calendar subscriptions");
    try {
      teamEventExternalSubscriptionCache.updateCache();
    } catch (final Throwable ex) {
      log.error("Exception while executing ExternalCalendarSubscriptionJob: " + ex.getMessage(), ex);
    }
  }

}
