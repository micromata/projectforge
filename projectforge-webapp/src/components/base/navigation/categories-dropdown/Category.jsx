import React from 'react';
import { categoryPropType } from '../../../../utilities/propTypes';
import style from '../Navigation.module.scss';

function Category({ category, ...props }) {
    return (
        <div {...props}>
            <span className={style.categoryTitle}>{category.name}</span>
            <ul className={style.categoryLinks}>
                {category.items.map(item => (
                    <li className={style.categoryLink}>{item.name}</li>
                ))}
            </ul>
        </div>
    );
}

Category.propTypes = {
    category: categoryPropType.isRequired,
};

export default Category;
