import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../../utilities/propTypes';
import { UncontrolledTooltip } from '../../../design';
import style from '../Navigation.module.scss';

function MenuBadge(
    {
        children,
        color,
        isFlying,
        elementKey,
        tooltip,
        ...props
    },
) {
    const id = `menu-badge-${elementKey}`;

    return (
        <React.Fragment>
            <span
                className={classNames(style.badge, { [style.isFlying]: isFlying }, style[color])}
                id={id}
                {...props}
            >
                {children}
            </span>
            {tooltip
                ? (
                    <UncontrolledTooltip target={id} placement="right">
                        {tooltip}
                    </UncontrolledTooltip>
                )
                : undefined}
        </React.Fragment>
    );
}

MenuBadge.propTypes = {
    children: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number,
    ]).isRequired,
    elementKey: PropTypes.string.isRequired,
    color: colorPropType,
    isFlying: PropTypes.bool,
    tooltip: PropTypes.string,
};

MenuBadge.defaultProps = {
    color: undefined,
    isFlying: false,
    tooltip: undefined,
};

export default MenuBadge;
