import React from 'react';
import CalendarPanel from '../panel/CalendarPanel';

class CalendarPage extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return <CalendarPanel />
    }
}

CalendarPage.propTypes = {
};

CalendarPage.defaultProps = {
};

export default CalendarPage;
