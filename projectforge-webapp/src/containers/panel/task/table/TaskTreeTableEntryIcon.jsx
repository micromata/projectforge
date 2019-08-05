import { faFile } from '@fortawesome/free-regular-svg-icons';
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
        case 'LEAF':
            icon = faFile;
            break;
        default:
    }

    return (
        <FontAwesomeIcon icon={icon} className={style.entryIcon} />
    );
}

TaskTreeTableEntryIcon.propTypes = {
    treeStatus: PropTypes.oneOf(['OPEN', 'CLOSED', 'LEAF']).isRequired,
};

TaskTreeTableEntryIcon.defaultProps = {};

export default TaskTreeTableEntryIcon;
