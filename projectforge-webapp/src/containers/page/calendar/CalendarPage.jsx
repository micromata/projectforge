import PropTypes from 'prop-types';
import React from 'react';
import Select from 'react-select';
import { Card, CardBody, Col, Row } from 'reactstrap';
import EditableMultiValueLabel from '../../../components/design/EditableMultiValueLabel';
import LoadingContainer from '../../../components/design/loading-container';
import { getTranslation } from '../../../utilities/layout';
import { fetchJsonGet } from '../../../utilities/rest';
import CalendarFilterSettings from '../../panel/calendar/CalendarFilterSettings';
import CalendarPanel from '../../panel/calendar/CalendarPanel';
import FavoritesPanel from '../../panel/favorite/FavoritesPanel';
import { customStyles } from './Calendar.module';
import { CalendarContext, defaultValues as calendarContextDefaultValues } from './CalendarContext';

class CalendarPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            colors: {},
            date: new Date(),
            view: 'week',
            teamCalendars: undefined,
            activeCalendars: [],
            filter: {
                defaultCalendarId: undefined,
                listOfDefaultCalendars: [],
                gridSize: 30,
            },
            timesheetUser: undefined,
            filterFavorites: undefined,
            isFilterModified: false,
            translations: undefined,
        };

        this.fetchInitial = this.fetchInitial.bind(this);
        this.onChange = this.onChange.bind(this);
        this.handleMultiValueChange = this.handleMultiValueChange.bind(this);
        this.onDefaultCalendarChange = this.onDefaultCalendarChange.bind(this);
        this.onGridSizeChange = this.onGridSizeChange.bind(this);
        this.onFavoriteCreate = this.onFavoriteCreate.bind(this);
        this.onFavoriteDelete = this.onFavoriteDelete.bind(this);
        this.onFavoriteRename = this.onFavoriteRename.bind(this);
        this.onFavoriteSelect = this.onFavoriteSelect.bind(this);
        this.onFavoriteUpdate = this.onFavoriteUpdate.bind(this);
        this.saveUpdateResponseInState = this.saveUpdateResponseInState.bind(this);
        this.onTimesheetUserChange = this.onTimesheetUserChange.bind(this);
    }

    componentDidMount() {
        this.fetchInitial();
    }

    onChange(activeCalendars) {
        activeCalendars.sort((a, b) => a.title.localeCompare(b.title));
        this.setState({
            activeCalendars,
            isFilterModified: true,
        });
    }

    onTimesheetUserChange(timesheetUser) {
        this.setState({ timesheetUser });
    }

    onDefaultCalendarChange(defaultCalendarId) {
        this.setState(currentState => ({
            filter: {
                ...currentState.filter,
                defaultCalendarId,
            },
        }));
    }


    onGridSizeChange(gridSize) {
        this.setState(currentState => ({
            filter: {
                ...currentState.filter,
                gridSize,
            },
        }));
    }

    onFavoriteCreate(newFilterName) {
        fetchJsonGet('calendar/createNewFilter',
            { newFilterName },
            this.saveUpdateResponseInState);
    }

    onFavoriteDelete(id) {
        fetchJsonGet('calendar/deleteFilter',
            { id },
            this.saveUpdateResponseInState);
    }

    onFavoriteSelect(id) {
        fetchJsonGet('calendar/selectFilter',
            { id },
            this.saveUpdateResponseInState);
    }

    onFavoriteRename(id, newName) {
        fetchJsonGet('calendar/renameFilter',
            {
                id,
                newName,
            },
            this.saveUpdateResponseInState);
    }

    onFavoriteUpdate(id) {
        fetchJsonGet('calendar/updateFilter',
            { id },
            this.saveUpdateResponseInState);
    }

    fetchInitial() {
        this.setState({ loading: true });
        fetchJsonGet('calendar/initial',
            undefined,
            this.saveUpdateResponseInState);
    }

    saveUpdateResponseInState(json) {
        const newState = {
            loading: false,
            ...json,
        };
        if (newState.translations) {
            document.title = `ProjectForge - ${getTranslation('calendar.title', newState.translations)}`;
        }

        if (newState.date) {
            newState.date = new Date(newState.date);
        }

        this.setState(newState);
    }

    handleMultiValueChange(id, newValue) {
        this.setState(({ colors }) => ({
            colors: {
                ...colors,
                [id]: newValue,
            },
        }));
    }

    render() {
        const {
            activeCalendars,
            listOfDefaultCalendars,
            isFilterModified,
            colors,
            filter,
            timesheetUser,
            date,
            filterFavorites,
            loading,
            teamCalendars,
            translations,
            view,
        } = this.state;

        if (!translations) {
            return <div>...</div>;
        }

        const { match, location } = this.props;

        const options = teamCalendars.map(option => ({
            ...option,
            filterType: 'COLOR_PICKER',
            label: option.title,
        }));

        return (
            <LoadingContainer loading={loading}>
                <CalendarContext.Provider
                    value={{
                        ...calendarContextDefaultValues,
                        saveUpdateResponseInState: this.saveUpdateResponseInState,
                    }}
                >
                    <Card>
                        <CardBody>
                            <form>
                                <Row>
                                    <Col sm="11">
                                        <Select
                                            closeMenuOnSelect={false}
                                            components={{
                                                MultiValueLabel: EditableMultiValueLabel,
                                            }}
                                            getOptionLabel={option => (option.title)}
                                            getOptionValue={option => (option.id)}
                                            isClearable
                                            isMulti
                                            onChange={this.onChange}
                                            options={options}
                                            placeholder={translations['select.placeholder']}
                                            setMultiValue={this.handleMultiValueChange}
                                            styles={customStyles}
                                            values={colors}
                                            value={activeCalendars.map(option => ({
                                                ...option,
                                                filterType: 'COLOR_PICKER',
                                                label: option.title,
                                            }))}
                                            // loadOptions={loadOptions}
                                            // defaultOptions={defaultOptions}
                                        />
                                    </Col>
                                    <Col sm="1" className="d-flex align-items-center">
                                        <FavoritesPanel
                                            onFavoriteCreate={this.onFavoriteCreate}
                                            onFavoriteDelete={this.onFavoriteDelete}
                                            onFavoriteRename={this.onFavoriteRename}
                                            onFavoriteSelect={this.onFavoriteSelect}
                                            onFavoriteUpdate={this.onFavoriteUpdate}
                                            favorites={filterFavorites}
                                            translations={translations}
                                            currentFavoriteId={filter.id}
                                            isModified={isFilterModified}
                                            closeOnSelect={false}
                                            htmlId="calendarFavoritesPopover"
                                        />
                                        <CalendarFilterSettings
                                            listOfDefaultCalendars={listOfDefaultCalendars}
                                            defaultCalendarId={filter.defaultCalendarId}
                                            otherTimesheetUsersEnabled={
                                                filter.otherTimesheetUsersEnabled
                                            }
                                            timesheetUser={timesheetUser}
                                            gridSize={filter.gridSize}
                                            translations={translations}
                                            onTimesheetUserChange={this.onTimesheetUserChange}
                                            onDefaultCalendarChange={this.onDefaultCalendarChange}
                                            onGridSizeChange={this.onGridSizeChange}
                                        />
                                    </Col>
                                </Row>
                            </form>
                        </CardBody>
                    </Card>
                    <CalendarPanel
                        defaultDate={date}
                        defaultView={view}
                        gridSize={filter.gridSize}
                        activeCalendars={activeCalendars}
                        timesheetUserId={timesheetUser ? timesheetUser.id : undefined}
                        topHeight="225px"
                        translations={translations}
                        match={match}
                        location={location}
                    />
                </CalendarContext.Provider>
            </LoadingContainer>
        );
    }
}

CalendarPage.propTypes = {
    match: PropTypes.shape({}).isRequired,
    location: PropTypes.shape({}).isRequired,
};

CalendarPage.defaultProps = {};

export default CalendarPage;
