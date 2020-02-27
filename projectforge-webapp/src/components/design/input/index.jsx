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
        selectOnFocus,
        value,
        ...props
    },
    ref,
) => {
    // Initialize inputRef
    let inputRef = React.useRef(null);
    const labelRef = React.useRef(null);
    const [labelWidth, setLabelWidth] = React.useState(0);

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

        if (selectOnFocus) {
            setTimeout(() => {
                if (inputRef.current) {
                    inputRef.current.select();
                }
            }, 100);
        }

        setIsActive(true);
    };

    React.useLayoutEffect(() => {
        if (autoFocus && inputRef.current) {
            inputRef.current.focus();
        }
    }, [autoFocus]);

    React.useLayoutEffect(() => {
        if (labelRef.current) {
            setLabelWidth(labelRef.current.clientWidth + (icon ? 100 : 20));
        }
    }, [label]);

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
            style={{ minWidth: labelWidth }}
        >
            <label
                className={classNames(
                    styles.inputContainer,
                    {
                        [styles.isActive]: value || isActive,
                        [styles.withMargin]: !noStyle,
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
                <span
                    ref={labelRef}
                    className={styles.labelText}
                >
                    {label}
                </span>
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
    selectOnFocus: PropTypes.bool,
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
    selectOnFocus: false,
    value: undefined,
};

export default Input;
