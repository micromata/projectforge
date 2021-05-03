import React from 'react';
import 'react-rrule-generator/build/styles.css';
import { Col, Row } from 'reactstrap';
import { Input } from '../../../../../design';
import ReactSelect from '../../../../../design/react-select/ReactSelect';
import { DynamicLayoutContext } from '../../../context';

function CalendarEventReminder() {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const options = [
        {
            value: undefined,
            label: ui.translations['plugins.teamcal.event.reminder.NONE'],
        },
        {
            value: 'MESSAGE',
            label: ui.translations['plugins.teamcal.event.reminder.MESSAGE'],
        },
        {
            value: 'MESSAGE_SOUND',
            label: ui.translations['plugins.teamcal.event.reminder.MESSAGE_SOUND'],
        },
    ];

    const units = [
        {
            value: 'MINUTES',
            label: ui.translations['plugins.teamcal.event.reminder.MINUTES_BEFORE'],
        },
        {
            value: 'HOURS',
            label: ui.translations['plugins.teamcal.event.reminder.HOURS_BEFORE'],
        },
        {
            value: 'DAYS',
            label: ui.translations['plugins.teamcal.event.reminder.DAYS_BEFORE'],
        },
    ];

    const onReminderChange = (option) => {
        // console.log(option)
        setData({ reminderActionType: option.value });
    };

    const onUnitChange = (option) => {
        setData({ reminderDurationUnit: option.value });
    };

    const handleInputChange = (event) => {
        // console.log(event.target.value)
        setData({ reminderDuration: event.target.value });
    };

    if (!data.reminderDurationUnit) {
        setData({ reminderDurationUnit: 'MINUTES' });
    }
    if (!data.reminderDuration) {
        setData({ reminderDuration: '15' });
    }

    const defaultReminder = options.find((element) => element.value
        === data.reminderActionType) || options[0];
    const defaultUnit = units.find((element) => element.value
        === data.reminderDurationUnit) || units[0];
    return React.useMemo(
        () => (
            <Row>
                <Col sm={4}>
                    <ReactSelect
                        label={ui.translations['plugins.teamcal.event.reminder']}
                        translations={ui.translations}
                        values={options}
                        value={defaultReminder}
                        onChange={onReminderChange}
                        required
                    />
                </Col>
                {data.reminderActionType
                    ? (
                        <>
                            <Col sm={1}>
                                <Input
                                    label=""
                                    id="reminderDuration"
                                    value={data.reminderDuration.toString()}
                                    onChange={handleInputChange}
                                />
                            </Col>
                            <Col sm={4}>
                                <ReactSelect
                                    label=""
                                    translations={ui.translations}
                                    values={units}
                                    value={defaultUnit}
                                    onChange={onUnitChange}
                                    required
                                />
                            </Col>
                        </>
                    ) : undefined}
            </Row>
        ),
        [data.reminderActionType, data.reminderDuration, data.reminderDurationUnit],
    );
}

CalendarEventReminder.propTypes = {};

CalendarEventReminder.defaultProps = {};

export default CalendarEventReminder;
