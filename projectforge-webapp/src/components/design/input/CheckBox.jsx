import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import AdditionalLabel from './AdditionalLabel';
import style from './Input.module.scss';

function CheckBox(
    {
        additionalLabel,
        className,
        color,
        id,
        label,
        ...props
    },
) {
    return (
        <div className={classNames(style.formGroup, className, style.checkboxGroup)}>
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
            <AdditionalLabel title={additionalLabel} />
        </div>
    );
}

CheckBox.propTypes = {
    id: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    label: PropTypes.string,
    className: PropTypes.string,
    color: colorPropType,
};

CheckBox.defaultProps = {
    additionalLabel: undefined,
    label: undefined,
    className: undefined,
    color: undefined,
};

export default CheckBox;
