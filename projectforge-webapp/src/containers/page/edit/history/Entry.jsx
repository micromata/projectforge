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
            diffEntries,
            modifiedAt,
            modifiedByUser,
        },
    },
) {
    const [active, setActive] = React.useState(false);
    const diffSummary = {};

    diffEntries.forEach(({ operation, operationType }) => {
        let diff = diffSummary[operationType];

        if (!diff) {
            diff = {
                operation,
                amount: 1,
            };
        } else {
            diff.amount += 1;
        }

        diffSummary[operationType] = diff;
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
                    <span className={style.username}>{modifiedByUser}</span>
                </Col>
                <Col>
                    <span className={style.changesAmount}>
                        {Object.keys(diffSummary)
                            .map(diffType => (
                                <span
                                    className={style[diffType]}
                                    key={`history-diff-at-${modifiedAt}-${diffType}`}
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
                        {diffEntries
                            .map(diff => diff.property)
                            .join(', ')}
                    </span>
                </Col>
                <Col>
                    <span className={style.modifiedAt}>
                        <i id={dateId}>{format(TEXT_SINCE_TIMESTAMP, modifiedAt)}</i>
                        <UncontrolledTooltip
                            placement="left"
                            target={dateId}
                        >
                            {modifiedAt}
                        </UncontrolledTooltip>
                    </span>
                </Col>
            </Row>
            <Collapse isOpen={active}>
                <Container fluid className={style.details}>
                    <h5><strong>[Änderungen]:</strong></h5>
                    {diffEntries.map((
                        {
                            operationType,
                            property,
                            oldValue,
                            newValue,
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
                                key={`history-diff-at-${modifiedAt}-details-${property}`}
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
        diffEntries: PropTypes.arrayOf(PropTypes.shape({
            newValue: PropTypes.string,
            oldValue: PropTypes.string,
            operation: PropTypes.string,
            operationType: PropTypes.string,
            property: PropTypes.string,
        })),
        modifiedAt: PropTypes.string,
        modifiedByUser: PropTypes.string,
        modifiedByUserId: PropTypes.string,
        operation: PropTypes.string,
        operationType: PropTypes.string,
    }).isRequired,
};

export default HistoryEntry;
