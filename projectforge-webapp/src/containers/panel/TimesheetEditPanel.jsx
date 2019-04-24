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
        const search = startDate ? `start=${startDate}&end=${endDate}` : '';
        const location = {
            search,
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
    timesheetId: PropTypes.string,
    startDate: PropTypes.number, // Epoch seconds
    endDate: PropTypes.number, // Epoch seconds
};

TimesheetEditPanel.defaultProps = {
    timesheetId: undefined,
    startDate: undefined,
    endDate: undefined,
};

export default (TimesheetEditPanel);
