import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import styles from './Input.module.scss';

function InputContainer({ children, isActive, color }) {
    return (
        <div
            className={classNames(
                styles.inputContainer,
                { [styles.isActive]: isActive },
                styles[color],
            )}
        >
            {children}
        </div>
    );
}

InputContainer.propTypes = {
    children: PropTypes.node.isRequired,
    color: colorPropType,
    isActive: PropTypes.bool,
};

InputContainer.defaultProps = {
    color: undefined,
    isActive: false,
};

export default InputContainer;
