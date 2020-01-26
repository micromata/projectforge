import PropTypes from 'prop-types';
import React from 'react';
import { CheckBox } from '../../../../../../components/design';
import MagicObjectInput from './MagicObjectInput';

function MagicCheckboxInput(
    {
        id,
        onChange,
        value,
    },
) {
    return (
        <CheckBox
            id={`magic-checkbox-${id}`}
            onChange={({ target }) => onChange({ value: target.checked })}
            checked={value.value === true}
        />
    );
}

MagicCheckboxInput.propTypes = {
    id: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.shape({}).isRequired,
};

MagicCheckboxInput.defaultProps = {};

MagicCheckboxInput.isEmpty = () => false;

MagicCheckboxInput.getLabel = (label, { value }) => `${label}: ${value}`;

export default MagicCheckboxInput;
