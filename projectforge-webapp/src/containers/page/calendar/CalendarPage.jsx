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
    const queryParams = new URLSearchParams(window.location.search);
    const dateParam = queryParams.get('date');
    const [state, setState] = useState({
        colors: {},
        date: dateParam,
        view: undefined,
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
        if (newState.translations) {
            setTranslations(newState.translations);
        }
        // console.log('saveUpdateResponseInState', newState);
        setState((prevState) => ({
            ...prevState,
            colors: newState.colors || prevState.colors,
            date: newState.date || prevState.date,
            view: newState.view || prevState.view,
            teamCalendars: newState.teamCalendars || prevState.teamCalendars,
            activeCalendars: newState.activeCalendars || prevState.activeCalendars,
            styleMap: newState.styleMap || prevState.styleMap,
            filter: newState.filter || prevState.filter,
            // eslint-disable-next-line max-len
            listOfDefaultCalendars: newState.listOfDefaultCalendars || prevState.listOfDefaultCalendars,
            timesheetUser: newState.timesheetUser || prevState.timesheetUser,
            filterFavorites: newState.filterFavorites || prevState.filterFavorites,
            // eslint-disable-next-line max-len
            isFilterModified: newState.isFilterModified || (newState.isFilterModified === undefined && prevState.isFilterModified),
            vacationGroups: newState.vacationGroups || prevState.vacationGroups,
            vacationUsers: newState.vacationUsers || prevState.vacationUsers,
        }));
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
                    <div className="calendar-filter">
                        <form>
                            <Row>
                                <Col sm="10" md="11">
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
                                <Col sm="2" md="1" className="d-flex justify-content-end">
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
                    </div>
                    {state.date && (
                        <FullCalendarPanel
                            defaultDate={state.date}
                            defaultView={state.view}
                            gridSize={state.filter.gridSize}
                            activeCalendars={state.activeCalendars}
                            timesheetUserId={state.timesheetUser?.id}
                            topHeight="250px"
                            translations={translations}
                            match={match}
                            location={location}
                            vacationGroups={state.vacationGroups}
                            vacationUsers={state.vacationUsers}
                        />
                    )}
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
