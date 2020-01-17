import React from 'react';
import 'react-rrule-generator/build/styles.css';
import { Col, Row } from 'reactstrap';
import { Input } from '../../../../../design';
import ReactSelect from '../../../../../design/ReactSelect';
import CheckBox from '../../../../../design/input/CheckBox';
import { DynamicLayoutContext } from '../../../context';
import { fetchJsonGet } from '../../../../../../utilities/rest';

function CalendarEditExternalSubscription() {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const handleInputChange = (event) => {
        // console.log(event.target.value)
        setData({ reminderDuration: event.target.value });
    };

    const handleCheckBoxChange = (event) => {
        setData({ externalSubscription: event.target.checked });
    }


    return React.useMemo(
        () => (
            <Row>
                <Col sm={4}>
                    <CheckBox
                        label={ui.translations['plugins.teamcal.externalsubscription.label']}
                        tooltip={ui.translations['plugins.teamcal.externalsubscription.label.tooltip']}
                        id="externalSubscription"
                        onChange={handleCheckBoxChange}
                        checked={data.externalSubscription}
                    />
                </Col>
                {data.reminderActionType
                    ? (
                        <React.Fragment>
                            <Col sm={1}>
                                Hurzel
                            </Col>
                            <Col sm={4}>
                                hruez
                            </Col>
                        </React.Fragment>
                    ) : undefined}
            </Row>
        ),
        [data.externalSubscription, data.externalSubscriptionUrl, data.externalSubscriptionUpdateInterval],
    );
}

CalendarEditExternalSubscription.propTypes = {};

CalendarEditExternalSubscription.defaultProps = {};

export default CalendarEditExternalSubscription;
