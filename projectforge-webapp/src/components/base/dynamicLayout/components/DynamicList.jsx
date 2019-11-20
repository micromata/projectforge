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

    const { data, renderLayout, setData } = context;

    const list = Object.getByString(data, listId) || [];

    return React.useMemo(() => (
        <React.Fragment>
            {list
                .sort((elementA, elementB) => elementA.number - elementB.number)
                .map((element) => {
                    const setElementData = (newData) => {
                        const calculatedNewData = Object
                            .keys(newData)
                            .reduce((accumulator, key) => ({
                                ...accumulator,
                                // Removes the elementVar in the newData.
                                [key.substring(elementVar.length + 1, key.length)]: newData[key],
                            }), {});

                        setData({
                            [listId]: [
                                // Remove the current element
                                ...list.filter(e => e !== element),
                                // Add the current element with changed values
                                {
                                    // Old values from element
                                    ...element,
                                    // New Values from newData.
                                    ...calculatedNewData,
                                },
                            ],
                        });
                    };

                    return (
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
                                        setData: setElementData,
                                    }}
                                >
                                    {renderLayout(content)}
                                </DynamicLayoutContext.Provider>
                            )}
                        />
                    );
                })}
        </React.Fragment>
    ), [list, setData]);
}

DynamicList.propTypes = {
    content: PropTypes.arrayOf(contentPropType).isRequired,
    elementVar: PropTypes.string.isRequired,
    listId: PropTypes.string.isRequired,
    positionLabel: PropTypes.string.isRequired,
};

DynamicList.defaultProps = {};

export default DynamicList;
