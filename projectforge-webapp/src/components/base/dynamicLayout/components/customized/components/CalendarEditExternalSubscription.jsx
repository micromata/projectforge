import React from 'react';
import 'react-rrule-generator/build/styles.css';
import { Col, Row } from 'reactstrap';
import { Input } from '../../../../../design';
import CheckBox from '../../../../../design/input/CheckBox';
import { DynamicLayoutContext } from '../../../context';
import ReactSelect from '../../../../../design/react-select/ReactSelect';

function CalendarEditExternalSubscription({ values }) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const { intervals } = values;

    const defaultInterval = intervals.find((element) => element.id
        === data.externalSubscriptionUpdateInterval) || values.intervals[0];

    const handleInputChange = (event) => {
        setData({ externalSubscriptionUrl: event.target.value });
    };

    const onIntervalChange = (option) => {
        setData({ externalSubscriptionUpdateInterval: option.id });
    };

    const handleCheckBoxChange = (event) => {
        setData({ externalSubscription: event.target.checked });
    };

    return React.useMemo(
        () => (
            <>
                <Row>
                    <Col sm={3}>
                        <CheckBox
                            label={ui.translations['plugins.teamcal.externalsubscription.label']}
                            tooltip={ui.translations['plugins.teamcal.externalsubscription.label.tooltip']}
                            id="externalSubscription"
                            onChange={handleCheckBoxChange}
                            checked={data.externalSubscription}
                        />
                    </Col>
                    {data.externalSubscription
                        ? (
                            <Col sm={3}>
                                <ReactSelect
                                    label={ui.translations['plugins.teamcal.externalsubscription.updateInterval']}
                                    translations={ui.translations}
                                    values={values.intervals}
                                    value={defaultInterval}
                                    onChange={onIntervalChange}
                                    valueProperty="id"
                                    labelProperty="displayName"
                                    required
                                />
                            </Col>
                        ) : undefined}
                </Row>
                {
                    data.externalSubscription
                        ? (
                            <Row>
                                <Col sm={12}>
                                    <Input
                                        label={ui.translations['plugins.teamcal.externalsubscription.url']}
                                        tooltip={ui.translations['plugins.teamcal.externalsubscription.url.tooltip']}
                                        id="externalSubscriptionUrl"
                                        value={data.externalSubscriptionUrl}
                                        onChange={handleInputChange}
                                    />
                                </Col>
                            </Row>
                        ) : undefined
                }
            </>
        ),
        [data.externalSubscription,
            data.externalSubscriptionUrl,
            data.externalSubscriptionUpdateInterval],
    );
}

CalendarEditExternalSubscription.propTypes = {};

CalendarEditExternalSubscription.defaultProps = {};

export default CalendarEditExternalSubscription;
