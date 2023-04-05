package org.projectforge.business.poll

import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
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
    @get:Column(name = "title", nullable = true, length = 1000)
    open var title: String? = null

    @PropertyInfo(i18nKey = "poll.description")
    @get:Column(name = "description", nullable = true, length = 10000)
    open var description: String? = null

  /*  @PropertyInfo(i18nKey = "poll.owner")
    @IndexedEmbedded(depth = 1)
    @ManyToOne
    @JoinColumn(name = "owner_pk")
    open var owner: PFUserDO? = null*/

    @PropertyInfo(i18nKey = "poll.location")
    @get:Column(name = "location", nullable = true)
    open var location: String? = null

/*    @PropertyInfo(i18nKey = "poll.date")
    @get:Column(name = "date", nullable = true)
    open var date: LocalDate? = null

    @PropertyInfo(i18nKey = "poll.deadline")
    @get:Column(name = "deadline", nullable = true)
    open var deadline: LocalDate? = null

    @PropertyInfo(i18nKey = "poll.inputlist")
    @get:Column(name = "input_list", nullable = true, length = 10000)
    open var inputFields: String? = null*/

   /* @PropertyInfo(i18nKey = "poll.canSeeResultUsers")
    @get:Column(name = "canSeeResultUsers", nullable = true)
    open var canSeeResultUsers: String? = null

    @PropertyInfo(i18nKey = "poll.canEditPollUsers")
    @get:Column(name = "canEditPollUsers", nullable = true)
    open var canEditPollUsers: String? = null

    @PropertyInfo(i18nKey = "poll.canVoteInPoll")
    @get:Column(name = "canVoteInPoll", nullable = true)
    open var canVoteInPoll: String? = null*/

}