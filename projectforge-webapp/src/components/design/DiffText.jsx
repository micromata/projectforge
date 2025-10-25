import PropTypes from 'prop-types';
import React from 'react';
import { parseDiff, Diff, Hunk } from 'react-diff-view';
import 'react-diff-view/style/index.css';
import './DiffText.css';
import _ from 'lodash';

function createUnifiedDiff(oldValue, newValue) {
    // Convert values to strings and split into lines
    const oldLines = _.toString(oldValue).split('\n');
    const newLines = _.toString(newValue).split('\n');

    // Create a git-style unified diff header
    const diffHeader = `diff --git a/old b/new
--- a/old
+++ b/new
@@ -1,${oldLines.length} +1,${newLines.length} @@`;

    // Create the diff content with + and - prefixes
    const diffContent = [
        ...oldLines.map((line) => `-${line}`),
        ...newLines.map((line) => `+${line}`),
    ].join('\n');

    return `${diffHeader}\n${diffContent}`;
}

function DiffText({ oldValue = '', newValue, inline = false }) {
    // If inline mode is requested and values are simple strings, render inline
    if (inline && _.isString(oldValue) && _.isString(newValue)) {
        const hasOldValue = oldValue && oldValue.trim().length > 0;
        const hasNewValue = newValue && newValue.trim().length > 0;

        return (
            <span className="diff-text-inline">
                {hasOldValue && (
                    <span
                        className="diff-text-old"
                        style={{
                            backgroundColor: '#ffeef0',
                            textDecoration: 'line-through',
                            padding: '2px 4px',
                            marginRight: '4px',
                            borderRadius: '3px',
                        }}
                    >
                        {oldValue}
                    </span>
                )}
                {hasNewValue && (
                    <span
                        className="diff-text-new"
                        style={{
                            backgroundColor: '#e6ffec',
                            padding: '2px 4px',
                            borderRadius: '3px',
                        }}
                    >
                        {newValue}
                    </span>
                )}
            </span>
        );
    }

    // Create unified diff format
    const diffText = createUnifiedDiff(oldValue, newValue);

    // Parse the diff
    const [file] = parseDiff(diffText);

    if (!file || !file.hunks) {
        return null;
    }

    return (
        <Diff
            viewType="unified"
            diffType={file.type}
            hunks={file.hunks}
            gutterType="none"
        >
            {(hunks) => hunks.map((hunk) => (
                <Hunk
                    key={hunk.content}
                    hunk={hunk}
                />
            ))}
        </Diff>
    );
}

DiffText.propTypes = {
    // eslint-disable-next-line react/forbid-prop-types
    newValue: PropTypes.any.isRequired, // string, number, boolean, array, ...
    // eslint-disable-next-line react/forbid-prop-types
    oldValue: PropTypes.any, // string, number, boolean, array, ...
    inline: PropTypes.bool, // If true, render as inline diff with strikethrough for old value
};

export default DiffText;
