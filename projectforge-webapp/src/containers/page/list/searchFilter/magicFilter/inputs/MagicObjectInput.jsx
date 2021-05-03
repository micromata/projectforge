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
}

MagicObjectInput.propTypes = {
    autoCompletion: PropTypes.shape({
        url: PropTypes.string.isRequired,
        type: PropTypes.string,
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

MagicObjectInput.isEmpty = ({ id }) => id === undefined;

MagicObjectInput.getLabel = (label, { displayName }) => `${label}: ${displayName}`;

export default MagicObjectInput;
