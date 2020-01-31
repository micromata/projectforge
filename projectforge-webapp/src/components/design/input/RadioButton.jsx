import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import AdditionalLabel from './AdditionalLabel';
import style from './Input.module.scss';

function RadioButton(
    {
        additionalLabel,
        className,
        color,
        id,
        name,
        label,
        ...props
    },
) {
    return (
        <div className={classNames(style.formGroup, className, style.radioButtonGroup)}>
            <label
                className={style.radioButtonLabel}
                htmlFor={id}
            >
                <input
                    type="radio"
                    className={style.radio}
                    id={id}
                    name={name}
                    {...props}
                />
                <span className={classNames(style.text, style[color])}>{label}</span>
            </label>
            <AdditionalLabel title={additionalLabel} />
        </div>
    );
}

RadioButton.propTypes = {
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    label: PropTypes.string,
    className: PropTypes.string,
    color: colorPropType,
};

RadioButton.defaultProps = {
    additionalLabel: undefined,
    label: undefined,
    className: undefined,
    color: undefined,
};

export default RadioButton;
