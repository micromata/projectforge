package org.projectforge.business.poll;

import org.projectforge.framework.access.*;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.user.entities.*;
import org.projectforge.framework.time.PFDateTime;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;
import java.util.Locale;

@Repository
public class PollDao extends BaseDao<PollDO> {

    public PollDao() {
        super(PollDO.class);
    }

    @Override
    public boolean hasAccess(PFUserDO user, PollDO obj, PollDO oldObj, OperationType operationType, boolean throwException) {
        return true;
    }

    @Override
    public PollDO newInstance() {
        return new PollDO();
    }


}
