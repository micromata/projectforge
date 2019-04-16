package projectforge.business.teamcal.event.model;

import org.junit.jupiter.api.Test;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by blumenstein on 19.12.16.
 */
public class TeamEventAttendeeDOTest
{
  @Test
  public void testEqualsHashCode()
  {
    AddressDO exampleAddress = new AddressDO();
    exampleAddress.setPk(4712);

    TeamEventAttendeeDO first = new TeamEventAttendeeDO();
    TeamEventAttendeeDO second = new TeamEventAttendeeDO();
    assertTrue(first.equals(second));
    assertTrue(first.hashCode() == second.hashCode());

    first.setUrl("test@test.de");
    assertFalse(first.equals(second));
    assertFalse(first.hashCode() == second.hashCode());
    second.setUrl("test@test.de");
    assertTrue(first.equals(second));
    assertTrue(first.hashCode() == second.hashCode());

    first.setPk(4711);
    assertTrue(first.equals(second));
    assertTrue(first.hashCode() == second.hashCode());
    second.setPk(4711);
    assertTrue(first.equals(second));
    assertTrue(first.hashCode() == second.hashCode());

    first.setUrl(null);
    assertTrue(first.equals(second));
    //Equals/HashCode contract is broken
    assertFalse(first.hashCode() == second.hashCode());

    first.setPk(null);
    first.setAddress(exampleAddress);
    assertFalse(first.equals(second));
    assertFalse(first.hashCode() == second.hashCode());

    second.setUrl(null);
    second.setAddress(exampleAddress);
    assertTrue(first.equals(second));
    assertTrue(first.hashCode() == second.hashCode());

  }

  @Test
  public void testReomoveFromSet()
  {
    Set<TeamEventAttendeeDO> testSet = new HashSet<>();
    TeamEventAttendeeDO first = new TeamEventAttendeeDO();
    testSet.add(first);
    TeamEventAttendeeDO second = new TeamEventAttendeeDO();
    testSet.remove(second);
    assertTrue(testSet.isEmpty());
    first.setPk(4711);
    testSet.add(first);
    second.setPk(4711);
    testSet.remove(second);
    assertTrue(testSet.isEmpty());
    first.setPk(null);
    first.setUrl("test@test.de");
    testSet.add(first);
    second.setUrl("test@test.de");
    testSet.remove(second);
    assertTrue(testSet.isEmpty());
  }

}
