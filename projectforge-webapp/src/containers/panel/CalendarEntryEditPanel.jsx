import React from 'react';
import PropTypes from 'prop-types';

import EditPage from '../page/edit/index';

function CalendarEntryEditPanel(
    {
        category,
        dbId,
        startDate,
        endDate,
        afterEdit,
    },
) {
    const match = {
        params: {
            category,
            id: dbId,
        },
    };
    const search = startDate ? `start=${startDate}&end=${endDate}` : '';
    const location = {
        search,
    };
    return (
        <div>
            time slot select (uses selection from previous).
            <EditPage
                location={location}
                match={match}
                afterEdit={afterEdit}
            />
        </div>
    );
}

CalendarEntryEditPanel.propTypes = {
    dbId: PropTypes.string,
    category: PropTypes.string,
    startDate: PropTypes.number, // Epoch seconds
    endDate: PropTypes.number, // Epoch seconds
    afterEdit: PropTypes.func.isRequired,
};

CalendarEntryEditPanel.defaultProps = {
    dbId: undefined,
    category: 'timesheet',
    startDate: undefined,
    endDate: undefined,
};

export default (CalendarEntryEditPanel);
