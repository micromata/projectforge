import React from 'react';
import PropTypes from 'prop-types';

import EditPage from '../page/edit/index';

class TimesheetEditPanel extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const { timesheetId, startDate, endDate } = this.props;
        const match = {
            params: {
                category: 'timesheet',
                id: timesheetId,
            },
        };
        const location = {
            search: 'date=1235',
        };
        return (
            <EditPage
                location={location}
                match={match}
            />
        );
    }
}

TimesheetEditPanel.propTypes = {
    timesheetId: PropTypes.number,
    startDate: PropTypes.instanceOf(Date),
    endDate: PropTypes.instanceOf(Date),
};

TimesheetEditPanel.defaultProps = {
    timesheetId: undefined,
    startDate: undefined,
    endDate: undefined,
};

export default (TimesheetEditPanel);
