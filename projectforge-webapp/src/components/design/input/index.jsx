import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import style from './Input.module.scss';

function Input(
    {
        additionalLabel,
        className,
        color,
        id,
        label,
        type,
        value,
        ...props
    },
) {
    // Use new React Hook Feature
    // https://reactjs.org/docs/hooks-intro.html
    const [active, setActive] = React.useState(value);

    return (
        <div className={classNames(style.formGroup,"form-group", className)}>
            <label
                className={classNames(style.label, { [style.active]: active }, style[color])}
                htmlFor={id}
            >
                <input
                    className={style.input}
                    type={type}
                    id={id}
                    {...props}
                    onFocus={() => setActive(true)}
                    onBlur={event => setActive(event.target.value !== '')}
                    value={value}
                />
                <span className={style.text}>{label}</span>
            </label>
            <div className={style.subLine}>
                {additionalLabel ? <span>{additionalLabel}</span> : undefined}
            </div>
        </div>
    );
}

Input.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    className: PropTypes.string,
    color: colorPropType,
    type: PropTypes.string,
    value: PropTypes.string,
};

Input.defaultProps = {
    additionalLabel: undefined,
    className: undefined,
    color: undefined,
    type: 'text',
    value: undefined,
};

export default Input;
