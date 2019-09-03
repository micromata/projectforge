import React from 'react';
import { Button, Card, CardBody, Table } from 'reactstrap';
import FavoritesPanel from '../../../../../../containers/panel/favorite/FavoritesPanel';
import { useClickOutsideHandler } from '../../../../../../utilities/hooks';
import {
    fetchJsonGet,
    fetchJsonPost,
    getServiceURL,
    handleHTTPErrors,
} from '../../../../../../utilities/rest';
import { Collapse } from '../../../../../design';
import { DynamicLayoutContext } from '../../../context';

function TimesheetTemplatesAndRecents() {
    const {
        data,
        setData,
        setVariables,
        ui,
        variables,
    } = React.useContext(DynamicLayoutContext);
    const [
        timesheetFavorites,
        setTimesheetFavorites,
    ] = React.useState(variables.timesheetFavorites);

    const [recentsVisible, setRecentsVisible] = React.useState(false);
    const recentsRef = React.useRef(null);
    const [recents, setRecents] = React.useState([]);

    React.useEffect(
        () => {
            fetch(
                getServiceURL('timesheet/recents'),
                { credentials: 'include' },
            )
                .then(handleHTTPErrors)
                .then(body => body.json())
                .then(setRecents)
                .catch(() => setRecents([]));
        },
        [],
    );

    // Handle mouse events
    useClickOutsideHandler(recentsRef, () => setRecentsVisible(false), recentsVisible);

    return React.useMemo(
        () => {
            const handleFavoriteCreate = newFilterName => fetchJsonPost(
                'timesheet/favorites/create',
                {
                    name: newFilterName,
                    timesheet: data,
                },
                ({ timesheetFavorites: response }) => setTimesheetFavorites(response),
            );

            const handleFavoriteDelete = id => fetchJsonGet(
                'timesheet/favorites/delete',
                { id },
                ({ timesheetFavorites: response }) => setTimesheetFavorites(response),
            );

            const handleFavoriteSelect = id => fetchJsonPost('timesheet/favorites/select',
                {
                    id,
                    timesheet: data,
                },
                (
                    {
                        data: responseData,
                        variables: responseVariables,
                    },
                ) => {
                    setData(responseData);
                    setVariables(responseVariables);
                });

            const handleFavoriteRename = (favoriteId, newName) => fetchJsonGet(
                'timesheet/favorites/rename',
                {
                    id: favoriteId,
                    newName,
                },
                ({ timesheetFavorites: response }) => setTimesheetFavorites(response),
            );

            const toggleModal = () => {
                setRecentsVisible(!recentsVisible);
            };

            /* eslint-disable indent, react/jsx-indent, react/jsx-tag-spacing */
            return (
                <React.Fragment>
                    <FavoritesPanel
                        onFavoriteCreate={handleFavoriteCreate}
                        onFavoriteDelete={handleFavoriteDelete}
                        onFavoriteRename={handleFavoriteRename}
                        onFavoriteSelect={handleFavoriteSelect}
                        translations={ui.translations}
                        favorites={timesheetFavorites}
                        htmlId="timesheetFavoritesPopover"
                        favoriteButtonText={`${ui.translations.templates} | `}
                    />
                    <Button
                        color="link"
                        className="selectPanelIconLinks"
                        onClick={toggleModal}
                    >
                        Recents
                    </Button>
                    <Collapse
                        isOpen={recentsVisible}
                        style={{
                            maxHeight: '600px',
                            overflow: 'scroll',
                            scroll: 'auto',
                        }}
                    >
                        <div ref={recentsRef}>
                            <Card>
                                <CardBody>
                                    TODO TRANSLATIONS & SHOW KUNDE/PROJKET & SEARCH
                                    <Table striped hover responsive>
                                        <thead>
                                            <tr>
                                                <th>[Kunde]</th>
                                                <th>[Projekt]</th>
                                                <th>{ui.translations.task}</th>
                                                <th>[Ort]</th>
                                                <th>[Tätigkeitsbericht]</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {recents.map(recent => (
                                                <tr
                                                    key={`recent-${recent.task.id}-${recent.description}-${recent.location}`}
                                                >
                                                    <td>???</td>
                                                    <td>???</td>
                                                    <td>{recent.task.title}</td>
                                                    <td>{recent.location}</td>
                                                    <td>{recent.description}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </Table>
                                </CardBody>
                            </Card>
                        </div>
                    </Collapse>
                </React.Fragment>
            );
        },
        [
            data,
            recents,
            recentsRef,
            recentsVisible,
            setData,
            setTimesheetFavorites,
            setVariables,
            timesheetFavorites,
            ui.translations,
        ],
    );
}

TimesheetTemplatesAndRecents.propTypes = {};

TimesheetTemplatesAndRecents.defaultProps = {};

export default TimesheetTemplatesAndRecents;
