import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Input } from '../index';
import styles from './Popper.module.scss';

function AdvancedPopperInput(
    {
        children,
        dark = false,
        // Extract 'dispatch' so it's not passed to the input tag
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        dispatch,
        forwardRef,
        icon,
        onCancel,
        onKeyDown,
        ...props
    },
) {
    const handleKeyDown = (event) => {
        if (onCancel && event.key === 'Escape') {
            onCancel();
        }

        if (onKeyDown) {
            onKeyDown(event);
        }
    };

    return (
        <div className={classNames(styles.input, { [styles.dark]: dark })}>
            <Input
                ref={forwardRef}
                icon={icon}
                className={styles.container}
                autoComplete="off"
                {...props}
                onKeyDown={handleKeyDown}
            />
            {children}
        </div>
    );
}

AdvancedPopperInput.propTypes = {
    id: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    children: PropTypes.node,
    dark: PropTypes.bool,
    dispatch: PropTypes.func,
    forwardRef: PropTypes.shape({}),
    icon: PropTypes.shape({}),
    onCancel: PropTypes.func,
    onKeyDown: PropTypes.func,
    placeholder: PropTypes.string,
    value: PropTypes.string,
};

export default AdvancedPopperInput;
