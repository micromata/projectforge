import React from 'react';
import { Card, CardHeader, CardBody } from 'reactstrap';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { MenuCategory, Translations } from '../menuCustomizerTypes';
import { getItemId } from '../menuCustomizerUtils';
import { TemplateItem } from './TemplateItem';
import styles from '../MenuCustomizer.module.scss';

interface Props {
  mainMenuStructured: MenuCategory[];
  translations: Translations;
}

export function TemplateMenuPanel({ mainMenuStructured, translations }: Props) {
  const allTemplateIds = mainMenuStructured.flatMap(
    cat => (cat.subMenu || []).map(item => `tpl_${getItemId(item)}`)
  );

  return (
    <div className={styles.templateMenuSection}>
      <Card>
        <CardHeader>{translations.templateMenuSection || 'Available Menu Items (Template)'}</CardHeader>
        <CardBody className={styles.templateMenuBody}>
          <SortableContext items={allTemplateIds} strategy={verticalListSortingStrategy}>
            {mainMenuStructured.map(category => (
              <div key={category.id} className={styles.categoryColumn}>
                <div className={styles.categoryContainer}>
                  <button type="button" className={styles.categoryTitle}>
                    {category.title}
                  </button>
                  <ul className={styles.categoryLinks}>
                    {category.subMenu && category.subMenu.map(item => (
                      <li key={getItemId(item)} className={styles.categoryLink}>
                        <TemplateItem item={item} />
                      </li>
                    ))}
                    {(!category.subMenu || category.subMenu.length === 0) && (
                      <li className={styles.categoryLink}>
                        <div className={styles.emptyGroup}>
                          <p>{translations.noItemsInCategory || 'No items in this category'}</p>
                        </div>
                      </li>
                    )}
                  </ul>
                </div>
              </div>
            ))}
          </SortableContext>
        </CardBody>
      </Card>
    </div>
  );
}
