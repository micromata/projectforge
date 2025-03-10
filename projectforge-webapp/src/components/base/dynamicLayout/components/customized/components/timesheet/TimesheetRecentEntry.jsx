import PropTypes from 'prop-types';
import React from 'react';
import { getServiceURL, handleHTTPErrors } from '../../../../../../../utilities/rest';

const filterRecent = (
    search,
    {
        kost2,
        task,
        location,
        tag,
        reference,
        description,
    },
) => {
    const kost2Number = kost2 ? kost2.formattedNumber : '';
    const project = kost2 ? kost2.project : '';
    const customer = project ? project.customer : '';
    const projectName = project ? project.name : '';
    const customerName = customer ? customer.name : '';
    const taskTitle = task ? task.title : '';
    const str = `${taskTitle}|${kost2Number}|${projectName}|${customerName}|${location}|${tag}|${reference}|${description}`.toLocaleLowerCase();
    return str.includes(search.toLocaleLowerCase());
};

function TimesheetRecentEntry(
    {
        callback,
        cost2Visible = false,
        recent,
    },
) {
    const {
        kost2,
        task,
        location,
        tag,
        reference,
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
                ...recent,
            }),
        },
    )
        .then(handleHTTPErrors)
        .then((body) => body.json())
        .then(callback)
        // eslint-disable-next-line no-alert
        .catch((error) => alert(`Internal error: ${error}`));

    const project = kost2 ? kost2.project : undefined;
    const customer = project ? project.customer : undefined;

    return (
        <tr onClick={handleRowClick}>
            {cost2Visible && (
                <>
                    <td>{kost2 ? kost2.formattedNumber : ''}</td>
                    <td>{customer ? customer.name : ''}</td>
                    <td>{project ? project.name : ''}</td>
                </>
            )}
            <td>{task ? task.title : ''}</td>
            <td>{location || ''}</td>
            <td>{tag || ''}</td>
            <td>{reference || ''}</td>
            <td>{description || ''}</td>
        </tr>
    );
}

TimesheetRecentEntry.propTypes = {
    callback: PropTypes.func.isRequired,
    recent: PropTypes.shape({
        kost2: PropTypes.shape({
            formattedNumber: PropTypes.string,
            project: PropTypes.string,
        }),
        task: PropTypes.shape({
            title: PropTypes.string,
        }),
        location: PropTypes.string,
        tag: PropTypes.string,
        reference: PropTypes.string,
        description: PropTypes.string,
    }).isRequired,
    cost2Visible: PropTypes.bool,
};

export { filterRecent };
export default TimesheetRecentEntry;
