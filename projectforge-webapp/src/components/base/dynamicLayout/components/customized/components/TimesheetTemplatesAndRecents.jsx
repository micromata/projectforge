import React from 'react';
import { DynamicLayoutContext } from '../../../context';
import FavoritesPanel from '../../../../../../containers/panel/favorite/FavoritesPanel';
import { getServiceURL, handleHTTPErrors } from '../../../../../../utilities/rest';

function TimesheetTemplatesAndRecents() {
    const { ui, variables } = React.useContext(DynamicLayoutContext);
    const [timesheetFavorites, setTimesheetFavorites] = React.useState(undefined);

    console.log(variables, timesheetFavorites)

    const handleFavoriteCreate = newFilterName => fetch(getServiceURL('timesheet/favorites/create',
        { newFilterName }), {
        method: 'GET',
        credentials: 'include',
        headers: {
            Accept: 'application/json',
        },
    })
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(setTimesheetFavorites)
        .catch(error => alert(`Internal error: ${error}`));

    const handleFavoriteDelete = id => fetch(getServiceURL('timesheet/favorites/delete',
        { id }), {
        method: 'GET',
        credentials: 'include',
        headers: {
            Accept: 'application/json',
        },
    })
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(setTimesheetFavorites)
        .catch(error => alert(`Internal error: ${error}`));

    const handleFavoriteSelect = id => fetch(getServiceURL('timesheet/favorites/select',
        { id }), {
        method: 'GET',
        credentials: 'include',
        headers: {
            Accept: 'application/json',
        },
    })
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(setTimesheetFavorites)
        .catch(error => alert(`Internal error: ${error}`));

    const handleFavoriteRename = (favoriteId, newName) => fetch(
        getServiceURL(
            'timesheet/favorites/rename',
            {
                id: favoriteId,
                newName,
            },
        ),
        {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(setTimesheetFavorites)
        .catch(error => alert(`Internal error: ${error}`));

    const handleFavoriteUpdate = (id) => {
        fetch(getServiceURL('calendar/updateFilter',
            { id }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(setTimesheetFavorites)
            .catch(error => alert(`Internal error: ${error}`));
    };

    return React.useMemo(
        () => (
            <React.Fragment>
                <FavoritesPanel
                    handleFavoriteCreate={handleFavoriteCreate}
                    handleFavoriteDelete={handleFavoriteDelete}
                    handleFavoriteRename={handleFavoriteRename}
                    handleFavoriteSelect={handleFavoriteSelect}
                    handleFavoriteUpdate={handleFavoriteUpdate}
                    translations={ui.translations}
                    favorites={timesheetFavorites}
                    closeOnSelect={false}
                    htmlId="timesheetFavoritesPopover"
                    favoriteButtonText={`${ui.translations.templates} | `}
                />
                Recents
            </React.Fragment>
        ),
        [variables.task.consumption],
    );
}

TimesheetTemplatesAndRecents.propTypes = {};

TimesheetTemplatesAndRecents.defaultProps = {};

export default TimesheetTemplatesAndRecents;
