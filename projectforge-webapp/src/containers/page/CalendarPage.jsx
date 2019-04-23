import React from 'react';
import { Button, Card, CardBody, Col, Row } from 'reactstrap';
import Select from 'react-select';
import makeAnimated from 'react-select/lib/animated';
import CalendarPanel from '../panel/CalendarPanel';
import { getServiceURL } from '../../utilities/rest';
import LoadingContainer from '../../components/design/loading-container';
import { customStyles } from './Calendar.module';

class CalendarPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            date: new Date(),
            view: 'week',
            teamCalendars: undefined,
            activeCalendars: [],
            translations: undefined,
        };

        this.fetchInitial = this.fetchInitial.bind(this);
        this.onChange = this.onChange.bind(this);
    }

    componentDidMount() {
        this.fetchInitial();
    }

    onChange(activeCalendars) {
        activeCalendars.sort((a, b) => a.title.localeCompare(b.title));
        this.setState({ activeCalendars });
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
                                            color="primary"
                                            onClick={this.onSubmit}
                                            type="submit"
                                        >
                                            {translations.search}
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
            </LoadingContainer>
        );
    }
}

CalendarPage.propTypes = {};

CalendarPage.defaultProps = {};

export default CalendarPage;
