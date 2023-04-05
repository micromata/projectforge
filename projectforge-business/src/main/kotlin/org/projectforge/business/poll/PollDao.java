package org.projectforge.business.poll;

import org.projectforge.framework.access.*;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.user.entities.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

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
