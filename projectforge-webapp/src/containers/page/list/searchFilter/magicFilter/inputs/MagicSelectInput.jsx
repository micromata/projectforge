import PropTypes from 'prop-types';
import React from 'react';
import { CheckBox } from '../../../../../../components/design';
import RadioButton from '../../../../../../components/design/input/RadioButton';

function MagicSelectInput(
    {
        multi,
        onChange,
        value,
        values,
    },
) {
    const Tag = multi ? CheckBox : RadioButton;

    return (
        <React.Fragment>
            {values.map(({ id: selectValue, displayName }) => (
                <Tag
                    key={`magic-select-${selectValue}`}
                    id={`magic-select-${selectValue}`}
                    label={displayName}
                    onChange={() => {
                        if (multi) {
                            const oldValues = value.values || [];
                            if (oldValues.includes(selectValue)) {
                                onChange({ values: oldValues.filter(v => v !== selectValue) });
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
    multi: PropTypes.bool,
};

MagicSelectInput.defaultProps = {
    multi: true,
};

MagicSelectInput.isEmpty = ({ values }) => !values || values.length === 0;

MagicSelectInput.getLabel = (label, { values }, { values: dataValues }) => `${label}: ${values
    // Find Labels for selected items by values
    .map(v => dataValues.find(dv => dv.id === v).displayName)
    .join(', ')}`;

export default MagicSelectInput;
