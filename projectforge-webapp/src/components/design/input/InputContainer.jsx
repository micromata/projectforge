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
        ...props
    },
) {
    return (
        <div
            className={classNames(
                styles.inputContainer,
                { [styles.isActive]: isActive },
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
};

InputContainer.defaultProps = {
    className: undefined,
    color: undefined,
    isActive: false,
    label: undefined,
};

export default InputContainer;
