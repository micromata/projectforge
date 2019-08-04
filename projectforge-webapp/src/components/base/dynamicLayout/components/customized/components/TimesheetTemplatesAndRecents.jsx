import React from 'react';
import { DynamicLayoutContext } from '../../../context';
import FavoritesPanel from '../../../../../../containers/panel/favorite/FavoritesPanel';
import { getServiceURL, handleHTTPErrors } from '../../../../../../utilities/rest';

function TimesheetTemplatesAndRecents() {
    const { ui, variables } = React.useContext(DynamicLayoutContext);
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

    // @Fin: Bei Create müsste eigentlich auch das aktuelle Formular zum Server gesendet werden, damit
    // ein neuer Favorit serverseitig gespeichert werden kann.
    const handleFavoriteCreate = newFilterName => fetchTimesheetFavorites('timesheet/favorites/create', { newFilterName });

    // @Fin: Die zurückgebene Liste der Favoriten wird nicht in setTimesheetFavorites aktualisiert. Der gelöschte
    // Eintrag bleibt in der Liste. Erst wenn ich die Seite neu lade, stimmt die Liste wieder.
    const handleFavoriteDelete = id => fetchTimesheetFavorites('timesheet/favorites/delete', { id });

    // @Fin: Hier soll das Edit-Page-Model geändert werden, allerdings nur in den Feldern, die auch kommen.
    // Wenn z. B. Description undefinied ist, soll die vorhandene Description nicht überschrieben werden.
    const saveUpdateResponseInState = (json) => {
        const newState = {
            ...json,
        };
        this.setState(newState);
    };

    // @Fin: hier würde ich am liebsten auch die aktuellen Data-Felder mitsenden, um serverseitig
    // entscheiden zu können, was ich überschreibe (Kost2, Location etc.)
    // Kann dieser Call auch fetchTimesheetFavorites aufrufen? Dann müssen ggf. alle Callees
    // noch ein Callable mitgeben (saveUpdateResponseInState bzw. setTimesheetFavorites).
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
        .then(saveUpdateResponseInState)
        .catch(error => alert(`Internal error: ${error}`));

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
