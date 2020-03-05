import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import styles from './Input.module.scss';

function InputContainer(
    {
        children,
        className,
        color,
        isActive,
        label,
        withMargin,
        ...props
    },
) {
    return (
        <div
            className={classNames(
                styles.inputContainer,
                {
                    [styles.isActive]: isActive,
                    [styles.withMargin]: withMargin,
                },
                styles[color],
                className,
            )}
            {...props}
        >
            {children}
            {label && (
                <span className={styles.labelText}>
                    {label}
                </span>
            )}
        </div>
    );
}

InputContainer.propTypes = {
    children: PropTypes.node.isRequired,
    className: PropTypes.string,
    color: colorPropType,
    isActive: PropTypes.bool,
    label: PropTypes.string,
    withMargin: PropTypes.bool,
};

InputContainer.defaultProps = {
    className: undefined,
    color: undefined,
    isActive: false,
    label: undefined,
    withMargin: false,
};

export default InputContainer;
