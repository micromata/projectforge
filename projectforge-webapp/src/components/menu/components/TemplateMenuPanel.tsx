import React, { useMemo } from 'react';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faMagnifyingGlass, faChevronDown, faCheck } from '@fortawesome/free-solid-svg-icons';
import { MenuCategory, MenuItem, Translations } from '../menuCustomizerTypes';
import { getItemId, isDuplicate } from '../menuCustomizerUtils';
import { TemplateChip } from './TemplateChip';
import styles from '../MenuCustomizer.module.scss';

interface Props {
  mainMenuStructured: MenuCategory[];
  customMenu: MenuItem[];
  translations: Translations;
  search: string;
  collapsedCategories: Set<string>;
  onSearchChange: (value: string) => void;
  onToggleCategory: (categoryId: string) => void;
  onClickAdd: (item: MenuItem) => void;
}

export function TemplateMenuPanel({
  mainMenuStructured, customMenu, translations, search, collapsedCategories,
  onSearchChange, onToggleCategory, onClickAdd,
}: Props) {
  const allTemplateIds = useMemo(
    () => mainMenuStructured.flatMap(cat => (cat.subMenu || []).map(item => `tpl_${getItemId(item)}`)),
    [mainMenuStructured]
  );

  const filteredCategories = useMemo(() => {
    if (!search.trim()) return mainMenuStructured;
    const term = search.toLowerCase();
    return mainMenuStructured
      .map(cat => ({
        ...cat,
        subMenu: (cat.subMenu || []).filter(item => item.title.toLowerCase().includes(term)),
      }))
      .filter(cat => cat.subMenu && cat.subMenu.length > 0);
  }, [mainMenuStructured, search]);

  return (
    <div className={styles.panel}>
      <div className={styles.panelHeader}>
        <div className={styles.panelTitleRow}>
          <div>
            <span className={styles.panelTitle}>Verfügbare Einträge</span>
            <span className={styles.panelMeta}>Vorlage</span>
          </div>
        </div>
        <div className={styles.searchWrap}>
          <FontAwesomeIcon icon={faMagnifyingGlass} className={styles.searchIcon} />
          <input
            type="text"
            value={search}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Suchen..."
          />
        </div>
        <p className={styles.searchHint}>
          Klicke einen Eintrag an oder ziehe ihn nach links in dein Menü.
        </p>
      </div>

      <div className={styles.templateMenuBody}>
        <SortableContext items={allTemplateIds} strategy={verticalListSortingStrategy}>
          {filteredCategories.length > 0 ? filteredCategories.map(category => {
            const catId = category.id;
            const isCollapsed = collapsedCategories.has(catId);
            const items = category.subMenu || [];
            const addedCount = items.filter(item => isDuplicate(customMenu, getItemId(item))).length;

            return (
              <div key={catId} className={styles.category}>
                <button
                  type="button"
                  className={`${styles.categoryTitle} ${isCollapsed ? styles.collapsed : ''}`}
                  onClick={() => onToggleCategory(catId)}
                >
                  <FontAwesomeIcon
                    icon={faChevronDown}
                    className={`${styles.chevron} ${isCollapsed ? styles.chevronCollapsed : ''}`}
                    size="xs"
                  />
                  <span className={styles.name}>{category.title}</span>
                  {addedCount > 0 ? (
                    <span className={styles.categoryBadgeAdded}>
                      <FontAwesomeIcon icon={faCheck} size="xs" /> {addedCount}/{items.length}
                    </span>
                  ) : (
                    <span className={styles.categoryBadge}>{items.length}</span>
                  )}
                </button>
                {!isCollapsed && (
                  <div className={styles.categoryLinks}>
                    {items.map(item => (
                      <TemplateChip
                        key={getItemId(item)}
                        item={item}
                        isAdded={isDuplicate(customMenu, getItemId(item))}
                        onClickAdd={onClickAdd}
                      />
                    ))}
                  </div>
                )}
              </div>
            );
          }) : (
            <div className={styles.noResults}>Keine Einträge gefunden</div>
          )}
        </SortableContext>
      </div>
    </div>
  );
}
