import { faListUl } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { menuItemPropType } from '../../../../utilities/propTypes';
import revisedRandomId from '../../../../utilities/revisedRandomId';
import { Col, Container, Dropdown, DropdownMenu, DropdownToggle, Row, } from '../../../design';
import style from '../Navigation.module.scss';
import Category from './Category';

function countColumnSize(column) {
    return column
        .reduce((accumulator, currentValue) => accumulator + currentValue.subMenu.length + 1, 0);
}

function CategoriesDropdown({ categories }) {
    const [open, setOpen] = React.useState(false);

    // Creating an array of columns for the categories.
    const columns = [
        [], [], [], [],
    ];

    // Add the 'Common' category to the first column, so it will always be first.
    columns[0].push(categories[0]);

    // Summarized: Balancing the categories to get less white space.
    categories
    // Remove the first ('common') category.
        .slice(1)
        // Filter out empty categories
        .filter(category => category.subMenu)
        // Sort the categories by their items size.
        .sort((categoryA, categoryB) => categoryB.subMenu.length - categoryA.subMenu.length)
        // forEach through all categories.
        .forEach(category => columns
        // Compare all columns and get the smallest one.
            .reduce((columnA, columnB) => (countColumnSize(columnA) < countColumnSize(columnB)
                ? columnA : columnB))
            // Add the category to the smallest column.
            .push(category));

    return (
        <Dropdown isOpen={open} toggle={() => setOpen(!open)}>
            <DropdownToggle nav caret>
                <FontAwesomeIcon icon={faListUl} />
            </DropdownToggle>
            <DropdownMenu className={style.categoryListDropdownMenu}>
                <Container>
                    <Row>
                        {columns.map(column => (
                            <Col
                                md={3}
                                key={`menu-column-${revisedRandomId()}`}
                                className={style.categoryColumn}
                            >
                                {column.map(category => (
                                    <Category
                                        category={category}
                                        key={`category-${category.title}`}
                                        closeMenu={() => setOpen(false)}
                                    />
                                ))}
                            </Col>
                        ))}
                    </Row>
                </Container>
            </DropdownMenu>
        </Dropdown>
    );
}

CategoriesDropdown.propTypes = {
    categories: PropTypes.arrayOf(menuItemPropType).isRequired,
};

export default CategoriesDropdown;
