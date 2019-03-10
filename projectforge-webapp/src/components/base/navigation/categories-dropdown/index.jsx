import React from 'react';
import PropTypes from 'prop-types';
import {
    Col,
    Container,
    DropdownMenu,
    DropdownToggle,
    Row,
    UncontrolledDropdown,
} from '../../../design';
import Category from './Category';
import { categoryPropType } from '../../../../utilities/propTypes';

function countColumnSize(column) {
    return column.reduce((accumulator, currentValue) => accumulator + currentValue.items.length, 0);
}

function CategoriesDropdown({ categories }) {
    const columns = [
        [], [], [], [],
    ];

    columns[0].push(categories[0]);

    categories
        .slice(1)
        .sort((categoryA, categoryB) => categoryB.items.length - categoryA.items.length)
        .forEach(category => columns
            .reduce((columnA, columnB) => (countColumnSize(columnA) < countColumnSize(columnB)
                ? columnA : columnB))
            .push(category));

    // TODO: ADD STYLE FOR DROPDOWN MENU
    return (
        <UncontrolledDropdown>
            <DropdownToggle nav caret>
                [Categories]
            </DropdownToggle>
            <DropdownMenu style={{ width: '960px' }}>
                <Container>
                    <Row>
                        {
                            columns.map(chest => (
                                <Col md={3}>
                                    {chest.map(category => (
                                        <Category category={category} />
                                    ))}
                                </Col>
                            ))
                        }
                    </Row>
                </Container>
            </DropdownMenu>
        </UncontrolledDropdown>
    );
}

CategoriesDropdown.propTypes = {
    categories: PropTypes.arrayOf(categoryPropType).isRequired,
};

export default CategoriesDropdown;
