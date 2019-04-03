import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../../utilities/propTypes';
import revisedRandomId from '../../../../utilities/revisedRandomId';
import { UncontrolledTooltip } from '../../../design';
import style from '../Navigation.module.scss';

function MenuBadge(
    {
        children,
        color,
        flying,
        tooltip,
    },
) {
    const id = `menu-badge-${revisedRandomId()}`;

    return (
        <React.Fragment>
            <span
                className={classNames(style.badge, { [style.flying]: flying }, style[color])}
                id={id}
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
    color: colorPropType,
    flying: PropTypes.bool,
    tooltip: PropTypes.string,
};

MenuBadge.defaultProps = {
    color: undefined,
    flying: false,
    tooltip: undefined,
};

export default MenuBadge;
