import { faStar } from '@fortawesome/free-regular-svg-icons';
import { faCog } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React from 'react';
import Select from 'react-select';
import {
    Button,
    Card,
    CardBody,
    Col,
    Container,
    Popover,
    PopoverBody,
    PopoverHeader,
    Row,
    UncontrolledTooltip,
} from 'reactstrap';
import EditableMultiValueLabel from '../../components/base/page/layout/EditableMultiValueLabel';
import style from '../../components/design/input/Input.module.scss';
import LoadingContainer from '../../components/design/loading-container';
import { getServiceURL } from '../../utilities/rest';
import CalendarPanel from '../panel/calendar/CalendarPanel';
import { customStyles } from './Calendar.module';
import UncontrolledReactSelect from '../../components/base/page/layout/UncontrolledReactSelect';

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
            translations: undefined,
            settingsPopoverOpen: false,
        };

        this.fetchInitial = this.fetchInitial.bind(this);
        this.onChange = this.onChange.bind(this);
        this.toggleSettingsPopover = this.toggleSettingsPopover.bind(this);
        this.handleMultiValueChange = this.handleMultiValueChange.bind(this);
        this.changeDefaultCalendar = this.changeDefaultCalendar.bind(this);
    }

    componentDidMount() {
        this.fetchInitial();
    }

    onChange(activeCalendars) {
        activeCalendars.sort((a, b) => a.title.localeCompare(b.title));
        this.setState({ activeCalendars });
    }

    toggleSettingsPopover() {
        this.setState(prevState => ({
            settingsPopoverOpen: !prevState.settingsPopoverOpen,
        }));
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
                    translations,
                } = json;
                this.setState({
                    loading: false,
                    date: new Date(date),
                    teamCalendars,
                    activeCalendars,
                    listOfDefaultCalendars,
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
            loading,
            settingsPopoverOpen,
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
                                <Col sm={11}>
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
                                </Col>
                                <Col sm={1}>
                                    <Row>
                                        <Button
                                            id="settingsPopover"
                                            color="link"
                                            className="selectPanelIconLinks"
                                            onClick={this.toggleSettingsPopover}
                                        >
                                            <FontAwesomeIcon
                                                icon={faCog}
                                                className={style.icon}
                                                size="lg"
                                            />
                                        </Button>
                                        <Button
                                            color="link"
                                            className="selectPanelIconLinks"
                                            disabled
                                        >
                                            <FontAwesomeIcon
                                                icon={faStar}
                                                className={style.icon}
                                                size="lg"
                                            />
                                        </Button>
                                    </Row>
                                </Col>
                            </Row>

                        </form>
                    </CardBody>
                </Card>
                <CalendarPanel
                    defaultDate={date}
                    defaultView={view}
                    activeCalendars={activeCalendars}
                    topHeight="225px"
                />
                <Popover
                    placement="bottom-end"
                    isOpen={settingsPopoverOpen}
                    target="settingsPopover"
                    toggle={this.toggleSettingsPopover}
                    trigger="legacy"
                >
                    <PopoverHeader toggle={this.settingsPopoverOpen}>
                        {translations['calendar.filter.dialog.title']}
                    </PopoverHeader>
                    <PopoverBody>
                        <Container>
                            <Row>
                                <Col>
                                    <UncontrolledReactSelect
                                        label={translations['calendar.defaultCalendar']}
                                        tooltip={translations['calendar.defaultCalendar.tooltip']}
                                        // data={data}
                                        id="id"
                                        values={listOfDefaultCalendars}
                                        changeDataField={this.changeDefaultCalendar}
                                        translations={translations}
                                        valueProperty="id"
                                        labelProperty="title"
                                    />
                                </Col>
                            </Row>
                            <Row>
                                <Col>Zeitberichtsuser</Col>
                            </Row>
                            <Row>
                                <Col>
                                    Optionen: Pausen, Statistik,
                                    Geburtstage, Planungen
                                </Col>
                            </Row>
                        </Container>
                        [ToDo: Buttons Übernehmen, Reset]
                    </PopoverBody>
                </Popover>
            </LoadingContainer>
        );
    }
}

CalendarPage.propTypes = {};

CalendarPage.defaultProps = {};

export default CalendarPage;
