import PropTypes from 'prop-types';
import React from 'react';
import { getServiceURL, handleHTTPErrors } from '../../../../../../../utilities/rest';

const filterRecent = (
    search,
    {
        kost2,
        task,
        location,
        description,
    },
) => (
    (kost2 && kost2.formattedNumber && kost2.formattedNumber.includes(search))
    || (
        task && task.title && task.title.toLowerCase()
            .includes(search)
    )
    || (location && location.includes(search))
    || (description && location.includes(search))
);

function TimesheetRecentEntry(
    {
        callback,
        cost2Visible,
        data,
        recent,
    },
) {
    const {
        kost2,
        task,
        location,
        description,
    } = recent;

    const handleRowClick = () => fetch(
        getServiceURL('timesheet/selectRecent'),
        {
            credentials: 'include',
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
            },
            body: JSON.stringify({
                ...data,
                ...recent,
            }),
        },
    )
        .then(handleHTTPErrors)
        .then(body => body.json())
        .then(callback)
        .catch(error => alert(`Internal error: ${error}`));

    return (
        <tr onClick={handleRowClick}>
            {cost2Visible && <td>{kost2 ? kost2.formattedNumber : ''}</td>}
            {/* TODO ADD DATA */}
            <td>???</td>
            <td>???</td>
            <td>{task ? task.title : ''}</td>
            <td>{location || ''}</td>
            <td>{description || ''}</td>
        </tr>
    );
}

TimesheetRecentEntry.propTypes = {
    callback: PropTypes.func.isRequired,
    data: PropTypes.shape({}).isRequired,
    recent: PropTypes.shape({
        kost2: PropTypes.shape({
            formattedNumber: PropTypes.string,
        }),
        task: PropTypes.shape({
            title: PropTypes.string,
        }),
        location: PropTypes.string,
        description: PropTypes.string,
    }).isRequired,
    cost2Visible: PropTypes.bool,
};

TimesheetRecentEntry.defaultProps = {
    cost2Visible: false,
};

export { filterRecent };
export default TimesheetRecentEntry;
