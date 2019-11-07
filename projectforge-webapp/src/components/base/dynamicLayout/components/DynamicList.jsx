import PropTypes from 'prop-types';
import React from 'react';
import { contentPropType } from '../../../../utilities/propTypes';
import { Card, CardBody } from '../../../design';
import { DynamicLayoutContext } from '../context';

function DynamicList({ listId, content }) {
    const { data, renderLayout } = React.useContext(DynamicLayoutContext);

    return (
        <React.Fragment>
            {(data[listId] || []).map(({ number }) => (
                <Card key={`dynamic-list-card-${number}`}>
                    <CardBody>
                        {renderLayout(content)}
                    </CardBody>
                </Card>
            ))}
        </React.Fragment>
    );
}

DynamicList.propTypes = {
    content: PropTypes.arrayOf(contentPropType).isRequired,
    listId: PropTypes.string.isRequired,
};

DynamicList.defaultProps = {};

export default DynamicList;
