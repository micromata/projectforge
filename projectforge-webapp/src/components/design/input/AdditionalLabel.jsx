import PropTypes from 'prop-types';
import React from 'react';
import style from './Input.module.scss';

function AdditionalLabel({ title }) {
    if (!title) {
        return <></>;
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

AdditionalLabel.defaultProps = {
    title: undefined,
};

export default AdditionalLabel;
