import PropTypes from 'prop-types';
import React from 'react';
import DiffViewer from 'react-diff-viewer';
import { DiffMethod } from 'react-diff-viewer/lib/compute-lines';
import _ from 'lodash';

function DiffText({ oldValue, newValue, compareMethod }) {
    return (
        <DiffViewer
            oldValue={_.toString(oldValue)}
            newValue={_.toString(newValue)}
            showDiffOnly
            splitView={false}
            compareMethod={compareMethod}
            hideLineNumbers
        />
    );
}

DiffText.propTypes = {
    // eslint-disable-next-line react/forbid-prop-types
    newValue: PropTypes.any.isRequired, // string, number, boolean, array, ...
    // eslint-disable-next-line react/forbid-prop-types
    oldValue: PropTypes.any.isRequired, // string, number, boolean, array, ...
    compareMethod: PropTypes.string,
};

DiffText.defaultProps = {
    compareMethod: DiffMethod.WORDS,
};

export default DiffText;
