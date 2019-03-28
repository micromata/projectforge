import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Col, Collapse, Container, Row, UncontrolledTooltip, } from '../../../../components/design';
import DiffText from '../../../../components/design/DiffText';
import format, { TEXT_SINCE_TIMESTAMP } from '../../../../utilities/format';
import revisedRandomId from '../../../../utilities/revisedRandomId';
import style from './History.module.scss';

function getTypeSymbol(type) {
    switch (type) {
        case 'Insert':
            return '+';
        case 'Update':
            return '~';
        case 'Delete':
            return '-';
        default:
            return '';
    }
}

function HistoryEntry(
    {
        entry: {
            'diff-entries': entries,
            'modified-at': date,
            'modified-by-user': user,
        },
    },
) {
    const [active, setActive] = React.useState(false);
    const diffSummary = {};

    entries.forEach(({ operation: diffOperation, 'operation-type': diffType }) => {
        let diff = diffSummary[diffType];

        if (!diff) {
            diff = {
                operation: diffOperation,
                amount: 1,
            };
        } else {
            diff.amount += 1;
        }

        diffSummary[diffType] = diff;
    });

    const dateId = `history-date-${revisedRandomId()}`;

    return (
        <div
            className={classNames(style.entry, { [style.active]: active })}
            onClick={() => setActive(!active)}
            role="button"
            tabIndex={-1}
            onKeyDown={() => {
            }}
        >
            <Row>
                <Col>
                    <FontAwesomeIcon icon={faChevronRight} className={style.icon} />
                    <span className={style.username}>{user}</span>
                </Col>
                <Col>
                    <span className={style.changesAmount}>
                        {Object.keys(diffSummary)
                            .map(diffType => (
                                <span
                                    className={style[diffType]}
                                    key={`history-diff-at-${date}-${diffType}`}
                                >
                                    {`${diffSummary[diffType].amount} ${diffSummary[diffType].operation}`}
                                </span>
                            ))}
                    </span>
                </Col>
                <Col>
                    <span>
                        Felder:
                        {' '}
                        {entries
                            .map(diff => diff.property)
                            .join(', ')}
                    </span>
                </Col>
                <Col>
                    <span className={style.modifiedAt}>
                        <i id={dateId}>{format(TEXT_SINCE_TIMESTAMP, date)}</i>
                        <UncontrolledTooltip
                            placement="left"
                            target={dateId}
                        >
                            {date}
                        </UncontrolledTooltip>
                    </span>
                </Col>
            </Row>
            <Collapse isOpen={active}>
                <Container fluid className={style.details}>
                    <h5><strong>[Änderungen]:</strong></h5>
                    {entries.map((
                        {
                            'operation-type': operationType,
                            property,
                            'old-value': oldValue,
                            'new-value': newValue,
                        },
                    ) => {
                        let diff;

                        switch (operationType) {
                            case 'Insert':
                                diff = ` ${property}: ${newValue}`;
                                break;
                            case 'Update':
                                diff = (
                                    <DiffText
                                        oldValue={`WAS ${oldValue}`}
                                        id={`history-tooltip-${revisedRandomId()}`}
                                    >
                                        {` ${property}: ${newValue}`}
                                    </DiffText>
                                );
                                break;
                            case 'Delete':
                                diff = ` ${property}: ${oldValue}`;
                                break;
                            default:
                                diff = ' No change';
                        }

                        return (
                            <span
                                key={`history-diff-at-${date}-details-${property}`}
                                className={style.detail}
                            >
                                <span className={style[operationType]}>
                                    {getTypeSymbol(operationType)}
                                </span>
                                {diff}
                            </span>
                        );
                    })}
                </Container>
            </Collapse>
        </div>
    );
}

HistoryEntry.propTypes = {
    entry: PropTypes.shape({
        'diff-entries': PropTypes.arrayOf(PropTypes.shape({
            'new-value': PropTypes.string,
            'old-value': PropTypes.string,
            operation: PropTypes.string,
            'operation-type': PropTypes.string,
            property: PropTypes.string,
        })),
        'modified-at': PropTypes.string,
        'modified-by-user': PropTypes.string,
        'modified-by-user-id': PropTypes.string,
        operation: PropTypes.string,
        'operation-type': PropTypes.string,
    }).isRequired,
};

export default HistoryEntry;
