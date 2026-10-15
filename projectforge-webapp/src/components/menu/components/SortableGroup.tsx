import React from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { SortableContext, rectSortingStrategy } from '@dnd-kit/sortable';
import { useDroppable } from '@dnd-kit/core';
import { CSS } from '@dnd-kit/utilities';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faGripVertical, faFolder, faPen, faTrash } from '@fortawesome/free-solid-svg-icons';
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
    opacity: isDragging ? 0.45 : 1,
  };

  const subItems = item.subMenu || [];
  const groupItemIds = subItems.map(sub => `${groupId}_${getItemId(sub)}`);
  const isEditing = editingGroupId === groupId;

  return (
    <div ref={setNodeRef} style={style} className={styles.group}>
      <div className={styles.groupHeader} {...attributes} {...listeners}>
        <FontAwesomeIcon icon={faGripVertical} className={styles.grip} />
        <FontAwesomeIcon icon={faFolder} className={styles.folderIcon} />

        {isEditing ? (
          <input
            type="text"
            className={styles.groupEditInput}
            value={newGroupName}
            onChange={(e) => onNameChange(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') onSaveEdit(groupId, newGroupName);
              if (e.key === 'Escape') onCancelEdit();
            }}
            onBlur={() => onSaveEdit(groupId, newGroupName)}
            onClick={(e) => e.stopPropagation()}
            onMouseDown={(e) => e.stopPropagation()}
            autoFocus
          />
        ) : (
          <span className={styles.title}>{item.title}</span>
        )}

        <span className={styles.groupCount}>{subItems.length}</span>

        <div className={styles.groupActions}>
          {!isEditing && (
            <button
              type="button"
              onClick={(e) => { e.stopPropagation(); onStartEdit(groupId); }}
              onMouseDown={(e) => e.stopPropagation()}
              title={translations.editGroupName || 'Rename'}
            >
              <FontAwesomeIcon icon={faPen} />
            </button>
          )}
          <button
            type="button"
            className={styles.delete}
            onClick={(e) => { e.stopPropagation(); onRemove(groupId); }}
            onMouseDown={(e) => e.stopPropagation()}
            title={translations.removeGroup || 'Delete group'}
          >
            <FontAwesomeIcon icon={faTrash} />
          </button>
        </div>
      </div>

      <GroupDropBody groupId={groupId} groupItemIds={groupItemIds} subItems={subItems}
        translations={translations} onRemove={onRemove} />
    </div>
  );
}

function GroupDropBody({ groupId, groupItemIds, subItems, translations, onRemove }: {
  groupId: string;
  groupItemIds: string[];
  subItems: MenuItem[];
  translations: Translations;
  onRemove: (itemId: string, groupId?: string) => void;
}) {
  const { setNodeRef, isOver } = useDroppable({
    id: `dropbody_${groupId}`,
    data: { type: 'item', item: { id: groupId, title: '' }, container: groupId } as DragData,
  });

  return (
    <SortableContext id={groupId} items={groupItemIds} strategy={rectSortingStrategy}>
      <div ref={setNodeRef} className={`${styles.groupBody} ${isOver ? styles.draggingOver : ''}`}>
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
            {translations.dropItemsHere || 'Drop items here'}
          </div>
        )}
      </div>
    </SortableContext>
  );
}
