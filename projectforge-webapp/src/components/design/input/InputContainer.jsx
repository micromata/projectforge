import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import styles from './Input.module.scss';

function InputContainer(
    {
        children,
        className,
        isActive,
        color,
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
        </div>
    );
}

InputContainer.propTypes = {
    children: PropTypes.node.isRequired,
    className: PropTypes.string,
    color: colorPropType,
    isActive: PropTypes.bool,
};

InputContainer.defaultProps = {
    className: undefined,
    color: undefined,
    isActive: false,
};

export default InputContainer;
