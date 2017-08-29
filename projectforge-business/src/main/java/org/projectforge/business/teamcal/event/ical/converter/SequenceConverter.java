package org.projectforge.business.teamcal.event.ical.converter;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Sequence;

public class SequenceConverter extends PropertyConverter
{
  @Override
  public Property toVEvent(final TeamEventDO event)
  {
    return new Sequence(event.getSequence() != null ? event.getSequence() : 0);
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    final Sequence sequence = vEvent.getSequence();
    event.setSequence(sequence != null ? sequence.getSequenceNo() : 0);

    return false;
  }
}
