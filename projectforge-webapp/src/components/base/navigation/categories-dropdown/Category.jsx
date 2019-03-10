import React, { Component } from 'react';
import classNames from 'classnames';
import { Collapse } from '../../../design';
import { categoryPropType } from '../../../../utilities/propTypes';
import style from '../Navigation.module.scss';

class Category extends Component {
    constructor(props) {
        super(props);

        this.state = {
            collapse: false,
            viewportWidth: 0,
        };

        this.handleWindowResize = this.handleWindowResize.bind(this);
        this.toggle = this.toggle.bind(this);
    }

    componentDidMount() {
        this.handleWindowResize();
        window.addEventListener('resize', this.handleWindowResize);
    }

    componentWillUnmount() {
        window.removeEventListener('resize', this.handleWindowResize);
    }

    handleWindowResize() {
        const { innerWidth: viewportWidth } = window;


        let collapse = false;

        if (viewportWidth > 735) {
            collapse = true;
        }

        this.setState({
            viewportWidth,
            collapse,
        });
    }

    toggle(event) {
        event.preventDefault();

        const { collapse, viewportWidth } = this.state;

        if (viewportWidth > 735) {
            return;
        }

        this.setState({
            collapse: !collapse,
        });
    }

    render() {
        const { category, className, ...props } = this.props;
        const { collapse } = this.state;

        return (
            <div className={classNames(style.categoryContainer, className)} {...props}>
                <div
                    className={style.categoryTitle}
                    onClick={this.toggle}
                    role="presentation"
                >
                    {category.name}
                </div>
                <Collapse isOpen={collapse}>
                    <ul className={style.categoryLinks}>
                        {category.items.map(item => (
                            <li
                                className={style.categoryLink}
                                key={`category-link-${category.name}-${item.name}`}
                            >
                                {item.name}
                            </li>
                        ))}
                    </ul>
                </Collapse>
            </div>
        );
    }
}

Category.propTypes = {
    category: categoryPropType.isRequired,
};

export default Category;
