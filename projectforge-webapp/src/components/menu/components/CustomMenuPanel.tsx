import React from 'react';
import { SortableContext, rectSortingStrategy } from '@dnd-kit/sortable';
import { useDroppable } from '@dnd-kit/core';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus, faFolder } from '@fortawesome/free-solid-svg-icons';
import { MenuItem, Translations } from '../menuCustomizerTypes';
import { getItemId, isGroup } from '../menuCustomizerUtils';
import { SortableMenuItem } from './SortableMenuItem';
import { SortableGroup } from './SortableGroup';
import styles from '../MenuCustomizer.module.scss';

interface Props {
  customMenu: MenuItem[];
  translations: Translations;
  showGroupInput: boolean;
  newGroupName: string;
  editingGroupId: string | null;
  onAddGroup: () => void;
  onShowGroupInput: (show: boolean) => void;
  onGroupNameChange: (name: string) => void;
  onRemoveItem: (itemId: string, groupId?: string) => void;
  onStartEditGroup: (groupId: string) => void;
  onSaveEditGroup: (groupId: string, name: string) => void;
  onCancelEditGroup: () => void;
}

export function CustomMenuPanel({
  customMenu, translations, showGroupInput, newGroupName, editingGroupId,
  onAddGroup, onShowGroupInput, onGroupNameChange,
  onRemoveItem, onStartEditGroup, onSaveEditGroup, onCancelEditGroup,
}: Props) {
  const sortableIds = customMenu.map(item =>
    isGroup(item) ? `grp_${getItemId(item)}` : `fav_${getItemId(item)}`
  );

  const { setNodeRef, isOver } = useDroppable({
    id: 'favorites-drop',
    data: { type: 'item', item: { id: 'favorites', title: '' }, container: 'favorites' },
  });

  return (
    <div className={styles.panel}>
      <div className={styles.panelHeader}>
        <div className={styles.panelTitleRow}>
          <span className={styles.panelTitle}>{translations.customMenuSection || 'Your Menu'}</span>
          {!showGroupInput ? (
            <button type="button" className={styles.addGroupBtn} onClick={() => onShowGroupInput(true)}>
              <FontAwesomeIcon icon={faPlus} size="xs" />
              {translations.group || 'Group'}
            </button>
          ) : (
            <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
              <input
                type="text"
                className={styles.groupEditInput}
                value={newGroupName}
                onChange={(e) => onGroupNameChange(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') onAddGroup(); if (e.key === 'Escape') onShowGroupInput(false); }}
                placeholder={translations.groupName || 'Group name'}
                autoFocus
                style={{ width: 120 }}
              />
              <button type="button" className={styles.btnPrimary} onClick={onAddGroup} style={{ padding: '6px 10px' }}>
                {translations.add || 'OK'}
              </button>
              <button type="button" className={styles.btnSecondary} onClick={() => { onShowGroupInput(false); onGroupNameChange(''); }} style={{ padding: '6px 10px' }}>
                ×
              </button>
            </div>
          )}
        </div>
      </div>

      <SortableContext items={sortableIds} strategy={rectSortingStrategy}>
        <div ref={setNodeRef} className={`${styles.customMenuBody} ${isOver ? styles.draggingOver : ''}`}>
          {customMenu.length > 0 ? customMenu.map(item => {
            const itemId = getItemId(item);
            if (isGroup(item)) {
              return (
                <SortableGroup
                  key={itemId}
                  item={item}
                  translations={translations}
                  editingGroupId={editingGroupId}
                  newGroupName={newGroupName}
                  onRemove={onRemoveItem}
                  onStartEdit={onStartEditGroup}
                  onSaveEdit={onSaveEditGroup}
                  onCancelEdit={onCancelEditGroup}
                  onNameChange={onGroupNameChange}
                />
              );
            }
            return (
              <SortableMenuItem
                key={itemId}
                item={item}
                container="favorites"
                sortableId={`fav_${itemId}`}
                translations={translations}
                onRemove={onRemoveItem}
              />
            );
          }) : (
            <div className={styles.emptyMenu}>
              <div className={styles.icon}>
                <FontAwesomeIcon icon={faFolder} />
              </div>
              <div className={styles.emptyTitle}>{translations.emptyMenuTitle || 'Your menu is empty'}</div>
              <div className={styles.emptyText}>
                {translations.emptyMenuText || ''}
              </div>
            </div>
          )}
        </div>
      </SortableContext>
    </div>
  );
}
