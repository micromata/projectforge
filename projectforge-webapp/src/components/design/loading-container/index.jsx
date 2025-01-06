import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Spinner } from '..';
import style from './LoadingContainer.module.scss';

function LoadingContainer({ className, children, loading = false }) {
    return (
        <div className={classNames(style.loadingContainer, className)}>
            {loading && (
                <div className={style.loadingOverlay}>
                    <Spinner type="grow" color="primary" />
                </div>
            )}
            {children}
        </div>
    );
}

LoadingContainer.propTypes = {
    children: PropTypes.node,
    className: PropTypes.string,
    loading: PropTypes.bool,
};

export default LoadingContainer;
