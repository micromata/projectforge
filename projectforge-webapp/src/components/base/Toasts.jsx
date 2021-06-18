import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { clearAllToasts, removeToast } from '../../actions';
import { colorPropType } from '../../utilities/propTypes';
import { Toast, ToastBody, ToastHeader } from '../design';

function Toasts({ onClear, onToastRemove, toasts }) {
    const handleDismissClick = (id) => (event) => {
        if (event.shiftKey) {
            onClear();
        } else {
            onToastRemove(id);
        }
    };

    return (
        <div
            className="p-3 my-2 rounded bg-docs-transparent-grid"
            style={{
                position: 'absolute',
                top: 0,
                right: 0,
            }}
        >
            {toasts
                .filter(({ dismissed }) => dismissed !== true)
                .reverse()
                .map((toast) => (
                    <Toast key={`toast-${toast.id}`}>
                        <ToastHeader toggle={handleDismissClick(toast.id)} icon={toast.color}>
                            ProjectForge
                        </ToastHeader>
                        <ToastBody>{toast.message}</ToastBody>
                    </Toast>
                ))}
        </div>
    );
}

Toasts.propTypes = {
    onClear: PropTypes.func.isRequired,
    onToastRemove: PropTypes.func.isRequired,
    toasts: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string,
        message: PropTypes.string,
        color: colorPropType,
    })).isRequired,
};

Toasts.defaultProps = {};

const mapStateToProps = ({ toasts }) => ({ toasts });

const actions = {
    onToastRemove: removeToast,
    onClear: clearAllToasts,
};

export default connect(mapStateToProps, actions)(Toasts);
