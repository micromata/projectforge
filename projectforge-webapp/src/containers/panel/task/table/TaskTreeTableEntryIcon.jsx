import { faChevronDown, faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import style from '../TaskTreePanel.module.scss';

function TaskTreeTableEntryIcon({ treeStatus }) {
    let icon;

    switch (treeStatus) {
        case 'OPENED':
            icon = faChevronDown;
            break;
        case 'CLOSED':
            icon = faChevronRight;
            break;
        default:
    }

    return (
        <div className={style.entryIcon}>
            {icon ? <FontAwesomeIcon icon={icon} /> : undefined}
        </div>
    );
}

TaskTreeTableEntryIcon.propTypes = {
    treeStatus: PropTypes.oneOf(['OPENED', 'CLOSED', 'LEAF']).isRequired,
};

TaskTreeTableEntryIcon.defaultProps = {};

export default TaskTreeTableEntryIcon;
