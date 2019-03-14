import PropTypes from 'prop-types';
import React from 'react';
import revisedRandomId from '../../../../../utilities/revisedRandomId';
import { Col, Input } from '../../../../design';
import style from '../../Page.module.scss';

function LayoutInput({ id, type, values }) {
    // TODO: VALIDATION
    let children;
    let ColTag = Col;
    const inputProps = {};

    if (type === 'select') {
        children = values.map(option => (
            <option
                value={option.value}
                key={`input-select-value-${revisedRandomId()}`}
            >
                {option.title}
            </option>
        ));

        ColTag = React.Fragment;
        inputProps.className = style.select;
    }

    return (
        <ColTag>
            <Input type={type} name={id} id={id} {...inputProps}>
                {children}
            </Input>
        </ColTag>
    );
}

LayoutInput.propTypes = {
    id: PropTypes.string,
    type: PropTypes.string,
    values: PropTypes.arrayOf(PropTypes.shape({
        value: PropTypes.string,
        title: PropTypes.string,
    })),
};

LayoutInput.defaultProps = {
    id: undefined,
    type: 'text',
    values: [],
};

export default LayoutInput;
