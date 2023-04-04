package org.projectforge.rest.poll

import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.time.LocalDate
import javax.persistence.*


@Entity
@Indexed
@Table(name = "t_poll")
@AUserRightId(value = "poll", checkAccess = false)
open class PollDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "poll.title")
    @get:Column(name = "title", nullable = false)
    open var title: String? = null

    @PropertyInfo(i18nKey = "poll.description")
    @get:Column(name = "description", nullable = false)
    open var description: String? = null

    @PropertyInfo(i18nKey = "poll.owner")
    @IndexedEmbedded(depth = 1)
    @ManyToOne
    @JoinColumn(name = "owner_pk")
    open var owner: EmployeeDO? = null

    @PropertyInfo(i18nKey = "poll.location")
    @get:Column(name = "location", nullable = false)
    open var location: String? = null

    @PropertyInfo(i18nKey = "poll.date")
    @get:Column(name = "date", nullable = false)
    open var date: LocalDate? = null

    @PropertyInfo(i18nKey = "poll.deadline")
    @get:Column(name = "end_date", nullable = false)
    open var deadline: LocalDate? = null

    @PropertyInfo(i18nKey = "poll.canSeeResultUsers")
    @IndexedEmbedded(depth = 1)
    @get:ManyToMany(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "canSeeResultUsers", nullable = false)
    open var canSeeResultUsers: List<EmployeeDO>? = null

    @PropertyInfo(i18nKey = "poll.canEditPollUsers")
    @IndexedEmbedded(depth = 1)
    @get:ManyToMany(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "canEditPollUsers", nullable = false)
    open var canEditPollUsers: List<EmployeeDO>? = null

    @PropertyInfo(i18nKey = "poll.canVoteInPoll")
    @IndexedEmbedded(depth = 1)
    @get:ManyToMany(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "canVoteInPoll", nullable = false)
    open var canVoteInPoll: List<EmployeeDO>? = null

}