package org.projectforge.business.poll

import org.hibernate.search.annotations.Indexed
import org.projectforge.business.common.BaseUserGroupRightsDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.context.annotation.DependsOn
import java.time.LocalDate
import javax.persistence.*


@Entity
@Indexed
@Table(name = "t_poll")
@AUserRightId(value = "poll", checkAccess = false)
@DependsOn("org.projectforge.framework.persistence.user.entities.PFUserDO")
open class PollDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "poll.title")
    @get:Column(name = "title", nullable = false, length = 1000)
    open var title: String? = null

    @PropertyInfo(i18nKey = "poll.description")
    @get:Column(name = "description", length = 10000)
    open var description: String? = null

    @get:PropertyInfo(i18nKey = "poll.owner", additionalI18nKey = "poll.owner.explaination")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_pk", nullable = false)
    open var owner: PFUserDO? = null

    @PropertyInfo(i18nKey = "poll.location")
    @get:Column(name = "location")
    open var location: String? = null

    @PropertyInfo(i18nKey = "poll.deadline")
    @get:Column(name = "deadline", nullable = false)
    open var deadline: LocalDate? = null

    @PropertyInfo(i18nKey = "poll.date")
    @get:Column(name = "date")
    open var date: LocalDate? = null

    @PropertyInfo(i18nKey = "poll.attendees")
    @get:Column(name = "attendeesIds", nullable = true)
    open var attendeesIds: String? = null

    @PropertyInfo(i18nKey = "poll.group_attendees")
    @get:Column(name = "groupAttendeesIds", nullable = true)
    open var groupAttendeesIds: String? = null

    @get:Column(name = "full_access_group_ids", length = 4000, nullable = true)
    open var fullAccessGroupIds: String? = null

    @get:Column(name = "full_access_user_ids", length = 4000, nullable = true)
    open var fullAccessUserIds: String? = null

    @PropertyInfo(i18nKey = "poll.inputFields")
    @get:Column(name = "inputFields", nullable = true, length = 1000)
    open var inputFields: String? = null

    @PropertyInfo(i18nKey = "poll.state")
    @get:Column(name = "state", nullable = false)
    open var state: State? = State.RUNNING

    enum class State {
        RUNNING, FINISHED
    }
}