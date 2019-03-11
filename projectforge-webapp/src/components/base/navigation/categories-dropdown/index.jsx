import React from 'react';
import PropTypes from 'prop-types';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faListUl } from '@fortawesome/free-solid-svg-icons';
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
import revisedRandomId from '../../../../utilities/revisedRandomId';
import style from '../Navigation.module.scss';

function countColumnSize(column) {
    return column.reduce((accumulator, currentValue) => accumulator + currentValue.items.length, 0);
}

function CategoriesDropdown({ categories }) {
    // Creating an array of columns for the categories.
    const columns = [
        [], [], [], [],
    ];

    // Add the 'General' category to the first column, so it will always be first.
    columns[0].push(categories[0]);

    // Summarized: Balancing the categories to get less white space.
    categories
    // Remove the first ('general') category.
        .slice(1)
        // Sort the categories by their items size.
        .sort((categoryA, categoryB) => categoryB.items.length - categoryA.items.length)
        // forEach through all categories.
        .forEach(category => columns
        // Compare all columns and get the smallest one.
            .reduce((columnA, columnB) => (countColumnSize(columnA) < countColumnSize(columnB)
                ? columnA : columnB))
            // Add the category to the smallest column.
            .push(category));

    return (
        <UncontrolledDropdown>
            <DropdownToggle nav caret>
                <FontAwesomeIcon icon={faListUl} />
            </DropdownToggle>
            <DropdownMenu className={style.categoryListDropdownMenu}>
                <Container>
                    <Row>
                        {columns.map(chest => (
                            <Col
                                md={3}
                                key={`column-${revisedRandomId()}`}
                                className={style.categoryColumn}
                            >
                                {chest.map(category => (
                                    <Category
                                        category={category}
                                        key={`category-${category.name}`}
                                    />
                                ))}
                            </Col>
                        ))}
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
