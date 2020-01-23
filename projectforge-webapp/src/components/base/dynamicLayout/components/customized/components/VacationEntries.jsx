/* eslint-disable react/jsx-indent,indent,max-len */
import React from 'react';
import 'react-rrule-generator/build/styles.css';
import { Col, Row, Table } from 'reactstrap';
import { DynamicLayoutContext } from '../../../context';
import style from './Vacation.module.scss';

function VacationEntriesTable(vacationEntries, translations, year) {
    return (
        <React.Fragment>
            {vacationEntries && vacationEntries.length > 0
                ? (
                    <Row>
                        <Col sm={12}>
                            <h4>
                                {translations['vacation.title.list']}
                                {' '}
                                {year}
                            </h4>
                            <Table striped>
                                <thead>
                                <tr>
                                    <th>{translations['vacation.startdate']}</th>
                                    <th>{translations['vacation.enddate']}</th>
                                    <th>{translations['vacation.status']}</th>
                                    <th className={style.number}>{translations['vacation.Days']}</th>
                                    <th>{translations['vacation.special']}</th>
                                    <th>{translations['vacation.replacement']}</th>
                                    <th>{translations['vacation.manager']}</th>
                                    <th>{translations['vacation.vacationmode']}</th>
                                    <th>{translations.comment}</th>
                                </tr>
                                </thead>
                                <tbody>
                                {vacationEntries.map(entry => (
                                    <tr key={entry.id}>
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
                            <Table striped>
                                <thead>
                                <tr>
                                    <th>{translations.date}</th>
                                    <th className={style.number}>{translations['vacation.leaveAccountEntry.amount']}</th>
                                    <th className={style.fullWidth}>{translations.description}</th>
                                </tr>
                                </thead>
                                <tbody>
                                {leaveAccountEntries.map(entry => (
                                    <tr key={entry.id}>
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

function VacationEntries({ values }) {
    const { ui } = React.useContext(DynamicLayoutContext);

    const {
        vacationsCurrentYear,
        vacationsPreviousYear,
        leaveAccountEntries,
        yearCurrent,
        yearPrevious,
    } = values;

    return React.useMemo(
        () => (
            <React.Fragment>
                {VacationEntriesTable(vacationsCurrentYear, ui.translations, yearCurrent)}
                {VacationEntriesTable(vacationsPreviousYear, ui.translations, yearPrevious)}
                {LeaveAccountEntriesTable(leaveAccountEntries, ui.translations)}
            </React.Fragment>
        ),
    );
}

VacationEntries.propTypes = {};

VacationEntries.defaultProps = {};

export default VacationEntries;
