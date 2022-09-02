import PropTypes from 'prop-types';
import React from 'react';
import DiffViewer from 'react-diff-viewer';
import { DiffMethod } from 'react-diff-viewer/lib/compute-lines';

function DiffText({ oldValue, newValue, compareMethod }) {
    return (
        <DiffViewer
            oldValue={oldValue}
            newValue={newValue}
            showDiffOnly
            splitView={false}
            compareMethod={compareMethod}
            hideLineNumbers
        />
    );
}

DiffText.propTypes = {
    newValue: PropTypes.string.isRequired,
    oldValue: PropTypes.string.isRequired,
    compareMethod: PropTypes.string,
};

DiffText.defaultProps = {
    compareMethod: DiffMethod.WORDS,
};

export default DiffText;
