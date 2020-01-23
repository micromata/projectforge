/* eslint-disable react/jsx-indent,indent,max-len */
import React from 'react';
import 'react-rrule-generator/build/styles.css';
import { Col, Row, Table } from 'reactstrap';
import { DynamicLayoutContext } from '../../../context';

function VacationEntriesTable(vacationEntries, translations, year) {
    return (
        <React.Fragment>
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
                    <th>{translations['vacation.workingdays']}</th>
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
                        <td>{entry.workingDaysFormatted}</td>
                        <td>{entry.specialFormatted}</td>
                        <td>{entry.replacement.displayName}</td>
                        <td>{entry.manager.displayName}</td>
                        <td>{entry.vacationModeString}</td>
                        <td>{entry.comment}</td>
                    </tr>
                ))}
                </tbody>
            </Table>
        </React.Fragment>
    );
}

function VacationEntries({ values }) {
    const { ui } = React.useContext(DynamicLayoutContext);

    const {
        vacationsCurrentYear,
        vacationPreviousYear,
        yearCurrent,
        yearPrevious,
    } = values;
    const currentVacations = vacationsCurrentYear;
    const prevVacations = vacationPreviousYear;

    return React.useMemo(
        () => (
            <React.Fragment>
                <Row>
                    <Col sm={12}>
                        {!currentVacations || currentVacations.length > 0
                            ? (VacationEntriesTable(currentVacations, ui.translations, yearCurrent)
                            ) : undefined
                        }
                    </Col>
                </Row>
            </React.Fragment>
        ),
    );
}

VacationEntries.propTypes = {};

VacationEntries.defaultProps = {};

export default VacationEntries;
