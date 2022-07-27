import PropTypes from 'prop-types';
import React, { useEffect, useState } from 'react';
import Select from 'react-select';
import { Card, CardBody, Col, Container, Row } from '../../../components/design';
import EditableMultiValueLabel from '../../../components/design/EditableMultiValueLabel';
import LoadingContainer from '../../../components/design/loading-container';
import { getTranslation } from '../../../utilities/layout';
import { fetchJsonGet } from '../../../utilities/rest';
import CalendarFilterSettings from '../../panel/calendar/CalendarFilterSettings';
import FullCalendarPanel from '../../panel/calendar/FullCalendarPanel';
import FavoritesPanel from '../../panel/favorite/FavoritesPanel';
import customStyles from './Calendar.module';
import { CalendarContext, defaultValues as calendarContextDefaultValues } from './CalendarContext';

function CalendarPage({ match, location }) {
    const [loading, setLoading] = useState(true);
    const [translations, setTranslations] = useState(undefined);
    const [state, setState] = useState({
        colors: {},
        date: new Date(),
        view: 'timeGridWorkingWeek',
        teamCalendars: undefined,
        activeCalendars: [],
        filter: {
            defaultCalendarId: undefined,
            gridSize: 30,
        },
        listOfDefaultCalendars: [],
        timesheetUser: undefined,
        filterFavorites: undefined,
        isFilterModified: false,
        vacationGroups: [],
        vacationUsers: [],
    });

    React.useEffect(() => {
        if (translations) {
            document.title = `ProjectForge - ${getTranslation('calendar.title', translations)}`;
        }
    }, [translations]);

    const handleMultiValueChange = (id, newValue) => {
        setState((prevState) => ({
            ...prevState,
            [id]: newValue,
        }));
    };

    const onTimesheetUserChange = (newTimesheetUser) => {
        setState((prevState) => ({
            ...prevState,
            timesheetUser: newTimesheetUser,
        }));
    };

    const onVacationGroupsChange = (newVacationGroups) => {
        setState((prevState) => ({
            ...prevState,
            vacationGroups: newVacationGroups,
        }));
    };

    const onVacationUsersChange = (newVacationUsers) => {
        setState((prevState) => ({
            ...prevState,
            vacationUsers: newVacationUsers,
        }));
    };

    const onDefaultCalendarChange = (defaultCalendarId) => {
        setState((prevState) => ({
            ...prevState,
            filter: {
                ...prevState.filter,
                defaultCalendarId,
            },
        }));
    };

    const onGridSizeChange = (gridSize) => {
        setState((prevState) => ({
            ...prevState,
            filter: {
                ...prevState.filter,
                gridSize,
            },
        }));
    };

    const saveUpdateResponseInState = (json) => {
        setLoading(false);
        const newState = {
            ...json,
        };
        if (newState.date) {
            newState.date = new Date(newState.date);
        }
        if (newState.translations) {
            setTranslations(newState.translations);
            newState.translations = undefined;
        }
        setState({ ...newState });
    };

    const onFavoriteCreate = (newFilterName) => {
        fetchJsonGet(
            'calendar/createNewFilter',
            { newFilterName },
            saveUpdateResponseInState,
        );
    };

    const onFavoriteDelete = (id) => {
        fetchJsonGet(
            'calendar/deleteFilter',
            { id },
            saveUpdateResponseInState,
        );
    };

    const onFavoriteSelect = (id) => {
        fetchJsonGet(
            'calendar/selectFilter',
            { id },
            saveUpdateResponseInState,
        );
    };

    const onFavoriteRename = (id, newName) => {
        fetchJsonGet(
            'calendar/renameFilter',
            {
                id,
                newName,
            },
            saveUpdateResponseInState,
        );
    };

    const onChange = (newActiveCalendars) => {
        setState((prevState) => ({
            ...prevState,
            activeCalendars: newActiveCalendars
                // When activeCalendars are set, sort them
                ? newActiveCalendars.sort((a, b) => a.title.localeCompare(b.title))
                // Otherwise set empty array.
                : [],
            isFilterModified: true,
        }));
    };

    const onFavoriteUpdate = (id) => {
        fetchJsonGet(
            'calendar/updateFilter',
            { id },
            saveUpdateResponseInState,
        );
    };

    const fetchInitial = () => {
        fetchJsonGet(
            'calendar/initial',
            undefined,
            saveUpdateResponseInState,
        );
    };

    useEffect(() => {
        fetchInitial(); // Component mounted
    }, []);

    if (!translations) {
        return <div>...</div>;
    }

    const options = state.teamCalendars?.map((option) => ({
        ...option,
        filterType: 'COLOR_PICKER',
        label: option.title,
    })) || [];
    const value = state.activeCalendars?.map((option) => ({
        ...option,
        filterType: 'COLOR_PICKER',
        label: option.title,
    })) || [];
    const colors = state.colors || {};

    return (
        <Container fluid>
            <LoadingContainer loading={loading}>
                <CalendarContext.Provider
                    /* eslint-disable-next-line react/jsx-no-constructed-context-values */
                    value={{
                        ...calendarContextDefaultValues,
                        saveUpdateResponseInState,
                    }}
                >
                    <Card>
                        <CardBody>
                            <form>
                                <Row>
                                    <Col sm="11">
                                        {options && (
                                            <Select
                                                closeMenuOnSelect={false}
                                                components={{
                                                    MultiValueLabel: EditableMultiValueLabel,
                                                }}
                                                getOptionLabel={(option) => (option.title)}
                                                getOptionValue={(option) => (option.id)}
                                                isClearable
                                                isMulti
                                                onChange={onChange}
                                                options={options}
                                                placeholder={translations['select.placeholder']}
                                                setMultiValue={handleMultiValueChange}
                                                styles={customStyles}
                                                values={colors}
                                                value={value}
                                            />
                                        )}
                                    </Col>
                                    <Col sm="1" className="d-flex align-items-center">
                                        <FavoritesPanel
                                            onFavoriteCreate={onFavoriteCreate}
                                            onFavoriteDelete={onFavoriteDelete}
                                            onFavoriteRename={onFavoriteRename}
                                            onFavoriteSelect={onFavoriteSelect}
                                            onFavoriteUpdate={onFavoriteUpdate}
                                            favorites={state.filterFavorites}
                                            translations={translations}
                                            currentFavoriteId={state.filter.id}
                                            isModified={state.isFilterModified}
                                            closeOnSelect={false}
                                            htmlId="calendarFavoritesPopover"
                                        />
                                        <CalendarFilterSettings
                                            /* eslint-disable-next-line max-len */
                                            listOfDefaultCalendars={state.listOfDefaultCalendars}
                                            defaultCalendarId={state.filter.defaultCalendarId}
                                            otherTimesheetUsersEnabled={
                                                state.filter.otherTimesheetUsersEnabled
                                            }
                                            timesheetUser={state.timesheetUser}
                                            gridSize={state.filter.gridSize}
                                            translations={translations}
                                            onTimesheetUserChange={onTimesheetUserChange}
                                            onDefaultCalendarChange={
                                                onDefaultCalendarChange
                                            }
                                            onGridSizeChange={onGridSizeChange}
                                            onVacationGroupsChange={onVacationGroupsChange}
                                            onVacationUsersChange={onVacationUsersChange}
                                            vacationGroups={state.vacationGroups}
                                            vacationUsers={state.vacationUsers}
                                        />
                                    </Col>
                                </Row>
                            </form>
                        </CardBody>
                    </Card>
                    <FullCalendarPanel
                        defaultDate={state.date}
                        defaultView={state.view}
                        gridSize={state.filter.gridSize}
                        activeCalendars={state.activeCalendars}
                        timesheetUserId={state.timesheetUser?.id}
                        topHeight="225px"
                        translations={translations}
                        match={match}
                        location={location}
                        vacationGroups={state.vacationGroups}
                        vacationUsers={state.vacationUsers}
                    />
                </CalendarContext.Provider>
            </LoadingContainer>
        </Container>
    );
}

CalendarPage.propTypes = {
    match: PropTypes.shape({}).isRequired,
    location: PropTypes.shape({}).isRequired,
};

CalendarPage.defaultProps = {};

export default CalendarPage;
