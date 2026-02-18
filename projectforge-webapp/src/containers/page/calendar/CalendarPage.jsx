import React, { useEffect, useState } from 'react';
import Select from 'react-select';
import { Col, Container, Row } from '../../../components/design';
import EditableMultiValueLabel from '../../../components/design/EditableMultiValueLabel';
import LoadingContainer from '../../../components/design/loading-container';
import { getTranslation } from '../../../utilities/layout';
import { fetchJsonGet } from '../../../utilities/rest';
import CalendarFilterSettings from '../../panel/calendar/CalendarFilterSettings';
import FullCalendarPanel from '../../panel/calendar/FullCalendarPanel';
import FavoritesPanel from '../../panel/favorite/FavoritesPanel';
import customStyles from './Calendar.module';
import { CalendarContext, defaultValues as calendarContextDefaultValues } from './CalendarContext';

function CalendarPage() {
    const [loading, setLoading] = useState(true);
    const [translations, setTranslations] = useState(undefined);
    const queryParams = new URLSearchParams(window.location.search);
    const dateParam = queryParams.get('date');
    const [state, setState] = useState({
        colors: {},
        date: dateParam,
        view: undefined,
        alternateHoursBackground: undefined,
        teamCalendars: undefined,
        activeCalendars: [],
        filter: {
            defaultCalendarId: undefined,
            gridSize: 30,
            firstHour: 8,
            showBreaks: undefined,
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

    const onShowBreaksChange = (showBreaks) => {
        setState((prevState) => ({
            ...prevState,
            filter: {
                ...prevState.filter,
                showBreaks,
            },
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

    const onFirstHourChange = (firstHour) => {
        setState((prevState) => ({
            ...prevState,
            filter: {
                ...prevState.filter,
                firstHour,
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
            alternateHoursBackground: newState.alternateHoursBackground
                || prevState.alternateHoursBackground,
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

    /**
     * Force polling/refresh of external calendar subscriptions (if any displayed).
     */
    const refresh = () => {
        setLoading(true);
        fetchJsonGet(
            'calendar/refresh',
            undefined,
            () => {
                // const { reload } = json;
                // if (reload) {
                // Force reload page independent of refreshing subscribed calendars:
                window.location.reload(false);
                // }
                setLoading(false);
            },
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
        translations,
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
                                <Col sm="9" md="10" xl="10">
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
                                <Col sm="3" md="2" xl="2" className="d-flex justify-content-end">
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
                                        newFavoriteI18nKey="calendar.templates.new"
                                        newFavoriteTooltipI18nKey="calendar.templates.new.tooltip"
                                        favoriteButtonTooltip={translations['calendar.templates.tooltip']}
                                    />
                                    <CalendarFilterSettings
                                        /* eslint-disable-next-line max-len */
                                        listOfDefaultCalendars={state.listOfDefaultCalendars}
                                        defaultCalendarId={state.filter.defaultCalendarId}
                                        otherTimesheetUsersEnabled={
                                            state.filter.otherTimesheetUsersEnabled
                                        }
                                        timesheetUser={state.timesheetUser}
                                        showBreaks={state.filter.showBreaks}
                                        gridSize={state.filter.gridSize}
                                        firstHour={state.filter.firstHour}
                                        translations={translations}
                                        onTimesheetUserChange={onTimesheetUserChange}
                                        onShowBreaksChange={onShowBreaksChange}
                                        onDefaultCalendarChange={
                                            onDefaultCalendarChange
                                        }
                                        onGridSizeChange={onGridSizeChange}
                                        onFirstHourChange={onFirstHourChange}
                                        onVacationGroupsChange={onVacationGroupsChange}
                                        onVacationUsersChange={onVacationUsersChange}
                                        vacationGroups={state.vacationGroups}
                                        vacationUsers={state.vacationUsers}
                                        refresh={refresh}
                                    />
                                </Col>
                            </Row>
                        </form>
                    </div>
                    {state.date && (
                        <FullCalendarPanel
                            defaultDate={state.date}
                            defaultView={state.view}
                            alternateHoursBackground={state.alternateHoursBackground}
                            gridSize={state.filter.gridSize}
                            firstHour={state.filter.firstHour}
                            activeCalendars={state.activeCalendars}
                            timesheetUserId={state.timesheetUser?.id}
                            showBreaks={state.filter.showBreaks}
                            topHeight="250px"
                            translations={translations}
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
};

export default CalendarPage;
