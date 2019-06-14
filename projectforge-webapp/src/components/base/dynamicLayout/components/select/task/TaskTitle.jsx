import PropTypes from 'prop-types';
import React from 'react';
import style from './TaskSelect.module.scss';

function TaskTitle(
    {
        id,
        isHighlighted,
        openModal,
        title,
    },
) {
    if (isHighlighted) {
        return (
            <span className={style.highlighted}>
                {title}
            </span>
        );
    }

    return (
        <span
            className="onclick"
            onClick={() => openModal(id)}
            role="presentation"
        >
            {title}
        </span>
    );
}

TaskTitle.propTypes = {
    isHighlighted: PropTypes.bool.isRequired,
    title: PropTypes.string.isRequired,
    id: PropTypes.number,
    openModal: PropTypes.func,
};

TaskTitle.defaultProps = {
    id: undefined,
    openModal: undefined,
};

export default TaskTitle;
