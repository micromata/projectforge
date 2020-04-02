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
) => {
    const kost2Number = kost2 ? kost2.formattedNumber : '';
    const projekt = kost2 ? kost2.projekt : '';
    const kunde = projekt ? projekt.kunde : '';
    const projektName = projekt ? projekt.name : '';
    const kundeName = kunde ? kunde.name : '';
    const taskTitle = task ? task.title : '';
    const str = `${taskTitle}|${kost2Number}|${projektName}|${kundeName}|${location}|${description}`.toLocaleLowerCase();
    return str.includes(search.toLocaleLowerCase());
};

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

    const projekt = kost2 ? kost2.projekt : undefined;
    const kunde = projekt ? projekt.kunde : undefined;

    return (
        <tr onClick={handleRowClick}>
            {cost2Visible && (
                <React.Fragment>
                    <td>{kost2 ? kost2.formattedNumber : ''}</td>
                    <td>{kunde ? kunde.name : ''}</td>
                    <td>{projekt ? projekt.name : ''}</td>
                </React.Fragment>
            )}
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
