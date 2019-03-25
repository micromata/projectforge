import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import style from './Input.module.scss';

function CheckBox(
    {
        className,
        color,
        id,
        label,
        props
    },
) {
    return (
        <div className={classNames(style.formGroup, className)}>
            <label
                className={style.checkboxLabel}
                htmlFor={id}
            >
                <input
                    type="checkbox"
                    className={style.checkbox}
                    id={id}
                    {...props}
                />
                <span className={classNames(style.text, style[color])}>{label}</span>
            </label>
        </div>
    );
}

CheckBox.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string,
    className: PropTypes.string,
    color: colorPropType,
};

CheckBox.defaultProps = {
    label: undefined,
    className: undefined,
    color: undefined,
};

export default CheckBox;
