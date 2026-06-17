import React, { useReducer, useEffect, useCallback } from 'react';
import { Alert } from 'reactstrap';
import {
  DndContext,
  DragOverlay,
  useSensor,
  useSensors,
  PointerSensor,
  KeyboardSensor,
  pointerWithin,
  rectIntersection,
} from '@dnd-kit/core';
import { sortableKeyboardCoordinates } from '@dnd-kit/sortable';
import { menuCustomizerReducer, initialState } from './menuCustomizerReducer';
import { useMenuDragDrop } from './useMenuDragDrop';
import * as api from './menuCustomizerApi';
import { cleanMenuForSave } from './menuCustomizerUtils';
import { CustomMenuPanel } from './components/CustomMenuPanel';
import { TemplateMenuPanel } from './components/TemplateMenuPanel';
import { ActionBar } from './components/ActionBar';
import { DragOverlayContent } from './components/DragOverlayContent';
import LoadingContainer from '../design/loading-container';
import styles from './MenuCustomizer.module.scss';

function customCollisionDetection(args: Parameters<typeof pointerWithin>[0]) {
  const pointerCollisions = pointerWithin(args);
  if (pointerCollisions.length > 0) return pointerCollisions;
  return rectIntersection(args);
}

function MenuCustomizer() {
  const [state, dispatch] = useReducer(menuCustomizerReducer, initialState);
  const { handleDragStart, handleDragEnd } = useMenuDragDrop(state, dispatch);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  useEffect(() => {
    dispatch({ type: 'LOAD_START' });
    api.loadMenuData()
      .then(data => dispatch({ type: 'LOAD_SUCCESS', payload: data }))
      .catch(() => dispatch({ type: 'LOAD_FAILURE', payload: 'Error loading menu data. Please try again.' }));
  }, []);

  useEffect(() => {
    if (state.success || state.error) {
      const timer = setTimeout(() => dispatch({ type: 'CLEAR_MESSAGE' }), 3000);
      return () => clearTimeout(timer);
    }
  }, [state.success, state.error]);

  const handleSave = useCallback(async () => {
    dispatch({ type: 'SAVE_START' });
    try {
      await api.saveCustomMenu(cleanMenuForSave(state.customMenu));
      dispatch({ type: 'SAVE_SUCCESS' });
    } catch {
      dispatch({ type: 'SAVE_FAILURE', payload: state.translations.errorSavingMenu || 'Error saving menu.' });
    }
  }, [state.customMenu, state.translations]);

  const handleUndo = useCallback(() => dispatch({ type: 'UNDO' }), []);

  const handleLoadDefault = useCallback(async () => {
    dispatch({ type: 'LOAD_START' });
    try {
      const items = await api.loadDefaultMenu();
      dispatch({ type: 'SET_CUSTOM_MENU', payload: items });
      dispatch({ type: 'SET_SUCCESS', payload: state.translations.loadDefault || 'Default menu loaded' });
    } catch {
      dispatch({ type: 'LOAD_FAILURE', payload: state.translations.errorLoadingMenu || 'Error loading default menu.' });
    }
  }, [state.translations]);

  const handleReset = useCallback(async () => {
    if (!window.confirm(state.translations.confirmReset || 'Are you sure you want to reset your menu to default?')) return;
    dispatch({ type: 'LOAD_START' });
    try {
      await api.resetMenu();
      const data = await api.loadMenuData();
      dispatch({ type: 'LOAD_SUCCESS', payload: data });
      dispatch({ type: 'SET_SUCCESS', payload: state.translations.menuResetSuccessfully || 'Menu reset successfully' });
    } catch {
      dispatch({ type: 'LOAD_FAILURE', payload: state.translations.errorResettingMenu || 'Error resetting menu.' });
    }
  }, [state.translations]);

  const handleAddGroup = useCallback(() => {
    if (!state.newGroupName.trim()) {
      dispatch({ type: 'SET_ERROR', payload: state.translations.groupNameCannotBeEmpty || 'Group name cannot be empty' });
      return;
    }
    dispatch({ type: 'ADD_GROUP', payload: { name: state.newGroupName } });
  }, [state.newGroupName, state.translations]);

  const handleRemoveItem = useCallback((itemId: string, groupId?: string) => {
    dispatch({ type: 'REMOVE_ITEM', payload: { itemId, groupId } });
  }, []);

  if (state.loading) return <LoadingContainer className="" loading>{null}</LoadingContainer>;

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={customCollisionDetection}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
      autoScroll={{ threshold: { x: 0.1, y: 0.2 } }}
    >
      <div className={styles.menuCustomizer}>
        <h2>{state.translations.title || 'Customize Your Menu'}</h2>
        {state.error && <Alert color="danger">{state.error}</Alert>}
        {state.success && <Alert color="success">{state.success}</Alert>}

        <CustomMenuPanel
          customMenu={state.customMenu}
          translations={state.translations}
          showGroupInput={state.showGroupInput}
          newGroupName={state.newGroupName}
          editingGroupId={state.editingGroupId}
          onAddGroup={handleAddGroup}
          onShowGroupInput={(show) => dispatch({ type: 'SET_SHOW_GROUP_INPUT', payload: show })}
          onGroupNameChange={(name) => dispatch({ type: 'SET_NEW_GROUP_NAME', payload: name })}
          onRemoveItem={handleRemoveItem}
          onStartEditGroup={(id) => dispatch({ type: 'SET_EDITING_GROUP', payload: id })}
          onSaveEditGroup={(groupId, name) => {
            if (!name.trim()) {
              dispatch({ type: 'SET_ERROR', payload: state.translations.groupNameCannotBeEmpty || 'Group name cannot be empty' });
              return;
            }
            dispatch({ type: 'RENAME_GROUP', payload: { groupId, name } });
          }}
          onCancelEditGroup={() => dispatch({ type: 'SET_EDITING_GROUP', payload: null })}
        />

        <ActionBar
          translations={state.translations}
          isDirty={state.isDirty}
          onSave={handleSave}
          onUndo={handleUndo}
          onLoadDefault={handleLoadDefault}
          onReset={handleReset}
        />

        <TemplateMenuPanel
          mainMenuStructured={state.mainMenuStructured}
          translations={state.translations}
        />
      </div>

      <DragOverlay>
        {state.draggedItem ? <DragOverlayContent item={state.draggedItem} /> : null}
      </DragOverlay>
    </DndContext>
  );
}

export default MenuCustomizer;
