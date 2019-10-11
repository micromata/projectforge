import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import style from './CalendarInput.module.scss';

function TimeInputUnit(
    {
        children,
        className,
        onClick,
        selected,
    },
) {
    const handleClick = () => onClick(children);

    return (
        <li
            className={classNames(
                className,
                { [style.selected]: selected === children },
            )}
            onClick={handleClick}
            role="presentation"
        >
            {children}
        </li>
    );
}

TimeInputUnit.propTypes = {
    children: PropTypes.number.isRequired,
    onClick: PropTypes.func.isRequired,
    className: PropTypes.string,
    selected: PropTypes.number,
};

TimeInputUnit.defaultProps = {
    className: undefined,
    selected: -1,
};

export default TimeInputUnit;
