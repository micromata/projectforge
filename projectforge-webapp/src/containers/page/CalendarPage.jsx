import React from 'react';
import Select from 'react-select';
import { Card, CardBody, Row, } from 'reactstrap';
import EditableMultiValueLabel from '../../components/base/page/layout/EditableMultiValueLabel';
import LoadingContainer from '../../components/design/loading-container';
import { getServiceURL } from '../../utilities/rest';
import CalendarPanel from '../panel/calendar/CalendarPanel';
import { customStyles } from './Calendar.module';
import FavoritesPanel from '../panel/FavoritesPanel';
import CalendarFilterSettings from '../panel/calendar/CalendarFilterSettings';

class CalendarPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            colors: {},
            date: new Date(),
            view: 'week',
            teamCalendars: undefined,
            activeCalendars: [],
            listOfDefaultCalendars: [],
            defaultCalendar: undefined,
            filterFavorites: undefined,
            translations: undefined,
        };

        this.fetchInitial = this.fetchInitial.bind(this);
        this.onChange = this.onChange.bind(this);
        this.handleMultiValueChange = this.handleMultiValueChange.bind(this);
        this.changeDefaultCalendar = this.changeDefaultCalendar.bind(this);
        this.onFavoriteCreate = this.onFavoriteCreate.bind(this);
        this.onFavoriteDelete = this.onFavoriteDelete.bind(this);
        this.onFavoriteRename = this.onFavoriteRename.bind(this);
        this.onFavoriteSelect = this.onFavoriteSelect.bind(this);
        this.onFavoriteUpdate = this.onFavoriteUpdate.bind(this);
    }

    componentDidMount() {
        this.fetchInitial();
    }

    onChange(activeCalendars) {
        activeCalendars.sort((a, b) => a.title.localeCompare(b.title));
        this.setState({ activeCalendars });
    }

    onFavoriteCreate(id) {
        console.log(id);
    }

    onFavoriteDelete(id) {
        console.log(id);
    }

    onFavoriteSelect(id) {
        console.log(id);
    }

    onFavoriteRename(id, newName) {
        console.log(id, newName);
    }

    onFavoriteUpdate(id) {
        console.log(id);
    }

    changeDefaultCalendar(defaultCalendar) {
        this.setState({ defaultCalendar });
    }

    fetchInitial() {
        this.setState({ loading: true });
        fetch(getServiceURL('calendar/initial'), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(response => response.json())
            .then((json) => {
                const {
                    date,
                    view,
                    teamCalendars,
                    activeCalendars,
                    listOfDefaultCalendars,
                    filterFavorites,
                    translations,
                } = json;
                this.setState({
                    loading: false,
                    date: new Date(date),
                    teamCalendars,
                    activeCalendars,
                    listOfDefaultCalendars,
                    filterFavorites,
                    view,
                    translations,
                });
            })
            .catch(error => alert(`Internal error: ${error}`));
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
            colors,
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

        const options = teamCalendars.map(option => ({
            ...option,
            filterType: 'COLOR_PICKER',
            label: option.title,
        }));

        return (
            <LoadingContainer loading={loading}>
                <Card>
                    <CardBody>
                        <form>
                            <Row>
                                <CalendarFilterSettings
                                    listOfDefaultCalendars={listOfDefaultCalendars}
                                    translations={translations}
                                />
                                <Select
                                    closeMenuOnSelect={false}
                                    components={{
                                        MultiValueLabel: EditableMultiValueLabel,
                                    }}
                                    defaultValue={activeCalendars.map(option => ({
                                        ...option,
                                        filterType: 'COLOR_PICKER',
                                        label: option.title,
                                    }))}
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
                                    // loadOptions={loadOptions}
                                    // defaultOptions={defaultOptions}
                                />
                                <FavoritesPanel
                                    onFavoriteCreate={this.onFavoriteCreate}
                                    onFavoriteDelete={this.onFavoriteDelete}
                                    onFavoriteRename={this.onFavoriteRename}
                                    onFavoriteSelect={this.onFavoriteSelect}
                                    onFavoriteUpdate={this.onFavoriteUpdate}
                                    favorites={filterFavorites}
                                    translations={translations}
                                />
                            </Row>
                        </form>
                    </CardBody>
                </Card>
                <CalendarPanel
                    defaultDate={date}
                    defaultView={view}
                    activeCalendars={activeCalendars}
                    topHeight="225px"
                    translations={translations}
                />
            </LoadingContainer>
        );
    }
}

CalendarPage.propTypes = {};

CalendarPage.defaultProps = {};

export default CalendarPage;
