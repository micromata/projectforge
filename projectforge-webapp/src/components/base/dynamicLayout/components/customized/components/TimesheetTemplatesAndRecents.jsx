import React from 'react';
import { DynamicLayoutContext } from '../../../context';
import FavoritesPanel from '../../../../../../containers/panel/favorite/FavoritesPanel';
import { getServiceURL, handleHTTPErrors } from '../../../../../../utilities/rest';

function TimesheetTemplatesAndRecents() {
    const { ui, variables, data } = React.useContext(DynamicLayoutContext);
    const [
        timesheetFavorites,
        setTimesheetFavorites,
    ] = React.useState(variables.timesheetFavorites);

    const fetchTimesheetFavorites = (url, params = undefined) => fetch(
        getServiceURL(url, params), {
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

    const handleFavoriteCreate = (newFilterName) => {
        console.log(data); // @Fin: Leider sind das nicht die aktuellen Daten der Form :-(
        fetch(getServiceURL('timesheet/favorites/create'), {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                name: newFilterName,
                timesheet: data,
            }),
        })
            .then(response => response.json())
            .then(setTimesheetFavorites)
            .catch(error => alert(`Internal error: ${error}`));
    };

    // @Fin: Die zurückgebene Liste der Favoriten wird nicht in setTimesheetFavorites aktualisiert. Der gelöschte
    // Eintrag bleibt in der Liste. Erst wenn ich die Seite neu lade, stimmt die Liste wieder.
    const handleFavoriteDelete = id => fetchTimesheetFavorites('timesheet/favorites/delete', { id });

    // @Fin: Hier soll das Edit-Page-Model geändert werden.
    const saveUpdateResponseInState = (json) => {
        console.log(json);
        const newState = {
            ...json,
        };
        this.setState(newState);
    };

    const handleFavoriteSelect = (id) => {
        console.log(data); // @Fin: Leider sind das nicht die aktuellen Daten der Form :-(
        fetch(getServiceURL('timesheet/favorites/select'), {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                id,
                timesheet: data,
            }),
        })
            .then(response => response.json())
            .then(saveUpdateResponseInState)
            .catch(error => alert(`Internal error: ${error}`));
    };

    const handleFavoriteRename = (favoriteId, newName) => fetchTimesheetFavorites('timesheet/favorites/rename',
        {
            id: favoriteId,
            newName,
        });

    return React.useMemo(
        () => (
            <React.Fragment>
                <FavoritesPanel
                    onFavoriteCreate={handleFavoriteCreate}
                    onFavoriteDelete={handleFavoriteDelete}
                    onFavoriteRename={handleFavoriteRename}
                    onFavoriteSelect={handleFavoriteSelect}
                    translations={ui.translations}
                    favorites={timesheetFavorites}
                    closeOnSelect={false}
                    htmlId="timesheetFavoritesPopover"
                    favoriteButtonText={`${ui.translations.templates} | `}
                />
                Recents
            </React.Fragment>
        ),
        [variables.task.consumption], // @Fin: Was muss hierhin? consumption ist noch von Copy&Paste.
    );
}

TimesheetTemplatesAndRecents.propTypes = {};

TimesheetTemplatesAndRecents.defaultProps = {};

export default TimesheetTemplatesAndRecents;
