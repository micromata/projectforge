import PropTypes from 'prop-types';
import React from 'react';
import ObjectSelect from '../../../../../../components/design/input/autoCompletion/ObjectSelect';

function MagicObjectInput(
    {
        autoCompletion,
        onChange,
        translations,
        value,
        ...props
    },
) {
    console.log(props);

    return (
        <ObjectSelect
            onSelect={onChange}
            translations={translations}
            value={value}
            {...props}
            type={autoCompletion.type}
            url={autoCompletion.url}
        />
    );
    /*
    return (
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
    );
     */
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
