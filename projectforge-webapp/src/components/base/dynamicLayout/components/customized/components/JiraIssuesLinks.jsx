import PropTypes from 'prop-types';
import React from 'react';

/**
 * Shows links to given jira issues (if given), otherwise nothing is shown.
 */
function JiraIssuesLinks(
    {
        values = null,
    },
) {
    let jiraIssuesMap;
    let jiraIssues;
    if (values && values.jiraIssues) {
        jiraIssuesMap = values.jiraIssues;
        jiraIssues = Object.keys(jiraIssuesMap);
    }
    return jiraIssues && jiraIssues.length > 0 && (
        jiraIssues.map((issue) => (
            <a
                key={issue}
                href={jiraIssuesMap[issue]}
                rel="noopener noreferrer"
                target="_blank"
            >
                {`${issue} `}
            </a>
        ))
    );
}

JiraIssuesLinks.propTypes = {
    // eslint-disable-next-line react/forbid-prop-types
    values: PropTypes.any,
};

export default JiraIssuesLinks;
