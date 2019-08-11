import React from 'react';
import { Button, Card, CardBody, Table } from 'reactstrap';
import FavoritesPanel from '../../../../../../containers/panel/favorite/FavoritesPanel';
import { fetchJsonGet, fetchJsonPost } from '../../../../../../utilities/rest';
import { DynamicLayoutContext } from '../../../context';
import { Collapse } from '../../../../../design';

function TimesheetTemplatesAndRecents() {
    const {
        data,
        setData,
        setVariables,
        translations,
        ui,
        variables,
    } = React.useContext(DynamicLayoutContext);
    const [
        timesheetFavorites,
        setTimesheetFavorites,
    ] = React.useState(variables.timesheetFavorites);

    const [recentsVisible, setRecentsVisible] = React.useState(false);
    const [columnsVisibility, setColumnsVisibility] = React.useState([]);
    const recentsRef = React.useRef(null);

    // Handle mouse events
    React.useEffect(() => {
        const handleClickOutside = ({ target }) => {
            if (recentsRef.current && !recentsRef.current.contains(target)) {
                setRecentsVisible(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

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
                                    <Table striped hover responsive>
                                        <thead>
                                        <tr>
                                            {columnsVisibility.kost2
                                                ? <th>{translations['fibu.kost2']}</th> : undefined}
                                            <th>Kunde</th>
                                            <th>Projekt</th>
                                            <th>Strukturelement</th>
                                            <th>Ort</th>
                                            <th>Tätigkeitsbericht</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        </tbody>
                                    </Table>
                                </CardBody>
                            </Card>
                        </div>
                    </Collapse>
                </React.Fragment>
            );
        },
        [timesheetFavorites,
            ui.translations,
            data,
            setTimesheetFavorites,
            setData,
            setVariables,
            recentsRef,
            recentsVisible],
    );
}

TimesheetTemplatesAndRecents.propTypes = {};

TimesheetTemplatesAndRecents.defaultProps = {};

export default TimesheetTemplatesAndRecents;
