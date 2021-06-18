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

    const [recentListVisible, setRecentListVisible] = React.useState(false);
    const recentListRef = React.useRef(null);
    const [recentList, setRecentList] = React.useState({ timesheets: [] });
    const [search, setSearch] = React.useState('');

    React.useEffect(
        () => {
            fetch(
                getServiceURL('timesheet/recentList'),
                { credentials: 'include' },
            )
                .then(handleHTTPErrors)
                .then((body) => body.json())
                .then(setRecentList)
                .catch(() => setRecentList({ timesheets: [] }));
        },
        [],
    );

    // Handle mouse events
    useClickOutsideHandler(recentListRef, () => setRecentListVisible(false), recentListVisible);

    const handleSearchChange = ({ target }) => setSearch(target.value);

    return React.useMemo(
        () => {
            const handleFavoriteCreate = (newFilterName) => fetchJsonPost(
                'timesheet/favorites/create',
                {
                    name: newFilterName,
                    timesheet: data,
                },
                ({ timesheetFavorites: response }) => setTimesheetFavorites(response),
            );

            const handleFavoriteDelete = (id) => fetchJsonGet(
                'timesheet/favorites/delete',
                { id },
                ({ timesheetFavorites: response }) => setTimesheetFavorites(response),
            );

            const handleFavoriteSelect = (id) => fetchJsonPost('timesheet/favorites/select',
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
                setRecentListVisible(!recentListVisible);
            };

            return (
                <>
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
                    <span ref={recentListRef}>
                        <Button
                            color="link"
                            className="selectPanelIconLinks"
                            onClick={toggleModal}
                        >
                            {ui.translations['timesheet.recent']}
                        </Button>
                        <Collapse
                            isOpen={recentListVisible}
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
                                        selectOnFocus
                                    />
                                    <Table striped hover responsive>
                                        <thead>
                                            <tr>
                                                {recentList.cost2Visible && (
                                                    <>
                                                        <th>{ui.translations['fibu.kost2']}</th>
                                                        <th>{ui.translations['fibu.kunde']}</th>
                                                        <th>{ui.translations['fibu.projekt']}</th>
                                                    </>
                                                )}
                                                <th>{ui.translations.task}</th>
                                                <th>{ui.translations['timesheet.location']}</th>
                                                <th>{ui.translations['timesheet.description']}</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {recentList.timesheets
                                                .filter((recent) => filterRecent(
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
                                                            setRecentListVisible(false);
                                                        }}
                                                        cost2Visible={recentList.cost2Visible}
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
                </>
            );
        },
        [
            data,
            recentList,
            recentListRef,
            recentListVisible,
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
