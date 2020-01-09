import PropTypes from 'prop-types';
import React from 'react';
import { CheckBox } from '../../../../../../components/design';

function MagicSelectInput(
    {
        onChange,
        value,
        values,
    },
) {
    return (
        <React.Fragment>
            {values.map(({ value: selectValue, label }) => (
                <CheckBox
                    key={`magic-select-${selectValue}`}
                    id={`magic-select-${selectValue}`}
                    label={label}
                    onChange={() => {
                        const oldValues = value.values || [];
                        if (oldValues.includes(selectValue)) {
                            onChange({ values: oldValues.filter(v => v !== selectValue) });
                        } else {
                            onChange({ values: [...oldValues, selectValue] });
                        }
                    }}
                    checked={Boolean(value.values && value.values.includes(selectValue))}
                />
            ))}
        </React.Fragment>
    );
}

MagicSelectInput.propTypes = {
    onChange: PropTypes.func.isRequired,
    value: PropTypes.shape({}).isRequired,
    values: PropTypes.arrayOf(PropTypes.shape({
        value: PropTypes.string,
        label: PropTypes.string,
    })).isRequired,
};

MagicSelectInput.defaultProps = {};

MagicSelectInput.getLabel = (label, { values }, { values: dataValues }) => `${label}: ${values
    // Find Labels for selected items by values
    .map(v => dataValues.find(dv => dv.value === v).label)
    .join(', ')}`;

export default MagicSelectInput;
