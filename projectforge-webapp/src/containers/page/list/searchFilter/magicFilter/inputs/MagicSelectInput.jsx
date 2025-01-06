import PropTypes from 'prop-types';
import React from 'react';
import { CheckBox } from '../../../../../../components/design';
import RadioButton from '../../../../../../components/design/input/RadioButton';

function MagicSelectInput(
    {
        id,
        multi = true,
        onChange,
        value,
        values,
    },
) {
    const Tag = multi ? CheckBox : RadioButton;

    return (
        <>
            {values.map(({ id: selectValue, displayName }) => (
                <Tag
                    key={`magic-select-${selectValue}`}
                    id={`magic-select-${selectValue}`}
                    label={displayName}
                    name={id}
                    onChange={() => {
                        if (multi) {
                            const oldValues = value.values || [];
                            if (oldValues.includes(selectValue)) {
                                onChange({ values: oldValues.filter((v) => v !== selectValue) });
                            } else {
                                onChange({ values: [...oldValues, selectValue] });
                            }
                        } else {
                            onChange({ values: [selectValue] });
                        }
                    }}
                    checked={Boolean(value.values && value.values.includes(selectValue))}
                />
            ))}
        </>
    );
}

MagicSelectInput.propTypes = {
    onChange: PropTypes.func.isRequired,
    value: PropTypes.shape({
        // eslint-disable-next-line react/forbid-prop-types
        values: PropTypes.any,
    }).isRequired,
    values: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    id: PropTypes.string,
    multi: PropTypes.bool,
};

MagicSelectInput.isEmpty = ({ values }) => !values || values.length === 0;

MagicSelectInput.getLabel = (label, { values }, { values: dataValues }) => `${label}: ${values
    // Find Labels for selected items by values
    ?.map((v) => dataValues?.find((dv) => dv.id === v)?.displayName)
    ?.join(', ')}`;

export default MagicSelectInput;
