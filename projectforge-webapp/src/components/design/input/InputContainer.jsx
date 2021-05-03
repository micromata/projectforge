import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../utilities/propTypes';
import AdditionalLabel from './AdditionalLabel';
import styles from './Input.module.scss';

function InputContainer(
    {
        additionalLabel,
        children,
        className,
        color,
        isActive,
        label,
        readOnly,
        withMargin,
        ...props
    },
) {
    return (
        <>
            <div
                className={classNames(
                    styles.inputContainer,
                    {
                        [styles.isActive]: isActive,
                        [styles.withMargin]: withMargin,
                        [styles.readOnly]: readOnly,
                    },
                    styles[color],
                    className,
                )}
                {...props}
            >
                {children}
                {label && <span className={styles.labelText}>{label}</span>}
            </div>
            <AdditionalLabel title={additionalLabel} />
        </>
    );
}

InputContainer.propTypes = {
    children: PropTypes.node.isRequired,
    additionalLabel: PropTypes.string,
    className: PropTypes.string,
    color: colorPropType,
    isActive: PropTypes.bool,
    label: PropTypes.node,
    readOnly: PropTypes.bool,
    withMargin: PropTypes.bool,
};

InputContainer.defaultProps = {
    additionalLabel: undefined,
    className: undefined,
    color: undefined,
    isActive: false,
    label: undefined,
    readOnly: false,
    withMargin: false,
};

export default InputContainer;
