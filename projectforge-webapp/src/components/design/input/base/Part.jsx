import PropTypes from 'prop-types';
import React from 'react';
import style from './Base.module.scss';

function BasePart({ children, flexSize }) {
    return (
        <li
            className={style.part}
            style={{ flex: `${flexSize} 0 auto` }}
        >
            {children}
        </li>
    );
}

BasePart.propTypes = {
    children: PropTypes.node.isRequired,
    flexSize: PropTypes.number,
};

BasePart.defaultProps = {
    flexSize: 0,
};

export default BasePart;
