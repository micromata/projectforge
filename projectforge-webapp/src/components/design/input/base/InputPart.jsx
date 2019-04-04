import PropTypes from 'prop-types';
import React from 'react';
import BasePart from './Part';
import style from './Base.module.scss';

function InputPart(props) {
    return (
        <BasePart flexSize={1}>
            <input
                type="text"
                className={style.input}
                {...props}
            />
        </BasePart>
    );
}

InputPart.propTypes = {
    id: PropTypes.string.isRequired,
};

export default InputPart;
