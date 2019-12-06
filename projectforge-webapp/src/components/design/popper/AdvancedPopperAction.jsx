import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import styles from './Popper.module.scss';

function AdvancedPopperAction({ type, children, ...props }) {
    return (
        <button
            type="button"
            className={classNames(styles.action, styles[type])}
            {...props}
        >
            {children}
        </button>
    );
}

AdvancedPopperAction.propTypes = {
    children: PropTypes.node.isRequired,
    type: PropTypes.oneOf(['delete']).isRequired,
};

AdvancedPopperAction.defaultProps = {};

export default AdvancedPopperAction;
