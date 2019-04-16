import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import InputBase from './base';
import InputPart from './base/InputPart';

function Input(
    {
        additionalLabel,
        color,
        id,
        label,
        ...props
    },
) {
    return (
        <InputBase label={label} id={id} color={color} additionalLabel={additionalLabel}>
            <InputPart id={id} {...props} />
        </InputBase>
    );
}

Input.propTypes = {
    label: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    color: colorPropType,
};

Input.defaultProps = {
    additionalLabel: undefined,
    color: undefined,
};

export default Input;
