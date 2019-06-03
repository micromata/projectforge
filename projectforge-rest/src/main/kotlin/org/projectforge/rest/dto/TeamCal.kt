package org.projectforge.rest.dto

import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

class TeamCal(var title: String? = null,
              var owner: PFUserDO? = null,
              var description: String? = null,
              var externalSubscription: Boolean = false,
              var externalSubscriptionUrl: String? = null,
              var externalSubscriptionUpdateInterval: Int? = null,
              var externalSubscriptionUrlAnonymized: String? = null
) : BaseObject<TeamCalDO>()
