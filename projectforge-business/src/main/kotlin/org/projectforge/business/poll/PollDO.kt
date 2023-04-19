package org.projectforge.business.poll

import org.hibernate.search.annotations.Indexed
import org.projectforge.business.common.BaseUserGroupRightsDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.context.annotation.DependsOn
import java.time.LocalDate
import javax.persistence.*


@Entity
@Indexed
@Table(name = "t_poll")
@AUserRightId(value = "poll", checkAccess = false)
@DependsOn("org.projectforge.framework.persistence.user.entities.PFUserDO")
open class PollDO : BaseUserGroupRightsDO() {

    @PropertyInfo(i18nKey = "poll.title")
    @get:Column(name = "title", nullable = true, length = 1000)
    open var title: String? = null

    @PropertyInfo(i18nKey = "poll.description")
    @get:Column(name = "description", nullable = true, length = 10000)
    open var description: String? = null

    @get:PropertyInfo(i18nKey = "poll.owner", additionalI18nKey = "poll.owner.explaination")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_pk")
    override var owner: PFUserDO? = null

    @PropertyInfo(i18nKey = "poll.location")
    @get:Column(name = "location", nullable = true)
    open var location: String? = null

    @PropertyInfo(i18nKey = "poll.deadline")
    @get:Column(name = "deadline", nullable = true)
    open var deadline: LocalDate? = null

    @PropertyInfo(i18nKey = "poll.date")
    @get:Column(name = "date", nullable = true)
    open var date: LocalDate? = null

    @PropertyInfo(i18nKey = "poll.attendees")
    @get:Column(name = "attendeesIds", nullable = true)
    open var attendeesIds: String? = null

    @PropertyInfo(i18nKey = "poll.group_attendees")
    @get:Column(name = "groupAttendeesIds", nullable = true)
    open var groupAttendeesIds: String? = null
}