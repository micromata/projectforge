import React from 'react';
import 'react-rrule-generator/build/styles.css';
import { Alert, Col, Row } from 'reactstrap';
import CheckBox from '../../../../../design/input/CheckBox';
import { DynamicLayoutContext } from '../../../context';

function CalendarSubscriptionInfo({ values }) {
    const { ui } = React.useContext(DynamicLayoutContext);

    const { subscriptionInfo } = values;
    const { remindersExportDefaultValue } = subscriptionInfo;

    const [remindersExport, setRemindersExport] = React.useState(remindersExportDefaultValue);

    const handleCheckBoxChange = (event) => {
        setRemindersExport(event.target.checked);
    };
    const url = remindersExport || !subscriptionInfo.urlWithoutExportedReminders
        ? subscriptionInfo.url : subscriptionInfo.urlWithoutExportedReminders;

    const barcodeImageUrl = `${subscriptionInfo.barcodeUrl}?text=${encodeURIComponent(url)}`;

    return React.useMemo(
        () => (
            <>
                <Row>
                    <Col sm={12}>
                        <h4>{subscriptionInfo.headline}</h4>
                    </Col>
                </Row>
                <Row>
                    <Col sm={12}>
                        <Alert color="danger">
                            <h4 className="alert-heading">{subscriptionInfo.securityAdviseHeadline}</h4>
                            <p>
                                {subscriptionInfo.securityAdvise}
                            </p>
                        </Alert>
                    </Col>
                </Row>
                {subscriptionInfo.urlWithoutExportedReminders
                    ? (
                        <Row>
                            <Col sm={4}>
                                <CheckBox
                                    label={ui.translations['plugins.teamcal.export.reminder.checkbox']}
                                    tooltip={ui.translations['plugins.teamcal.export.reminder.checkbox.tooltip']}
                                    id="remindersExport"
                                    onChange={handleCheckBoxChange}
                                    checked={remindersExport}
                                />
                            </Col>
                            <Col sm={4}>
                                <img alt="barcode" src={barcodeImageUrl} />
                            </Col>
                        </Row>
                    ) : (
                        <Row>
                            <Col sm={12}>
                                <img alt="barcode" src={barcodeImageUrl} />
                            </Col>
                        </Row>
                    )}
                <Row>
                    <Col sm={12}>
                        <a href={url}>{url}</a>
                    </Col>
                </Row>
            </>
        ),
    );
}

CalendarSubscriptionInfo.propTypes = {};

CalendarSubscriptionInfo.defaultProps = {};

export default CalendarSubscriptionInfo;
