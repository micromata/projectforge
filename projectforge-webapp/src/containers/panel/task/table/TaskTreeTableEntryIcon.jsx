import { faChevronDown, faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import TaskTreeContext from '../TaskTreeContext';
import style from '../TaskTreePanel.module.scss';

const TreeStatus = {
    OPENED: 'OPENED',
    CLOSED: 'CLOSED',
    LEAF: 'LEAF',
};

TreeStatus.isFolder = (status) => status !== TreeStatus.OPENED && status !== TreeStatus.CLOSED;
TreeStatus.getIcon = (status) => {
    switch (status) {
        case TreeStatus.OPENED:
            return faChevronDown;
        case TreeStatus.CLOSED:
            return faChevronRight;
        default:
            return undefined;
    }
};

function TaskTreeTableEntryIcon({ taskId, treeStatus }) {
    const { toggleTask } = React.useContext(TaskTreeContext);

    const handleClick = (event) => {
        if (TreeStatus.isFolder(treeStatus)) {
            return;
        }

        event.stopPropagation();
        toggleTask(taskId, treeStatus);
    };

    const icon = TreeStatus.getIcon(treeStatus);

    return (
        <div className={style.taskIcon}>
            {icon ? <FontAwesomeIcon icon={icon} onClick={handleClick} /> : undefined}
        </div>
    );
}

TaskTreeTableEntryIcon.propTypes = {
    treeStatus: PropTypes.oneOf([
        TreeStatus.OPENED,
        TreeStatus.CLOSED,
        TreeStatus.LEAF,
    ]).isRequired,
    taskId: PropTypes.number,
};

TaskTreeTableEntryIcon.defaultProps = {
    taskId: undefined,
};

TaskTreeTableEntryIcon.defaultProps = {};

export default TaskTreeTableEntryIcon;
