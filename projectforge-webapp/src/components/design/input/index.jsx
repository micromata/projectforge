import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import styles from './Input.module.scss';

const Input = React.forwardRef((
    {
        additionalLabel,
        autoFocus,
        className,
        color,
        icon,
        iconProps,
        id,
        label,
        onBlur,
        onFocus,
        noStyle,
        value,
        ...props
    },
    ref,
) => {
    // Initialize inputRef
    let inputRef = React.useRef(null);

    // Override ref with forwarded ref
    if (ref) {
        inputRef = ref;
    }

    const [isActive, setIsActive] = React.useState(false);

    const handleBlur = (event) => {
        if (onBlur) {
            onBlur(event);
        }

        setIsActive(false);
    };

    const handleFocus = (event) => {
        if (onFocus) {
            onFocus(event);
        }

        setIsActive(true);
    };

    React.useLayoutEffect(() => {
        if (autoFocus && inputRef.current) {
            inputRef.current.focus();
        }
    }, [autoFocus]);

    return (
        <div
            className={classNames(
                styles.inputField,
                className,
                {
                    [styles.noLabel]: !label,
                    [styles.noStyle]: noStyle,
                },
            )}
        >
            <label
                className={classNames(
                    {
                        [styles.isActive]: value || isActive,
                    },
                    styles[color],
                )}
                htmlFor={id}
            >
                {icon && (
                    <FontAwesomeIcon
                        icon={icon}
                        {...iconProps}
                        className={classNames(styles.icon, iconProps && iconProps.className)}
                    />
                )}
                <input
                    ref={inputRef}
                    id={id}
                    {...props}
                    onBlur={handleBlur}
                    onFocus={handleFocus}
                    value={value}
                />
                <span className={styles.labelText}>{label}</span>
            </label>
            {additionalLabel && (
                <span className={styles.additionalLabel}>{additionalLabel}</span>
            )}
        </div>
    );
});

Input.propTypes = {
    id: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    autoFocus: PropTypes.bool,
    className: PropTypes.string,
    color: colorPropType,
    icon: PropTypes.shape({}),
    iconProps: PropTypes.shape({}),
    label: PropTypes.string,
    onBlur: PropTypes.func,
    onFocus: PropTypes.func,
    noStyle: PropTypes.bool,
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
};

Input.defaultProps = {
    additionalLabel: undefined,
    autoFocus: false,
    className: undefined,
    color: undefined,
    icon: undefined,
    iconProps: undefined,
    label: undefined,
    onBlur: undefined,
    onFocus: undefined,
    noStyle: false,
    value: undefined,
};

export default Input;
