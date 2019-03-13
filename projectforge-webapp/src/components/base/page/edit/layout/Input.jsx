import PropTypes from 'prop-types';
import React from 'react';
import revisedRandomId from '../../../../../utilities/revisedRandomId';
import { Col, Input } from '../../../../design';
import style from '../../Page.module.scss';

function LayoutInput({ id, type, values }) {
    // TODO: VALIDATION
    let children;

    if (type === 'select') {
        children = values.map(value => (
            <option
                key={`input-select-value-${revisedRandomId()}`}
            >
                {value}
            </option>
        ));

        return (
            <Input type={type} name={id} id={id} className={style.select}>
                {children}
            </Input>
        );
    }

    return (
        <Col>
            <Input type={type} name={id} id={id}>
                {children}
            </Input>
        </Col>
    );
}

LayoutInput.propTypes = {
    id: PropTypes.string,
    type: PropTypes.string,
    values: PropTypes.arrayOf(PropTypes.string),
};

LayoutInput.defaultProps = {
    id: undefined,
    type: 'text',
    values: [],
};

export default LayoutInput;
