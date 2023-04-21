package org.projectforge.business.poll

import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.context.annotation.DependsOn
import javax.persistence.*

@Entity
@Indexed
@Table(name = "t_poll_response")
@AUserRightId(value = "poll.response", checkAccess = false)
@DependsOn("org.projectforge.framework.persistence.user.entities.PFUserDO")
open class PollResponseDO : DefaultBaseDO() {
    @get:PropertyInfo(i18nKey = "poll.response.poll")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "poll_fk", nullable = false)
    open var poll: PollDO? = null

    @get:PropertyInfo(i18nKey = "poll.response.owner")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_fk", nullable = false)
    open var owner: PFUserDO? = null

    @PropertyInfo(i18nKey = "poll.responses")
    @get:Column(name = "responses", nullable = true, length = 1000)
    open var responses: String? = null
}