import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import InputBase from './base';
import InputPart from './base/InputPart';

function Input(
    {
        label,
        id,
        color,
        ...props
    },
) {
    return (
        <InputBase label={label} id={id} color={color}>
            <InputPart id={id} {...props} />
        </InputBase>
    );
}

Input.propTypes = {
    label: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
    color: colorPropType,
};

Input.defaultProps = {
    value: undefined,
};

export default Input;
