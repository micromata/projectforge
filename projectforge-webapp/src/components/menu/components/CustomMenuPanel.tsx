import React from 'react';
import { Card, CardHeader, CardBody, Button } from 'reactstrap';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { useDroppable } from '@dnd-kit/core';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus } from '@fortawesome/free-solid-svg-icons';
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
    <div className={styles.customMenuSection}>
      <Card>
        <CardHeader>
          {translations.customMenuSection || 'Your Custom Menu'}
          <div className={styles.headerActions}>
            {!showGroupInput ? (
              <Button color="primary" size="sm" onClick={() => onShowGroupInput(true)}
                title={translations.addGroup || 'Add a new group'}>
                <FontAwesomeIcon icon={faPlus} />{' '}
                <span>{translations.addGroup || 'Add Group'}</span>
              </Button>
            ) : (
              <div className={styles.groupForm}>
                <input
                  type="text"
                  className={styles.groupNameInput}
                  value={newGroupName}
                  onChange={(e) => onGroupNameChange(e.target.value)}
                  placeholder={translations.groupName || 'Group name'}
                  autoFocus
                />
                <Button color="primary" size="sm" onClick={onAddGroup}>
                  {translations.add || 'Add'}
                </Button>
                <Button color="secondary" size="sm" onClick={() => { onShowGroupInput(false); onGroupNameChange(''); }}>
                  {translations.cancel || 'Cancel'}
                </Button>
              </div>
            )}
          </div>
        </CardHeader>
        <CardBody>
          <SortableContext items={sortableIds} strategy={verticalListSortingStrategy}>
            <div ref={setNodeRef} className={`${styles.horizontalMenuList} ${isOver ? styles.draggingOver : ''}`}>
              {customMenu.map(item => {
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
              })}
              {customMenu.length === 0 && (
                <div className={styles.emptyMenu}>
                  <p>{translations.dragItemsHere || 'Drag items from template below'}</p>
                </div>
              )}
            </div>
          </SortableContext>
        </CardBody>
      </Card>
    </div>
  );
}
