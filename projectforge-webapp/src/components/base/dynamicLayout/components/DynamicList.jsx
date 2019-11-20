import PropTypes from 'prop-types';
import React from 'react';
import { contentPropType } from '../../../../utilities/propTypes';
import ListElement from '../../../design/list/ListElement';
import { DynamicLayoutContext } from '../context';

function DynamicList(
    {
        listId,
        content,
        positionLabel,
        elementVar,
    },
) {
    const context = React.useContext(DynamicLayoutContext);

    const { data, renderLayout } = context;

    const list = Object.getByString(data, listId) || [];

    return (
        <React.Fragment>
            {list.map(element => (
                <ListElement
                    key={`dynamic-list-${element.number}`}
                    label={`${positionLabel} #${element.number}`}
                    bodyIsOpenInitial={!list.length}
                    renderBody={() => (
                        <DynamicLayoutContext.Provider
                            value={{
                                ...context,
                                data: {
                                    [elementVar]: element,
                                },
                                setData: console.log,
                            }}
                        >
                            {renderLayout(content)}
                        </DynamicLayoutContext.Provider>
                    )}
                />
            ))}
        </React.Fragment>
    );
}

DynamicList.propTypes = {
    content: PropTypes.arrayOf(contentPropType).isRequired,
    elementVar: PropTypes.string.isRequired,
    listId: PropTypes.string.isRequired,
    positionLabel: PropTypes.string.isRequired,
};

DynamicList.defaultProps = {};

export default DynamicList;
