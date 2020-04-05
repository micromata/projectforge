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

function TimesheetTemplatesAndRecent() {
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

    const [recentVisible, setRecentVisible] = React.useState(false);
    const recentRef = React.useRef(null);
    const [recent, setRecent] = React.useState({ timesheets: [] });
    const [search, setSearch] = React.useState('');

    React.useEffect(
        () => {
            fetch(
                getServiceURL('timesheet/recent'),
                { credentials: 'include' },
            )
                .then(handleHTTPErrors)
                .then(body => body.json())
                .then(setRecent)
                .catch(() => setRecent({ timesheets: [] }));
        },
        [],
    );

    // Handle mouse events
    useClickOutsideHandler(recentRef, () => setRecentVisible(false), recentVisible);

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
                setRecentVisible(!recentVisible);
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
                    <span ref={recentRef}>
                        <Button
                            color="link"
                            className="selectPanelIconLinks"
                            onClick={toggleModal}
                        >
                            {ui.translations['timesheet.recent']}
                        </Button>
                        <Collapse
                            isOpen={recentVisible}
                            style={{
                                maxHeight: '600px',
                                overflow: 'scroll',
                                scroll: 'auto',
                            }}
                        >
                            <Card>
                                <CardBody>
                                    <Input
                                        id="taskRecentSearch"
                                        label={ui.translations['search.search']}
                                        value={search}
                                        onChange={handleSearchChange}
                                        icon={faSearch}
                                    />
                                    <Table striped hover responsive>
                                        <thead>
                                            <tr>
                                                {recent.cost2Visible && (
                                                    <React.Fragment>
                                                        <th>{ui.translations['fibu.kost2']}</th>
                                                        <th>{ui.translations['fibu.kunde']}</th>
                                                        <th>{ui.translations['fibu.projekt']}</th>
                                                    </React.Fragment>
                                                )}
                                                <th>{ui.translations.task}</th>
                                                <th>{ui.translations['timesheet.location']}</th>
                                                <th>{ui.translations['timesheet.description']}</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {recent.timesheets
                                                .filter(recent => filterRecent(
                                                    search.toLowerCase(),
                                                    recent,
                                                ))
                                                .map(({ counter, ...recent }) => (
                                                    <TimesheetRecentEntry
                                                        key={counter}
                                                        callback={({ variables: newVariables }) => {
                                                            setVariables({
                                                                task: newVariables.task,
                                                            });
                                                            setData(newVariables.data);
                                                            setRecentVisible(false);
                                                        }}
                                                        cost2Visible={recent.cost2Visible}
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
            recent,
            recentRef,
            recentVisible,
            search,
            setData,
            setTimesheetFavorites,
            setVariables,
            timesheetFavorites,
            ui.translations,
        ],
    );
}

TimesheetTemplatesAndRecent.propTypes = {};

TimesheetTemplatesAndRecent.defaultProps = {};

export default TimesheetTemplatesAndRecent;
