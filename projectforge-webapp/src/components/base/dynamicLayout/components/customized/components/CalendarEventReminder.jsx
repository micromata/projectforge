import React from 'react';
import { Col, Row } from 'reactstrap';
import { DynamicLayoutContext } from '../../../context';
import 'react-rrule-generator/build/styles.css';
import ReactSelect from '../../../../../design/ReactSelect';
import { Input } from '../../../../../design';

function CalendarEventReminder() {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
    const [value, setValue] = React.useState(data.reminderDuration ? String(data.reminderDuration) : '1');

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
        console.log(option)
        setData({ reminderActionType: option.value });
    };

    const onUnitChange = (option) => {
        setData({ reminderDurationUnit: option.value });
    };

    const handleInputChange = (event) => {
        console.log(event.target.value)
        setValue(event.target.value);
        setData({ reminderDuration: event.target.value });
    };

    if (!data.reminderDurationUnit) {
        setData({ reminderDurationUnit: 'MINUTES' });
    }
    if (!data.reminderDuration) {
        setData({ reminderDuration: '15' });
    }

    const defaultReminder = options.find(element => element.value
        === data.reminderActionType) || options[0];
    const defaultUnit = units.find(element => element.value
        === data.reminderDurationUnit) || units[0];
    return React.useMemo(
        () => (
            <Row>
                <Col sm={4}>
                    <ReactSelect
                        label={ui.translations['plugins.teamcal.event.reminder']}
                        translations={ui.translations}
                        values={options}
                        defaultValue={defaultReminder}
                        onChange={onReminderChange}
                        required
                    />
                </Col>
                {data.reminderActionType
                    ? (
                        <React.Fragment>
                            <Col sm={1}>
                                <Input
                                    label=""
                                    id="reminderDuration"
                                    value={value}
                                    onChange={handleInputChange}
                                />
                            </Col>
                            <Col sm={4}>
                                <ReactSelect
                                    label=""
                                    translations={ui.translations}
                                    values={units}
                                    defaultValue={defaultUnit}
                                    onChange={onUnitChange}
                                    required
                                />
                            </Col>
                        </React.Fragment>
                    ) : undefined}
            </Row>
        ),
        [data.reminderActionType, value],
    );
}

CalendarEventReminder.propTypes = {};

CalendarEventReminder.defaultProps = {};

export default CalendarEventReminder;
