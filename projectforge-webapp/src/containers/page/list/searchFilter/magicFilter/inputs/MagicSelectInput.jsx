import PropTypes from 'prop-types';
import React from 'react';
import { Button, ButtonGroup } from '../../../../../../components/design';

function MagicSelectInput(
    {
        onChange,
        value,
        values,
    },
) {
    return (
        <ButtonGroup>
            {values.map(({ value: selectValue, label }) => (
                <Button
                    key={`magic-select-${selectValue}`}
                    onClick={() => {
                        const oldValues = value.values || [];
                        if (oldValues.includes(selectValue)) {
                            onChange({ values: oldValues.filter(v => v !== selectValue) });
                        } else {
                            onChange({ values: [...oldValues, selectValue] });
                        }
                    }}
                    active={value.values && value.values.includes(selectValue)}
                >
                    {label}
                </Button>
            ))}
        </ButtonGroup>
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
