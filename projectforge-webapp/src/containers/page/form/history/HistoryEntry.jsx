import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Col, Collapse, Container, Row, UncontrolledTooltip } from '../../../../components/design';
import DiffText from '../../../../components/design/DiffText';
import { getTranslation } from '../../../../utilities/layout';
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
            attributes,
            timeAgo,
            modifiedAt,
            modifiedByUser,
        },
        translations,
    },
) {
    const [active, setActive] = React.useState(false);
    const diffSummary = {};

    attributes.forEach(({ operation, operationType }) => {
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

    const idifiedDate = String.idify(modifiedAt);
    const dateId = `history-date-${idifiedDate}`;

    return (
        <div
            className={classNames(style.entry, { [style.active]: active })}
            onClick={() => setActive(!active)}
            role="button"
            tabIndex={-1}
            onKeyDown={() => undefined}
        >
            <Row>
                <Col>
                    <FontAwesomeIcon icon={faChevronRight} className={style.icon} />
                    <span className={style.username}>{modifiedByUser}</span>
                </Col>
                <Col>
                    <span className={style.changesAmount}>
                        {Object.keys(diffSummary)
                            .map((diffType) => (
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
                        {attributes
                            .map((diff) => diff.property)
                            .join(', ')}
                    </span>
                </Col>
                <Col>
                    <span className={style.modifiedAt}>
                        <i id={dateId}>{timeAgo}</i>
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
                    <h5>
                        <strong>
                            {getTranslation('changes', translations)}
                            :
                        </strong>
                    </h5>
                    {attributes.map((
                        {
                            operationType,
                            property,
                            oldValue,
                            newValue,
                            id,
                        },
                    ) => {
                        let diff;

                        switch (operationType) {
                            case 'Insert':
                                diff = `${newValue}`;
                                break;
                            case 'Update':
                                diff = (
                                    <DiffText
                                        oldValue={oldValue}
                                        newValue={newValue}
                                    />
                                );
                                break;
                            case 'Delete':
                                diff = `${oldValue}`;
                                break;
                            default:
                                diff = 'No change';
                        }

                        return (
                            <span
                                key={`history-diff-at-${id}`}
                                className={style.detail}
                            >
                                <span className={style[operationType]}>
                                    {getTypeSymbol(operationType)}
                                    {' '}
                                    {property}
                                    {': '}
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
        attributes: PropTypes.arrayOf(PropTypes.shape({
            id: PropTypes.number,
            newValue: PropTypes.string,
            oldValue: PropTypes.string,
            operation: PropTypes.string,
            operationType: PropTypes.string,
            property: PropTypes.string,
        })),
        id: PropTypes.number,
        modifiedAt: PropTypes.string,
        modifiedByUser: PropTypes.string,
        modifiedByUserId: PropTypes.number,
        operation: PropTypes.string,
        operationType: PropTypes.string,
        timeAgo: PropTypes.string,
    }).isRequired,
    translations: PropTypes.shape({
        changes: PropTypes.string,
    }),
};

HistoryEntry.defaultProps = {
    translations: undefined,
};

export default HistoryEntry;
