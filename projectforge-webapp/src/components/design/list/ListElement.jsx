import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Card, CardBody, CardHeader, Collapse } from '..';
import style from './List.module.scss';

function ListElement({ label, bodyIsOpenInitial, renderBody }) {
    const [bodyIsOpen, setBodyIsOpen] = React.useState(bodyIsOpenInitial);

    return (
        <Card className={classNames(style.listElement, { [style.bodyIsOpen]: bodyIsOpen })}>
            <CardHeader onClick={() => setBodyIsOpen(!bodyIsOpen)} className={style.header}>
                <FontAwesomeIcon icon={faChevronRight} className={style.chevron} />
                {label}
            </CardHeader>
            <Collapse isOpen={bodyIsOpen} mountOnEnter unmountOnExit>
                <CardBody>
                    {renderBody()}
                </CardBody>
            </Collapse>
        </Card>
    );
}

ListElement.propTypes = {
    label: PropTypes.string.isRequired,
    renderBody: PropTypes.func.isRequired,
    bodyIsOpenInitial: PropTypes.bool,
};

ListElement.defaultProps = {
    bodyIsOpenInitial: true,
};

export default ListElement;
