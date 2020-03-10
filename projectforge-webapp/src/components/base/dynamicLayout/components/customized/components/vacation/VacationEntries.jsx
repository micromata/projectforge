/* eslint-disable react/jsx-indent,indent,max-len */
import React from 'react';
import 'react-rrule-generator/build/styles.css';
import { Col, Row, Table } from 'reactstrap';
import prefix from '../../../../../../../utilities/prefix';
import history from '../../../../../../../utilities/history';
import { DynamicLayoutContext } from '../../../../context';
import style from './Vacation.module.scss';

function handleVacationRowClick(event, id) {
    history.push(`${prefix}vacation/edit/${id}`);
}

function handleLeaveEntryRowClick(event, id) {
    history.push(`${prefix}leaveAccountEntry/edit/${id}`);
}

function VacationEntriesTable(vacationEntries) {
    return (
        <React.Fragment>
            {vacationEntries && vacationEntries.length > 0
                ? (
                    vacationEntries.map((entry, index) => (
                        <tr
                            key={entry.id}
                            className={index === 0 ? style.borderTop : undefined}
                            onClick={event => handleVacationRowClick(event, entry.id)}
                        >
                            <td>{entry.startDateFormatted}</td>
                            <td>{entry.endDateFormatted}</td>
                            <td>{entry.statusString}</td>
                            <td className={style.number}>{entry.workingDaysFormatted}</td>
                            <td>{entry.specialFormatted}</td>
                            <td>{entry.replacement.displayName}</td>
                            <td>{entry.manager.displayName}</td>
                            <td>{entry.vacationModeString}</td>
                            <td>{entry.comment}</td>
                        </tr>
                    ))
                ) : undefined
            }
        </React.Fragment>
    );
}

function LeaveAccountEntriesTable(leaveAccountEntries, translations) {
    return (
        <React.Fragment>
            {leaveAccountEntries && leaveAccountEntries.length > 0
                ? (
                    <Row>
                        <Col sm={12}>
                            <h4>
                                {translations['vacation.leaveAccountEntry.title.heading']}
                            </h4>
                            <Table striped hover>
                                <thead>
                                <tr>
                                    <th>{translations.date}</th>
                                    <th className={style.number}>{translations['vacation.leaveAccountEntry.amount']}</th>
                                    <th className={style.fullWidth}>{translations.description}</th>
                                </tr>
                                </thead>
                                <tbody>
                                {leaveAccountEntries.map(entry => (
                                    <tr
                                        key={entry.id}
                                        onClick={event => handleLeaveEntryRowClick(event, entry.id)}
                                    >
                                        <td>{entry.dateFormatted}</td>
                                        <td className={style.number}>{entry.amount}</td>
                                        <td>{entry.description}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </Table>
                        </Col>
                    </Row>
                ) : undefined
            }
        </React.Fragment>
    );
}

function VacationEntries() {
    const { data, ui } = React.useContext(DynamicLayoutContext);

    const {
        vacationsCurrentYear,
        vacationsPreviousYear,
        leaveAccountEntries,
    } = data.vacations;

    return React.useMemo(
        () => (
            <React.Fragment>
                <Row>
                    <Col sm={12}>
                        <h4>{ui.translations['vacation.title.list']}</h4>
                        <Table striped hover>
                            <thead>
                            <tr>
                                <th>{ui.translations['vacation.startdate']}</th>
                                <th>{ui.translations['vacation.enddate']}</th>
                                <th>{ui.translations['vacation.status']}</th>
                                <th className={style.number}>{ui.translations['vacation.Days']}</th>
                                <th>{ui.translations['vacation.special']}</th>
                                <th>{ui.translations['vacation.replacement']}</th>
                                <th>{ui.translations['vacation.manager']}</th>
                                <th>{ui.translations['vacation.vacationmode']}</th>
                                <th>{ui.translations.comment}</th>
                            </tr>
                            </thead>
                            <tbody>
                            {VacationEntriesTable(vacationsCurrentYear)}
                            {VacationEntriesTable(vacationsPreviousYear)}
                            </tbody>
                        </Table>
                    </Col>
                </Row>
                {LeaveAccountEntriesTable(leaveAccountEntries, ui.translations)}
            </React.Fragment>
        ),
    );
}

VacationEntries.propTypes = {};

VacationEntries.defaultProps = {};

export default VacationEntries;
