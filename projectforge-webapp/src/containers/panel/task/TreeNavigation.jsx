import { faChevronDown, faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import style from './TaskTreePanel.module.scss';

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

function TreeNavigation({
    id, treeStatus, indent, title,
}) {
    const icon = TreeStatus.getIcon(treeStatus);

    return (
        <div style={{ paddingLeft: `${indent * 1.5 + 0.75}rem`, whiteSpace: 'nowrap', cursor: 'pointer' }}>
            <span className={style.taskIcon}>
                {icon ? <FontAwesomeIcon icon={icon} /> : undefined}
            </span>
            {title}
        </div>
    );
}

TreeNavigation.propTypes = {
    treeStatus: PropTypes.oneOf([
        TreeStatus.OPENED,
        TreeStatus.CLOSED,
        TreeStatus.LEAF,
    ]).isRequired,
    id: PropTypes.number,
    indent: PropTypes.number,
    title: PropTypes.string,
};

TreeNavigation.defaultProps = {
    id: undefined,
    indent: 1,
    title: '',
};

TreeNavigation.defaultProps = {};

export default TreeNavigation;
