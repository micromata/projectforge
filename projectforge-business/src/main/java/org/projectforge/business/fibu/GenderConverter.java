package org.projectforge.business.fibu;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, Integer>
{
  private final Gender DEFAULT_GENDER = Gender.NOT_KNOWN;

  @Override
  public Integer convertToDatabaseColumn(Gender gender)
  {
    if (gender == null) {
      gender = DEFAULT_GENDER;
    }
    return gender.getIsoCode();
  }

  @Override
  public Gender convertToEntityAttribute(Integer isoCode)
  {
    // it may be null on an empty database/column
    if (isoCode == null) {
      return DEFAULT_GENDER;
    }

    int intIsoCode = isoCode;
    for (Gender gender : Gender.values()) {
      if (gender.getIsoCode() == intIsoCode) {
        return gender;
      }
    }

    return DEFAULT_GENDER;
  }
}
