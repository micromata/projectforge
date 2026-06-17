import React from 'react';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { useDroppable } from '@dnd-kit/core';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus, faFolder, faFloppyDisk, faRotateLeft } from '@fortawesome/free-solid-svg-icons';
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
  isDirty: boolean;
  collapsedGroups: Set<string>;
  onAddGroup: () => void;
  onShowGroupInput: (show: boolean) => void;
  onGroupNameChange: (name: string) => void;
  onRemoveItem: (itemId: string, groupId?: string) => void;
  onStartEditGroup: (groupId: string) => void;
  onSaveEditGroup: (groupId: string, name: string) => void;
  onCancelEditGroup: () => void;
  onToggleGroupCollapse: (groupId: string) => void;
  onSave: () => void;
  onUndo: () => void;
  onLoadDefault: () => void;
  onReset: () => void;
}

export function CustomMenuPanel({
  customMenu, translations, showGroupInput, newGroupName, editingGroupId, isDirty, collapsedGroups,
  onAddGroup, onShowGroupInput, onGroupNameChange,
  onRemoveItem, onStartEditGroup, onSaveEditGroup, onCancelEditGroup, onToggleGroupCollapse,
  onSave, onUndo, onLoadDefault, onReset,
}: Props) {
  const sortableIds = customMenu.map(item =>
    isGroup(item) ? `grp_${getItemId(item)}` : `fav_${getItemId(item)}`
  );

  const { setNodeRef, isOver } = useDroppable({
    id: 'favorites-drop',
    data: { type: 'item', item: { id: 'favorites', title: '' }, container: 'favorites' },
  });

  const itemCount = customMenu.reduce((acc, item) =>
    acc + (isGroup(item) ? (item.subMenu?.length || 0) : 1), 0);
  const groupCount = customMenu.filter(isGroup).length;

  const countLabel = `${itemCount} ${itemCount === 1 ? 'Eintrag' : 'Einträge'}${groupCount > 0 ? ` · ${groupCount} ${groupCount === 1 ? 'Gruppe' : 'Gruppen'}` : ''}`;

  return (
    <div className={styles.panel}>
      <div className={styles.panelHeader}>
        <div className={styles.panelTitleRow}>
          <div>
            <span className={styles.panelTitle}>Dein Menü</span>
            <span className={styles.panelMeta}>{countLabel}</span>
          </div>
          {!showGroupInput ? (
            <button type="button" className={styles.addGroupBtn} onClick={() => onShowGroupInput(true)}>
              <FontAwesomeIcon icon={faPlus} size="xs" />
              Gruppe hinzufügen
            </button>
          ) : (
            <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
              <input
                type="text"
                className={styles.groupEditInput}
                value={newGroupName}
                onChange={(e) => onGroupNameChange(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') onAddGroup(); if (e.key === 'Escape') onShowGroupInput(false); }}
                placeholder={translations.groupName || 'Gruppenname'}
                autoFocus
                style={{ width: 150 }}
              />
              <button type="button" className={styles.btnPrimary} onClick={onAddGroup} style={{ padding: '6px 10px' }}>
                {translations.add || 'Hinzufügen'}
              </button>
              <button type="button" className={styles.btnSecondary} onClick={() => { onShowGroupInput(false); onGroupNameChange(''); }} style={{ padding: '6px 10px' }}>
                {translations.cancel || 'Abbrechen'}
              </button>
            </div>
          )}
        </div>
      </div>

      <SortableContext items={sortableIds} strategy={verticalListSortingStrategy}>
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
                  collapsed={collapsedGroups.has(itemId)}
                  onRemove={onRemoveItem}
                  onStartEdit={onStartEditGroup}
                  onSaveEdit={onSaveEditGroup}
                  onCancelEdit={onCancelEditGroup}
                  onNameChange={onGroupNameChange}
                  onToggleCollapse={onToggleGroupCollapse}
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
              <div className={styles.emptyTitle}>Dein Menü ist noch leer</div>
              <div className={styles.emptyText}>
                Ziehe Einträge aus der Vorlage rechts hierher — oder klicke einen Eintrag an, um ihn hinzuzufügen.
              </div>
            </div>
          )}
        </div>
      </SortableContext>

      <div className={styles.actionButtons}>
        <button type="button" className={styles.btnPrimary} onClick={onSave} disabled={!isDirty}>
          <FontAwesomeIcon icon={faFloppyDisk} size="sm" />
          {translations.saveChanges || 'Änderungen speichern'}
        </button>
        <button type="button" className={styles.btnSecondary} onClick={onUndo} disabled={!isDirty}>
          <FontAwesomeIcon icon={faRotateLeft} size="sm" />
          {translations.undo || 'Rückgängig'}
        </button>
        <button type="button" className={styles.btnWarning} onClick={onLoadDefault}>
          {translations.loadDefault || 'Standard laden'}
        </button>
        <button type="button" className={styles.btnDanger} onClick={onReset}>
          {translations.resetToDefault || 'Standard wiederherstellen'}
        </button>
      </div>
    </div>
  );
}
