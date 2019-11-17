import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import AdditionalLabel from './AdditionalLabel';
import style from './Input.module.scss';

function Input(
    {
        additionalLabel,
        className,
        color,
        icon,
        id,
        label,
        onBlur,
        onFocus,
        placeholder,
        small,
        type,
        value,
        ...props
    },
) {
    // Use new React Hook Feature
    // https://reactjs.org/docs/hooks-intro.html
    const [active, setActive] = React.useState(false);

    const handleBlur = (event) => {
        if (onBlur) {
            onBlur(event);
        }

        setActive(event.target.value !== '');
    };

    const handleFocus = (event) => {
        if (onFocus) {
            onFocus(event);
        }

        setActive(true);
    };

    return (
        <div
            className={classNames(
                style.formGroup,
                'form-group',
                { [style.small]: small },
                className,
            )}
        >
            <label
                className={classNames(
                    style.label,
                    {
                        [style.active]: value || active,
                        [style.noLabel]: label === undefined,
                        [style.withIcon]: icon !== undefined,
                    },
                    style[color],
                )}
                htmlFor={id}
            >
                {icon ? <FontAwesomeIcon icon={icon} className={style.icon} /> : undefined}
                <input
                    className={style.input}
                    type={type}
                    id={id}
                    {...props}
                    onBlur={handleBlur}
                    onFocus={handleFocus}
                    value={value}
                />
                <span className={style.text}>{placeholder || label}</span>
            </label>
            <AdditionalLabel title={additionalLabel} />
        </div>
    );
}

Input.propTypes = {
    id: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    className: PropTypes.string,
    color: colorPropType,
    icon: PropTypes.shape({}),
    label: PropTypes.string,
    onBlur: PropTypes.func,
    onFocus: PropTypes.func,
    placeholder: PropTypes.string,
    small: PropTypes.bool,
    type: PropTypes.string,
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
};

Input.defaultProps = {
    additionalLabel: undefined,
    className: undefined,
    color: undefined,
    icon: undefined,
    label: undefined,
    onBlur: undefined,
    onFocus: undefined,
    placeholder: undefined,
    small: false,
    type: 'text',
    value: undefined,
};

export default Input;
