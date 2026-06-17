import React from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { useDroppable } from '@dnd-kit/core';
import { CSS } from '@dnd-kit/utilities';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEllipsisV, faFolder, faPencilAlt, faSave, faTrash, faUndo } from '@fortawesome/free-solid-svg-icons';
import { Button } from 'reactstrap';
import { MenuItem, DragData, Translations } from '../menuCustomizerTypes';
import { getItemId } from '../menuCustomizerUtils';
import { SortableMenuItem } from './SortableMenuItem';
import styles from '../MenuCustomizer.module.scss';

interface Props {
  item: MenuItem;
  translations: Translations;
  editingGroupId: string | null;
  newGroupName: string;
  onRemove: (itemId: string, groupId?: string) => void;
  onStartEdit: (groupId: string) => void;
  onSaveEdit: (groupId: string, name: string) => void;
  onCancelEdit: () => void;
  onNameChange: (name: string) => void;
}

export function SortableGroup({
  item, translations, editingGroupId, newGroupName,
  onRemove, onStartEdit, onSaveEdit, onCancelEdit, onNameChange,
}: Props) {
  const groupId = getItemId(item);
  const sortableId = `grp_${groupId}`;
  const data: DragData = { type: 'group', item, container: 'favorites' };

  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: sortableId, data });

  const style: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition: isDragging ? 'none' : (transition || undefined),
    opacity: isDragging ? 0.4 : 1,
  };

  const subItems = item.subMenu || [];
  const groupItemIds = subItems.map(sub => `${groupId}_${getItemId(sub)}`);

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`${styles.menuItem} ${styles.groupItem} ${isDragging ? styles.dragging : ''}`}
    >
      <div className={styles.menuItemContent} {...attributes} {...listeners}>
        <FontAwesomeIcon icon={faEllipsisV} className={styles.dragHandle} />
        <FontAwesomeIcon icon={faFolder} className={styles.folderIcon} />
        {editingGroupId === groupId ? (
          <div className={styles.groupEditForm}>
            <input
              type="text"
              className={styles.groupNameInput}
              value={newGroupName}
              onChange={(e) => onNameChange(e.target.value)}
              autoFocus
            />
            <Button color="primary" size="sm" className={styles.saveGroupButton}
              onClick={(e) => { e.stopPropagation(); onSaveEdit(groupId, newGroupName); }}
              onMouseDown={(e) => e.stopPropagation()}
            >
              <FontAwesomeIcon icon={faSave} />
            </Button>
            <Button color="secondary" size="sm"
              onClick={(e) => { e.stopPropagation(); onCancelEdit(); }}
              onMouseDown={(e) => e.stopPropagation()}
            >
              <FontAwesomeIcon icon={faUndo} />
            </Button>
          </div>
        ) : (
          <>
            <span className={styles.itemTitle}>{item.title}</span>
            <Button color="link" className={styles.actionButton}
              onClick={(e) => { e.stopPropagation(); onStartEdit(groupId); }}
              onMouseDown={(e) => e.stopPropagation()}
              title={translations.editGroupName || 'Edit group name'}
            >
              <FontAwesomeIcon icon={faPencilAlt} />
            </Button>
            <Button color="link" className={styles.actionButton}
              onClick={(e) => { e.stopPropagation(); onRemove(groupId); }}
              onMouseDown={(e) => e.stopPropagation()}
              title={translations.removeGroup || 'Remove group'}
            >
              <FontAwesomeIcon icon={faTrash} />
            </Button>
          </>
        )}
      </div>

      <GroupDropArea groupId={groupId} groupItemIds={groupItemIds} subItems={subItems}
        translations={translations} onRemove={onRemove} />
    </div>
  );
}

function GroupDropArea({ groupId, groupItemIds, subItems, translations, onRemove }: {
  groupId: string;
  groupItemIds: string[];
  subItems: MenuItem[];
  translations: Translations;
  onRemove: (itemId: string, groupId?: string) => void;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: `drop_${groupId}`, data: { type: 'item', item: { id: groupId, title: '' }, container: groupId } as DragData });

  return (
    <SortableContext id={groupId} items={groupItemIds} strategy={verticalListSortingStrategy}>
      <div ref={setNodeRef} className={`${styles.groupContent} ${isOver ? styles.draggingOver : ''}`}>
        {subItems.length > 0 ? subItems.map(sub => (
          <SortableMenuItem
            key={getItemId(sub)}
            item={sub}
            container={groupId}
            sortableId={`${groupId}_${getItemId(sub)}`}
            translations={translations}
            onRemove={onRemove}
          />
        )) : (
          <div className={styles.emptyGroup}>
            <p>{translations.dropItemsHere || 'Drop items here'}</p>
          </div>
        )}
      </div>
    </SortableContext>
  );
}
