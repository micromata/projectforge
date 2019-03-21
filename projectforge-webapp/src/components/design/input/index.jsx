import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import style from './Input.module.scss';

function Input(
    {
        label,
        id,
        type,
        className,
        additionalLabel,
        color,
        ...props
    },
) {
    // Use new React Hook Feature
    // https://reactjs.org/docs/hooks-intro.html
    const [active, setActive] = React.useState(false);

    return (
        <div className={classNames(style.formGroup, className)}>
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
    label: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
    value: PropTypes.string,
    type: PropTypes.string,
    className: PropTypes.string,
    additionalLabel: PropTypes.string,
    color: PropTypes.oneOf(['primary', 'secondary', 'success', 'danger', 'warning', 'info']),
    validationMessage: PropTypes.string,
};

Input.defaultProps = {
    type: 'text',
    value: undefined,
    className: undefined,
    additionalLabel: undefined,
    color: undefined,
    validationMessage: undefined,
};

export default Input;
