import React from 'react';
import PropTypes from 'prop-types';

import EditPage from '../../page/edit';

function CalendarEntryEditPanel(
    {
        category,
        dbId,
        uid,
        startDate,
        endDate,
        afterEdit,
    },
) {
    const match = {
        params: {
            category,
            id: uid || dbId,
        },
    };
    const search = startDate ? `start=${startDate}&end=${endDate}` : '';
    const location = {
        search,
    };
    return (
        <div>
            <EditPage
                location={location}
                match={match}
                onClose={afterEdit}
            />
        </div>
    );
}

CalendarEntryEditPanel.propTypes = {
    dbId: PropTypes.string,
    uid: PropTypes.string,
    category: PropTypes.string,
    startDate: PropTypes.number, // Epoch seconds
    endDate: PropTypes.number, // Epoch seconds
    afterEdit: PropTypes.func.isRequired,
};

CalendarEntryEditPanel.defaultProps = {
    dbId: undefined,
    uid: undefined,
    category: 'timesheet',
    startDate: undefined,
    endDate: undefined,
};

export default (CalendarEntryEditPanel);
