import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import styles from './Popper.module.scss';

function AdvancedPopperAction({ type, children, ...props }) {
    return (
        <button
            type="button"
            className={classNames(styles.action, styles[type])}
            // eslint-disable-next-line react/jsx-props-no-spreading
            {...props}
        >
            {children}
        </button>
    );
}

AdvancedPopperAction.propTypes = {
    children: PropTypes.node.isRequired,
    type: PropTypes.oneOf(['delete', 'success']).isRequired,
};

AdvancedPopperAction.defaultProps = {};

export default AdvancedPopperAction;
