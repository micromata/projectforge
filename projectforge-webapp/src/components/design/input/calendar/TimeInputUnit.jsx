import PropTypes from 'prop-types';
import React from 'react';
import { formatTimeUnit } from '../../../../utilities/layout';

function TimeInputUnit(
    {
        children,
        onClick,
        precision = 1,
        selected = -1,
        ...props
    },
) {
    const handleClick = () => onClick(children);

    const distance = children - selected;
    const style = {};

    if (selected >= 0 && Math.abs(distance) < precision) {
        style.backgroundColor = `rgba(59, 153, 252, ${(precision - Math.abs(distance)) / precision})`;
        style.color = '#fff';
        if (distance < 0) {
            style.borderBottomLeftRadius = 0;
            style.borderBottomRightRadius = 0;
        } else if (distance > 0) {
            style.borderTopLeftRadius = 0;
            style.borderTopRightRadius = 0;
        }
    }

    return (
        <li
            style={style}
            onClick={handleClick}
            role="presentation"
            {...props}
        >
            {formatTimeUnit(children)}
        </li>
    );
}

TimeInputUnit.propTypes = {
    children: PropTypes.number.isRequired,
    onClick: PropTypes.func.isRequired,
    precision: PropTypes.number,
    selected: PropTypes.number,
};

export default TimeInputUnit;
