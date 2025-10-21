import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React, { useCallback } from 'react';
import { Button } from 'reactstrap';
import { Col, Collapse, Container, Row, UncontrolledTooltip } from '../../../../components/design';
import DiffText from '../../../../components/design/DiffText';
import { getTranslation } from '../../../../utilities/layout';
import style from './History.module.scss';
import useActions from '../../../../actions/useActions';
import { callAction as callActionHandler } from '../../../../actions';
import { evalServiceURL } from '../../../../utilities/rest';

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
            id: masterId,
            attributes,
            userComment,
            timeAgo,
            modifiedAt,
            modifiedByUser,
            diffSummary,
        },
        userAccess,
        translations,
    },
) {
    const [active, setActive] = React.useState(false);

    const callAction = useActions(callActionHandler);

    const editComment = useCallback(() => callAction({
        responseAction: {
            targetType: 'MODAL',
            url: evalServiceURL(`/react/historyEntries/edit/${masterId}`),
        },
    }), [callAction, masterId]);

    const idifiedDate = String.idify(modifiedAt);
    const dateId = `history-date-${idifiedDate}`;

    const { editHistoryComments } = userAccess;

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
                        {diffSummary
                            .map((diff) => (
                                <span
                                    className={style[diff.type]}
                                    key={`history-diff-summary-${masterId}`}
                                >
                                    {`${diff.count} ${diff.operation}`}
                                </span>
                            ))}
                    </span>
                </Col>
                <Col>
                    <span>
                        Felder:
                        {' '}
                        {attributes
                            .map((diff) => diff.displayPropertyName)
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
                    {userComment && (
                        <pre className={style.comment}>{userComment}</pre>
                    )}
                    {editHistoryComments && (
                        <Button
                            type="button"
                            color="link"
                            className={style.editComment}
                            onClick={editComment}
                        >
                            {getTranslation('history.userComment.edit', translations)}
                        </Button>
                    )}
                    <h5>
                        <strong>
                            {getTranslation('changes', translations)}
                            :
                        </strong>
                    </h5>
                    {attributes.map((
                        {
                            operationType: attrOperationType,
                            displayPropertyName,
                            oldValue,
                            newValue,
                        },
                    ) => {
                        let diff;

                        switch (attrOperationType) {
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
                                key={`history-diff-${masterId}`}
                                className={style.detail}
                            >
                                <span className={style[attrOperationType]}>
                                    {getTypeSymbol(attrOperationType)}
                                    {' '}
                                    {displayPropertyName}
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
            displayPropertyName: PropTypes.string,
        })),
        id: PropTypes.number,
        modifiedAt: PropTypes.string,
        modifiedByUser: PropTypes.string,
        diffSummary: PropTypes.arrayOf(PropTypes.shape({
            type: PropTypes.string,
            count: PropTypes.number,
            operation: PropTypes.string,
        })),
        userComment: PropTypes.string,
        modifiedByUserId: PropTypes.number,
        timeAgo: PropTypes.string,
    }).isRequired,
    userAccess: PropTypes.shape({
        editHistoryComments: PropTypes.bool,
    }),
    translations: PropTypes.shape({
        changes: PropTypes.string,
        history: PropTypes.arrayOf(PropTypes.shape({
            userComment: PropTypes.arrayOf(PropTypes.shape({
                edit: PropTypes.string,
            })),
        })),
    }),
};

export default HistoryEntry;
