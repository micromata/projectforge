import PropTypes from 'prop-types';
import React from 'react';
import style from '../Navigation.module.scss';

function MenuBadge({ children }) {
    return <span className={style.badge}>{children}</span>;
}

MenuBadge.propTypes = {
    children: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number,
    ]).isRequired,
};

export default MenuBadge;
