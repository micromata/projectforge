import React from 'react';
import { DynamicLayoutContext } from '../../../context';
import FavoritesPanel from '../../../../../../containers/panel/favorite/FavoritesPanel';
import { fetchJsonGet, fetchJsonPost } from '../../../../../../utilities/rest';

function TimesheetTemplatesAndRecents() {
    const { ui, variables, data } = React.useContext(DynamicLayoutContext);
    const [
        timesheetFavorites,
        setTimesheetFavorites,
    ] = React.useState(variables.timesheetFavorites);

    const handleFavoriteCreate = (newFilterName) => fetchJsonPost('timesheet/favorites/create',
        {
            name: newFilterName,
            timesheet: data,
        },
        setTimesheetFavorites);

    // @Fin: Die zurückgebene Liste der Favoriten wird nicht in setTimesheetFavorites aktualisiert. Der gelöschte
    // Eintrag bleibt in der Liste. Erst wenn ich die Seite neu lade, stimmt die Liste wieder.
    const handleFavoriteDelete = id => fetchJsonGet('timesheet/favorites/delete',
        { id },
        setTimesheetFavorites);

    // @Fin: Hier soll das Edit-Page-Model geändert werden.
    const saveUpdateResponseInState = (json) => {
        console.log(json);
        const newState = {
            ...json,
        };
        this.setState(newState);
    };

    const handleFavoriteSelect = id => fetchJsonPost('timesheet/favorites/select',
        {
            id,
            timesheet: data,
        },
        saveUpdateResponseInState);

    const handleFavoriteRename = (favoriteId, newName) => fetchJsonGet('timesheet/favorites/rename',
        {
            id: favoriteId,
            newName,
        },
        setTimesheetFavorites);

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
