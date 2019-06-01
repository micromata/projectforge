import { faStar } from '@fortawesome/free-regular-svg-icons';
import { faCog } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React from 'react';
import Select from 'react-select';
/* eslint-disable-next-line object-curly-newline */
import { Button, Card, CardBody, Col, Modal, ModalBody, ModalHeader, Row } from 'reactstrap';
import EditableMultiValueLabel from '../../components/base/page/layout/EditableMultiValueLabel';
import style from '../../components/design/input/Input.module.scss';
import LoadingContainer from '../../components/design/loading-container';
import { getServiceURL } from '../../utilities/rest';
import CalendarPanel from '../panel/CalendarPanel';
import { customStyles } from './Calendar.module';

class CalendarPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            colors: {},
            date: new Date(),
            view: 'week',
            teamCalendars: undefined,
            activeCalendars: [],
            translations: undefined,
            settingsModal: false,
        };

        this.fetchInitial = this.fetchInitial.bind(this);
        this.onChange = this.onChange.bind(this);
        this.toggleSettingsModal = this.toggleSettingsModal.bind(this);
        this.handleMultiValueChange = this.handleMultiValueChange.bind(this);
    }

    componentDidMount() {
        this.fetchInitial();
    }

    onChange(activeCalendars) {
        activeCalendars.sort((a, b) => a.title.localeCompare(b.title));
        this.setState({ activeCalendars });
    }

    toggleSettingsModal() {
        this.setState(prevState => ({
            settingsModal: !prevState.settingsModal,
        }));
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
                    translations,
                } = json;
                this.setState({
                    loading: false,
                    date: new Date(date),
                    teamCalendars,
                    activeCalendars,
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
            colors,
            date,
            loading,
            settingsModal,
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
                                        defaultValue={activeCalendars}
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
                                            color="link"
                                            className="selectPanelIconLinks"
                                            onClick={this.toggleTaskTreeModal}
                                            disabled
                                        >
                                            <FontAwesomeIcon
                                                icon={faStar}
                                                className={style.icon}
                                                size="lg"
                                            />
                                        </Button>
                                        <Button
                                            color="link"
                                            className="selectPanelIconLinks"
                                            onClick={this.toggleSettingsModal}
                                        >
                                            <FontAwesomeIcon
                                                icon={faCog}
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
                <Modal
                    isOpen={settingsModal}
                    toggle={this.toggleSettingsModal}
                    modalTransition={{ timeout: 100 }}
                    backdropTransition={{ timeout: 150 }}
                >
                    <ModalHeader toggle={this.settingsModal}>
                        {translations['plugins.teamcal.calendar.filterDialog.title']}
                    </ModalHeader>
                    <ModalBody>
                        [ToDo: Standardkalendar, Zeitberichtsuser, Optionen: Pausen, Statistik,
                        Geburtstage, Planungen, Farben?]
                        <br />
                        [ToDo: Buttons Übernehmen, Reset]
                    </ModalBody>
                </Modal>
            </LoadingContainer>
        );
    }
}

CalendarPage.propTypes = {};

CalendarPage.defaultProps = {};

export default CalendarPage;
