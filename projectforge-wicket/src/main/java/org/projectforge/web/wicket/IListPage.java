package org.projectforge.web.wicket;

import java.util.List;

import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IdObject;

public interface IListPage<O extends IdObject<?>, D extends IDao<?>>
{
  List<O> getList();

  D getBaseDao();
}
