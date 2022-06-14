import PropTypes from 'prop-types';
import React from 'react';
import DiffViewer from 'react-diff-viewer';
import { DiffMethod } from 'react-diff-viewer/lib/compute-lines';

function DiffText({ oldValue, newValue }) {
    return (
        <DiffViewer
            oldValue={oldValue}
            newValue={newValue}
            splitView={false}
            compareMethod={DiffMethod.WORDS}
            hideLineNumbers
        />
    );
}

DiffText.propTypes = {
    newValue: PropTypes.string.isRequired,
    oldValue: PropTypes.string.isRequired,
};

export default DiffText;
