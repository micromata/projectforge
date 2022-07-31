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

TreeStatus.isLeaf = (status) => status !== TreeStatus.OPENED && status !== TreeStatus.CLOSED;
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

function TaskTreeTableEntryNavigation({
    id, treeStatus, indent, title,
}) {
    const { toggleTask } = React.useContext(TaskTreeContext);

    const handleClick = (event) => {
        if (TreeStatus.isLeaf(treeStatus)) {
            // Click on leafs will result in selection.
            return;
        }
        event.stopPropagation();
        toggleTask(id, treeStatus);
    };

    const icon = TreeStatus.getIcon(treeStatus);

    return (
        // eslint-disable-next-line max-len
        // eslint-disable-next-line jsx-a11y/click-events-have-key-events,jsx-a11y/no-noninteractive-element-interactions
        <td style={{ paddingLeft: `${indent * 1.5 + 0.75}rem`, whiteSpace: 'nowrap' }} onClick={handleClick}>
            <div className={style.taskIcon}>
                {icon ? <FontAwesomeIcon icon={icon} /> : undefined}
            </div>
            {title}
        </td>
    );
}

TaskTreeTableEntryNavigation.propTypes = {
    treeStatus: PropTypes.oneOf([
        TreeStatus.OPENED,
        TreeStatus.CLOSED,
        TreeStatus.LEAF,
    ]).isRequired,
    id: PropTypes.number,
    indent: PropTypes.number,
    title: PropTypes.string,
};

TaskTreeTableEntryNavigation.defaultProps = {
    id: undefined,
    indent: 1,
    title: '',
};

TaskTreeTableEntryNavigation.defaultProps = {};

export default TaskTreeTableEntryNavigation;
