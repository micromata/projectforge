package org.projectforge.web.common;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.projectforge.common.i18n.I18nEnum;
import org.projectforge.framework.i18n.I18nHelper;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

public class I18nEnumChoiceProvider<T extends Enum<T> & I18nEnum> extends ChoiceProvider<T>
{
  private final Class<T> clazz;

  public I18nEnumChoiceProvider(final Class<T> clazz)
  {
    this.clazz = clazz;
  }

  @Override
  public String getDisplayValue(final T choice)
  {
    return I18nHelper.getLocalizedMessage(choice.getI18nKey());
  }

  /**
   * Converts the given Enum value to the Enum name.
   */
  @Override
  public String getIdValue(final T choice)
  {
    return choice.name();
  }

  @Override
  public void query(final String term, final int page, final Response<T> response)
  {
    final String termLowerCase = term.toLowerCase();
    final List<T> matchingAuftragsPositionsArten = EnumSet.allOf(clazz).stream()
        .filter(art -> I18nHelper.getLocalizedMessage(art.getI18nKey()).toLowerCase().contains(termLowerCase))
        .collect(Collectors.toList());

    response.addAll(matchingAuftragsPositionsArten);
  }

  /**
   * Converts a Collection of Enum names to a Collection of the corresponding Enum values.
   */
  @Override
  public Collection<T> toChoices(final Collection<String> ids)
  {
    return ids.stream()
        .map(t -> T.valueOf(clazz, t))
        .collect(Collectors.toList());
  }
}
