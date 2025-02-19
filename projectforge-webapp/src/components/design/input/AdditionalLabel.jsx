import PropTypes from 'prop-types';
import React from 'react';
import style from './Input.module.scss';

function AdditionalLabel({ title }) {
    if (!title) {
        return null;
    }

    return (
        <div className={style.additionalLabel}>
            <span>{title}</span>
        </div>
    );
}

AdditionalLabel.propTypes = {
    title: PropTypes.string,
};

export default AdditionalLabel;
