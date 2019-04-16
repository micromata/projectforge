import PropTypes from 'prop-types';
import React from 'react';
import { UncontrolledTooltip } from './index';

function DiffText({ id, children, oldValue }) {
    return (
        <React.Fragment>
            <span id={id}>{children}</span>
            <UncontrolledTooltip
                placement="top"
                target={id}
            >
                {oldValue}
            </UncontrolledTooltip>
        </React.Fragment>
    );
}

DiffText.propTypes = {
    children: PropTypes.node.isRequired,
    id: PropTypes.string.isRequired,
    oldValue: PropTypes.string.isRequired,
};

export default DiffText;
