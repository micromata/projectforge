import { faSearch } from '@fortawesome/free-solid-svg-icons';
import React from 'react';
import { Button, Card, CardBody, Table } from 'reactstrap';
import FavoritesPanel from '../../../../../../../containers/panel/favorite/FavoritesPanel';
import { useClickOutsideHandler } from '../../../../../../../utilities/hooks';
import {
    fetchJsonGet,
    fetchJsonPost,
    getServiceURL,
    handleHTTPErrors,
} from '../../../../../../../utilities/rest';
import { Collapse } from '../../../../../../design';
import Input from '../../../../../../design/input';
import { DynamicLayoutContext } from '../../../../context';
import TimesheetRecentEntry, { filterRecent } from './TimesheetRecentEntry';

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
    const [recents, setRecents] = React.useState({ timesheets: [] });
    const [search, setSearch] = React.useState('');

    React.useEffect(
        () => {
            fetch(
                getServiceURL('timesheet/recents'),
                { credentials: 'include' },
            )
                .then(handleHTTPErrors)
                .then(body => body.json())
                .then(setRecents)
                .catch(() => setRecents({ timesheets: [] }));
        },
        [],
    );

    // Handle mouse events
    useClickOutsideHandler(recentsRef, () => setRecentsVisible(false), recentsVisible);

    const handleSearchChange = ({ target }) => setSearch(target.value);

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
                    <span ref={recentsRef}>
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
                            <Card>
                                <CardBody>
                                    TODO TRANSLATIONS & SHOW KUNDE/PROJEKT
                                    <Input
                                        id="taskRecentSearch"
                                        label="[Suche]"
                                        value={search}
                                        onChange={handleSearchChange}
                                        icon={faSearch}
                                    />
                                    <Table striped hover responsive>
                                        <thead>
                                            <tr>
                                                {recents.cost2Visible
                                                    ? <th>[Kost2]</th>
                                                    : undefined}
                                                <th>[Kunde]</th>
                                                <th>[Projekt]</th>
                                                <th>{ui.translations.task}</th>
                                                <th>[Ort]</th>
                                                <th>[Tätigkeitsbericht]</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {recents.timesheets
                                                .filter(recent => filterRecent(
                                                    search.toLowerCase(),
                                                    recent,
                                                ))
                                                .map(({ hashKey, ...recent }) => (
                                                    <TimesheetRecentEntry
                                                        key={hashKey}
                                                        callback={({ variables: newVariables }) => {
                                                            setVariables(newVariables.task);
                                                            setData(newVariables.data);
                                                            setRecentsVisible(false);
                                                        }}
                                                        cost2Visible={recents.cost2Visible}
                                                        data={data}
                                                        recent={recent}
                                                    />
                                                ))}
                                        </tbody>
                                    </Table>
                                </CardBody>
                            </Card>
                        </Collapse>
                    </span>
                </React.Fragment>
            );
        },
        [
            data,
            recents,
            recentsRef,
            recentsVisible,
            search,
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
