import { useCallback } from 'react';
import { DragStartEvent, DragEndEvent } from '@dnd-kit/core';
import { MenuCustomizerState, MenuAction, DragData, MenuItem } from './menuCustomizerTypes';
import { getItemId, isGroup } from './menuCustomizerUtils';

export function useMenuDragDrop(
  state: MenuCustomizerState,
  dispatch: React.Dispatch<MenuAction>,
) {
  const handleDragStart = useCallback((event: DragStartEvent) => {
    const data = event.active.data.current as DragData | undefined;
    if (data) {
      dispatch({ type: 'DRAG_START', payload: { id: String(event.active.id), item: data.item } });
    }
  }, [dispatch]);

  const handleDragEnd = useCallback((event: DragEndEvent) => {
    dispatch({ type: 'DRAG_END' });

    const { active, over } = event;
    if (!over) return;

    const activeData = active.data.current as DragData | undefined;
    const overData = over.data.current as DragData | undefined;
    if (!activeData) return;

    // Prevent groups from being dropped into other groups
    if (activeData.type === 'group' && overData?.container !== 'favorites' && overData?.container !== 'template') {
      return;
    }

    const sourceContainer = activeData.container;
    const targetContainer = overData?.container || 'favorites';

    // Template → favorites/group: ADD
    if (sourceContainer === 'template') {
      const targetIndex = getTargetIndex(state.customMenu, targetContainer, over.id as string, overData);
      dispatch({
        type: 'ADD_FROM_TEMPLATE',
        payload: { item: activeData.item, targetContainer, targetIndex },
      });
      return;
    }

    // Same container: REORDER
    if (sourceContainer === targetContainer) {
      const oldIndex = getItemIndex(state.customMenu, sourceContainer, activeData.item);
      const overItem = overData?.item;
      const newIndex = overItem
        ? getItemIndex(state.customMenu, targetContainer, overItem)
        : getTargetIndex(state.customMenu, targetContainer, over.id as string, overData);

      if (oldIndex !== -1 && newIndex !== -1) {
        dispatch({ type: 'REORDER', payload: { container: sourceContainer, oldIndex, newIndex } });
      }
      return;
    }

    // Different containers: MOVE
    const sourceIndex = getItemIndex(state.customMenu, sourceContainer, activeData.item);
    const targetIndex = getTargetIndex(state.customMenu, targetContainer, over.id as string, overData);

    if (sourceIndex !== -1) {
      dispatch({
        type: 'MOVE_TO_CONTAINER',
        payload: { item: activeData.item, sourceContainer, sourceIndex, targetContainer, targetIndex },
      });
    }
  }, [state.customMenu, dispatch]);

  return { handleDragStart, handleDragEnd };
}

function getItemIndex(customMenu: MenuItem[], container: string, item: MenuItem): number {
  const itemId = getItemId(item);
  if (container === 'favorites') {
    return customMenu.findIndex(i => getItemId(i) === itemId);
  }
  // Group container
  const group = customMenu.find(g => getItemId(g) === container);
  if (group?.subMenu) {
    return group.subMenu.findIndex(i => getItemId(i) === itemId);
  }
  return -1;
}

function getTargetIndex(customMenu: MenuItem[], container: string, _overId: string, overData?: DragData): number {
  if (container === 'favorites') {
    if (overData?.item) {
      const idx = customMenu.findIndex(i => getItemId(i) === getItemId(overData.item));
      return idx !== -1 ? idx : customMenu.length;
    }
    return customMenu.length;
  }
  // Group container
  const group = customMenu.find(g => getItemId(g) === container);
  if (group?.subMenu && overData?.item) {
    const idx = group.subMenu.findIndex(i => getItemId(i) === getItemId(overData.item));
    return idx !== -1 ? idx : group.subMenu.length;
  }
  return group?.subMenu?.length || 0;
}
