import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../../utilities/propTypes';
import style from '../Navigation.module.scss';

function MenuBadge({ children, flying, color }) {
    return (
        <span className={classNames(style.badge, { [style.flying]: flying }, style[color])}>
            {children}
        </span>
    );
}

MenuBadge.propTypes = {
    children: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number,
    ]).isRequired,
    color: colorPropType,
    flying: PropTypes.bool,
};

MenuBadge.defaultProps = {
    color: undefined,
    flying: false,
};

export default MenuBadge;
