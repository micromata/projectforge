import React from 'react';
/* eslint-disable-next-line object-curly-newline */
import { Button, Card, CardBody, Col, Modal, ModalBody, ModalHeader, Row } from 'reactstrap';
import Select from 'react-select';
import makeAnimated from 'react-select/lib/animated';
import { faCog } from '@fortawesome/free-solid-svg-icons';
import { faStar } from '@fortawesome/free-regular-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import CalendarPanel from '../panel/CalendarPanel';
import { getServiceURL } from '../../utilities/rest';
import LoadingContainer from '../../components/design/loading-container';
import { customStyles } from './Calendar.module';
import style from '../../components/design/input/Input.module.scss';

class CalendarPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
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

    render() {
        const {
            loading,
            date,
            view,
            teamCalendars,
            activeCalendars,
            translations,
            settingsModal,
        } = this.state;
        if (!translations) {
            return <div>...</div>;
        }
        return (
            <LoadingContainer loading={loading}>
                <Card>
                    <CardBody>
                        <form>
                            <Row>
                                <Col sm={11}>
                                    <Select
                                        components={makeAnimated()}
                                        isMulti
                                        defaultValue={activeCalendars}
                                        closeMenuOnSelect={false}
                                        options={teamCalendars}
                                        isClearable
                                        getOptionValue={option => (option.id)}
                                        getOptionLabel={option => (option.title)}
                                        styles={customStyles}
                                        onChange={this.onChange}
                                        // loadOptions={loadOptions}
                                        // defaultOptions={defaultOptions}
                                        placeholder={translations['select.placeholder']}
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
                    defautlDate={date}
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
                    <ModalHeader toggle={this.settingsModal}>{translations['plugins.teamcal.calendar.filterDialog.title']}</ModalHeader>
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
