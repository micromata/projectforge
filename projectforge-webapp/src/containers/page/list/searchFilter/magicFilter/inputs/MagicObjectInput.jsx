import PropTypes from 'prop-types';
import React from 'react';
import AutoCompletion from '../../../../../../components/design/input/AutoCompletion';

function MagicObjectInput(
    {
        autoCompletion,
        onChange,
        translations,
        value,
    },
) {
    return (
        <div
            style={{
                width: 350,
                minHeight: 200,
            }}
        >
            <AutoCompletion
                {...autoCompletion}
                label={translations['select.placeholder'] || ''}
                // Wrap the onChange because it only accepts one argument
                onChange={newValue => onChange(newValue)}
                value={{
                    label: (value && value.label) || '',
                    value: (value && value.value) || '',
                }}
            />
        </div>
    );
}

MagicObjectInput.propTypes = {
    autoCompletion: PropTypes.shape({
        url: PropTypes.string.isRequired,
    }).isRequired,
    onChange: PropTypes.func.isRequired,
    translations: PropTypes.shape({
        'select.placeholder': PropTypes.string,
    }).isRequired,
    value: PropTypes.shape({
        label: PropTypes.string,
        value: PropTypes.oneOfType([
            PropTypes.string,
            PropTypes.number,
        ]),
    }),
};

MagicObjectInput.defaultProps = {
    value: {},
};

MagicObjectInput.isEmpty = () => false;

MagicObjectInput.getLabel = (label, { label: valueLabel }) => `${label}: ${valueLabel}`;

export default MagicObjectInput;
