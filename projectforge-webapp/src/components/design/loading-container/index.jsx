import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import style from './LoadingContainer.module.scss';
import { Spinner } from '..';

function LoadingContainer({ className, children, loading }) {
    return (
        <div className={classNames(style.loadingContainer, className)}>
            {loading
                ? (
                    <div className={style.loadingOverlay}>
                        <Spinner type="grow" color="primary" />
                    </div>
                )
                : undefined
            }
            {children}
        </div>
    );
}

LoadingContainer.propTypes = {
    children: PropTypes.node.isRequired,
    className: PropTypes.string,
    loading: PropTypes.bool,
};

LoadingContainer.defaultProps = {
    className: '',
    loading: false,
};

export default LoadingContainer;
