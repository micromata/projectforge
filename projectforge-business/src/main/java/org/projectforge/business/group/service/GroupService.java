package org.projectforge.business.group.service;

import java.util.Collection;
import java.util.List;

import org.projectforge.framework.persistence.user.entities.GroupDO;

public interface GroupService
{

  Collection<GroupDO> getSortedGroups();

  String getGroupIds(Collection<GroupDO> groups);

  Collection<GroupDO> getSortedGroups(String groupIds);

  List<String> getGroupNames(String groupIds);

  List<GroupDO> getAllGroups();

  GroupDO getGroup(Integer groupId);

  String getGroupname(Integer groupId);

  String getGroupnames(Integer userId);

}
