package org.projectforge.plugins.skillmatrix;

import java.io.Serializable;

import de.micromata.genome.jpa.impl.AbstractParentChildTableTruncater;

/**
 * Skill has reference to itself, so order of delete has to be childs first.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class SkillTableTruncater extends AbstractParentChildTableTruncater<SkillDO>
{

  @Override
  protected Serializable getPk(SkillDO ent)
  {
    return ent.getPk();
  }

  @Override
  protected Serializable getParentPk(SkillDO ent)
  {
    return ent.getParentId();
  }

}
