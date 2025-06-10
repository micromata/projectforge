/* eslint-disable */
import React, { useState, useEffect } from 'react';
import { Card, CardHeader, CardBody, Button, Alert } from 'reactstrap';
import {
    DndContext,
    DragOverlay,
    useSensor,
    useSensors,
    PointerSensor,
    KeyboardSensor,
    closestCenter,
    closestCorners,
    DragStartEvent,
    DragEndEvent,
    DragOverEvent,
    useDroppable,
} from '@dnd-kit/core';
import {
    SortableContext,
    sortableKeyboardCoordinates,
    verticalListSortingStrategy,
    useSortable,
    arrayMove,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus, faArrowDown, faArrowUp, faMinus, faEllipsisV, faPencilAlt, faSave, faUndo, faTrash, faFolder } from '@fortawesome/free-solid-svg-icons';
import { baseRestURL, handleHTTPErrors } from '../../utilities/rest';
import LoadingContainer from '../design/loading-container';
import styles from './MenuCustomizer.module.scss';

/**
 * Component that allows users to customize their menu via drag and drop.
 * Allows to:
 * - Drag items from main menu to favorites
 * - Drag items around within favorites menu
 * - Create groups and add items to groups
 * - Save the customized menu
 */
function MenuCustomizer() {
    const [loading, setLoading] = useState(true);
    const [menuItems, setMenuItems] = useState({ mainMenu: [], favoritesMenu: [] });
    const [customMenu, setCustomMenu] = useState([]);
    const [newGroupName, setNewGroupName] = useState('');
    const [editingGroup, setEditingGroup] = useState(null);
    const [showGroupInput, setShowGroupInput] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const [activeId, setActiveId] = useState(null);
    const [overId, setOverId] = useState(null);
    const [draggedItem, setDraggedItem] = useState(null);
    
    const sensors = useSensors(
        useSensor(PointerSensor, {
            activationConstraint: {
                distance: 8,
            },
        }),
        useSensor(KeyboardSensor, {
            coordinateGetter: sortableKeyboardCoordinates,
        })
    );

    const loadMenuData = () => {
        setLoading(true);
        // Direct URL call to prevent double URL issue
        fetch(`${baseRestURL}/menu`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then((json) => {
                // Keep original menu structure for rendering categories
                const menuStructure = json.mainMenu.menuItems || [];

                // Also create a flattened list for internal operations
                const flattenMenuItems = (items) => {
                    let result = [];
                    if (!items) return result;

                    items.forEach((item) => {
                        result.push(item);
                        if (item.subMenu && item.subMenu.length > 0) {
                            result = result.concat(flattenMenuItems(item.subMenu));
                        }
                    });
                    return result;
                };

                const allMenuItems = flattenMenuItems(menuStructure);

                console.log('📥 Loaded menu data:');
                console.log('  - Main menu items:', allMenuItems.length);
                console.log('  - Favorites menu items:', (json.favoritesMenu.menuItems || []).length);
                console.log('  - Favorites items:', (json.favoritesMenu.menuItems || []).map(item => ({ id: (item.id || item.key), title: item.title })));
                
                setMenuItems({
                    // Store both the structured and flattened menu items
                    mainMenu: allMenuItems || [],
                    mainMenuStructured: menuStructure || [],
                    favoritesMenu: json.favoritesMenu.menuItems || [],
                });
                setCustomMenu(json.favoritesMenu.menuItems || []);
                setLoading(false);
            })
            .catch((err) => {
                // eslint-disable-next-line no-console
                console.error('Error loading menu data:', err);
                setError('Error loading menu data. Please try again.');
                setLoading(false);
            });
    };

    useEffect(() => {
        loadMenuData();
    }, []);

    useEffect(() => {
        console.log('📋 Custom menu changed:', customMenu.length, 'items:', customMenu.map(item => item.title));
    }, [customMenu]);

    const handleDragStart = (event) => {
        console.log('🚀 Drag Start:', event.active.id, event.active.data.current);
        setActiveId(event.active.id);
        
        // Store the dragged item data for the overlay
        const activeData = event.active.data.current;
        if (activeData && activeData.item) {
            setDraggedItem(activeData.item);
        } else {
            setDraggedItem(null);
        }
        
        // Store current scroll position
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        const scrollLeft = window.pageXOffset || document.documentElement.scrollLeft;
        
        // Prevent scrolling during drag
        window.dragStartScrollPosition = { top: scrollTop, left: scrollLeft };
        
        // Completely disable scrolling during drag
        document.body.style.overflow = 'hidden';
        document.documentElement.style.overflow = 'hidden';
    };

    const handleDragOver = (event) => {
        const { over } = event;
        console.log('🔄 Drag Over:', over ? over.id : 'null', over ? over.data.current : 'no data');
        setOverId(over ? over.id : null);
    };

    const handleDragEnd = (event) => {
        console.log('🏁 Drag End:', event.active.id, '->', event.over ? event.over.id : 'null');
        console.log('   Active Data:', event.active.data.current);
        console.log('   Over Data:', event.over ? event.over.data.current : 'none');
        
        // Re-enable scrolling and restore position
        document.body.style.overflow = '';
        document.documentElement.style.overflow = '';
        
        // Restore scroll position with a delay to override any automatic scrolling
        if (window.dragStartScrollPosition) {
            const targetTop = window.dragStartScrollPosition.top;
            const targetLeft = window.dragStartScrollPosition.left;
            
            // Immediate restore
            window.scrollTo(targetLeft, targetTop);
            
            // Delayed restore to override any automatic scrolling
            setTimeout(() => {
                window.scrollTo(targetLeft, targetTop);
            }, 0);
            
            setTimeout(() => {
                window.scrollTo(targetLeft, targetTop);
            }, 10);
            
            setTimeout(() => {
                window.scrollTo(targetLeft, targetTop);
            }, 50);
            
            window.dragStartScrollPosition = null;
        }
        
        setActiveId(null);
        setOverId(null);
        setDraggedItem(null);
        
        const { active, over } = event;
        
        if (!over) {
            console.log('❌ No drop target');
            return;
        }

        const activeId = active.id;
        const overId = over.id;
        
        console.log('🔍 Raw IDs - activeId:', activeId, 'overId:', overId);
        

        // Determine containers based on our data structure
        const activeData = active.data.current;
        const overData = over.data.current;
        
        // Check if we're trying to drag a group into another group - this is not allowed
        if (activeData?.type === 'group' && overData?.groupId) {
            return; // Prevent groups from being dropped into other groups
        }
        
        let sourceContainer = 'mainMenu';
        let destContainer = 'favorites';
        
        // Determine source container
        if (activeData?.groupId) {
            sourceContainer = `group-${activeData.groupId}`;
        } else if (activeData?.isMainMenu) {
            sourceContainer = 'mainMenu';
        } else {
            sourceContainer = 'favorites';
        }
        
        // Determine destination container
        // If dragging FROM mainMenu (template), destination should always be favorites or groups
        if (activeData?.isMainMenu) {
            if (overData?.groupId) {
                destContainer = `group-${overData.groupId}`;
            } else if (overId.toString().startsWith('group-')) {
                // Handle group drop zones for template items
                if (overId.toString().includes('-drop-zone-')) {
                    const parts = overId.toString().split('-drop-zone-');
                    destContainer = parts[0]; // e.g., "group-PROJECT_MANAGEMENT"
                } else {
                    destContainer = overId.toString();
                }
            } else {
                destContainer = 'favorites';
            }
        } else {
            // Special handling for group moves - groups can only be reordered at top level
            if (activeData?.type === 'group') {
                // For groups, only allow dropping on drop zones or other groups at top level
                if (overId.toString().startsWith('drop-zone-')) {
                    destContainer = 'favorites';
                } else if (overData?.type === 'group') {
                    destContainer = 'favorites'; // Group-to-group reordering at top level
                } else if (overId.toString().startsWith('custom-') || overId === 'favorites') {
                    destContainer = 'favorites'; // Dropping on top-level items or favorites area
                } else {
                    // Prevent dropping groups into group items or invalid targets
                    console.log('🚫 Invalid drop target for group');
                    return;
                }
            } else {
                // Normal destination detection for non-template items
                if (overData?.groupId) {
                    destContainer = `group-${overData.groupId}`;
                } else if (overData?.isMainMenu) {
                    destContainer = 'mainMenu';
                } else if (overId.toString().startsWith('drop-zone-')) {
                    // Dropping on a drop zone - always treat as favorites
                    destContainer = 'favorites';
                } else if (overId.toString().includes('-drop-zone-')) {
                    // Dropping on a group drop zone - extract group ID
                    const parts = overId.toString().split('-drop-zone-');
                    if (parts[0].startsWith('group-')) {
                        destContainer = parts[0]; // e.g., "group-PROJECT_MANAGEMENT"
                    } else {
                        destContainer = 'favorites';
                    }
                } else if (overId.toString().startsWith('group-')) {
                    destContainer = overId.toString();
                } else if (overData?.type === 'group') {
                    // If dropping on a group header, treat as same container if item is from that group
                    if (activeData?.groupId && activeData.groupId === getItemId(overData.item)) {
                        destContainer = `group-${activeData.groupId}`;
                    } else {
                        destContainer = `group-${getItemId(overData.item)}`;
                    }
                } else {
                    destContainer = 'favorites';
                }
            }
        }
        
        // For groups, only allow reordering within favorites (top level)
        if (activeData?.type === 'group' && destContainer !== 'favorites') {
            console.log('🚫 Group move blocked - groups can only be reordered at top level');
            return;
        }

        console.log('🔄 Container transfer:', sourceContainer, '->', destContainer);

        // If dragging from mainMenu (template), always treat as cross-container move
        if (sourceContainer === 'mainMenu') {
            console.log('🔀 Template to custom menu move to:', destContainer);
            handleCrossContainerMove(sourceContainer, destContainer, activeId, overId);
        } else if (sourceContainer === destContainer) {
            console.log('📋 Same container reorder');
            handleSameContainerReorder(sourceContainer, activeId, overId);
        } else {
            console.log('🔀 Cross container move');
            handleCrossContainerMove(sourceContainer, destContainer, activeId, overId);
        }
    };
    
    const handleSameContainerReorder = (containerId, activeId, overId) => {
        console.log('🔄 Same container reorder:', containerId, activeId, '->', overId);
        
        const originalActiveId = getOriginalId(activeId);
        const originalOverId = getOriginalId(overId);
        
        console.log('🔍 Reorder Original IDs - activeId:', originalActiveId, 'overId:', originalOverId);
        
        if (containerId === 'favorites') {
            // Check if dropping on a drop zone
            if (overId.toString().startsWith('drop-zone-')) {
                // This is a reorder within favorites using drop zones
                const oldIndex = customMenu.findIndex(item => getItemId(item) === originalActiveId);
                let newIndex;
                
                if (overId === 'drop-zone-start') {
                    newIndex = 0;
                } else {
                    const dropZoneIndex = parseInt(overId.toString().replace('drop-zone-', ''));
                    newIndex = dropZoneIndex + 1;
                }
                
                console.log('   Favorites reorder via drop zone:', oldIndex, '->', newIndex);
                if (oldIndex !== -1 && oldIndex !== newIndex) {
                    // For groups and items, handle the adjustment differently
                    let adjustedNewIndex;
                    if (oldIndex < newIndex) {
                        adjustedNewIndex = newIndex - 1;
                    } else {
                        adjustedNewIndex = newIndex;
                    }
                    
                    console.log('   Adjusted newIndex:', adjustedNewIndex);
                    const newCustomMenu = arrayMove(customMenu, oldIndex, adjustedNewIndex);
                    setCustomMenu(newCustomMenu);
                    console.log('✅ Favorites reordered via drop zone to position', adjustedNewIndex);
                }
            } else {
                // Reordering in main favorites using regular items
                const oldIndex = customMenu.findIndex(item => getItemId(item) === originalActiveId);
                const newIndex = customMenu.findIndex(item => getItemId(item) === originalOverId);
                
                console.log('   Favorites reorder:', oldIndex, '->', newIndex);
                if (oldIndex !== -1 && newIndex !== -1 && oldIndex !== newIndex) {
                    const newCustomMenu = arrayMove(customMenu, oldIndex, newIndex);
                    setCustomMenu(newCustomMenu);
                    console.log('✅ Favorites reordered');
                } else {
                    console.log('❌ Invalid reorder indices');
                }
            }
        } else if (containerId.startsWith('group-')) {
            // Reordering within a group
            const groupId = containerId.replace('group-', '');
            const groupIndex = customMenu.findIndex(item => getItemId(item) === groupId);
            
            if (groupIndex !== -1 && customMenu[groupIndex].subMenu) {
                const subMenu = customMenu[groupIndex].subMenu;
                const oldIndex = subMenu.findIndex(item => getItemId(item) === originalActiveId);
                
                // Check if dropping on group drop zone
                if (overId.toString().includes('-drop-zone-')) {
                    const parts = overId.toString().split('-drop-zone-');
                    const dropZoneIndex = parseInt(parts[1]);
                    let newIndex = dropZoneIndex + 1; // Position after the drop zone
                    
                    console.log('   Group reorder via drop zone:', oldIndex, '->', newIndex, 'in group', groupId);
                    if (oldIndex !== -1) {
                        // Adjust newIndex if moving within same group
                        if (oldIndex < newIndex) {
                            newIndex = newIndex - 1;
                        }
                        
                        // Ensure newIndex is within bounds
                        newIndex = Math.min(newIndex, subMenu.length - 1);
                        newIndex = Math.max(newIndex, 0);
                        
                        console.log('   Adjusted group reorder:', oldIndex, '->', newIndex);
                        if (oldIndex !== newIndex) {
                            const newCustomMenu = [...customMenu];
                            const newSubMenu = arrayMove(subMenu, oldIndex, newIndex);
                            
                            newCustomMenu[groupIndex] = {
                                ...newCustomMenu[groupIndex],
                                subMenu: newSubMenu,
                            };
                            
                            setCustomMenu(newCustomMenu);
                            console.log('✅ Group reordered via drop zone');
                        }
                    }
                } else {
                    // Check if dropping on group itself (originalOverId matches groupId)
                    if (originalOverId === groupId) {
                        console.log('   Dropping on group header - no reorder needed');
                        return; // No action needed when dropping on group header
                    }
                    
                    const newIndex = subMenu.findIndex(item => getItemId(item) === originalOverId);
                    
                    console.log('   Group reorder:', oldIndex, '->', newIndex, 'in group', groupId);
                    if (oldIndex !== -1 && newIndex !== -1 && oldIndex !== newIndex) {
                        const newCustomMenu = [...customMenu];
                        const newSubMenu = arrayMove(subMenu, oldIndex, newIndex);
                        
                        newCustomMenu[groupIndex] = {
                            ...newCustomMenu[groupIndex],
                            subMenu: newSubMenu,
                        };
                        
                        setCustomMenu(newCustomMenu);
                        console.log('✅ Group reordered');
                    } else {
                        console.log('❌ Invalid group reorder indices');
                    }
                }
            }
        }
    };
    
    const handleCrossContainerMove = (sourceContainer, destContainer, activeId, overId) => {
        console.log('🔀 Cross container move:', sourceContainer, '->', destContainer, 'activeId:', activeId, 'overId:', overId);
        
        const originalActiveId = getOriginalId(activeId);
        const originalOverId = overId ? getOriginalId(overId) : null;
        
        console.log('🔍 Original IDs - activeId:', originalActiveId, 'overId:', originalOverId);
        
        // Find the item being moved
        let sourceItem = null;
        let sourceIndex = -1;
        let sourceGroupIndex = -1;
        
        if (sourceContainer === 'mainMenu') {
            sourceItem = menuItems.mainMenu.find(item => item.id === originalActiveId);
            console.log('   Found mainMenu item:', sourceItem);
        } else if (sourceContainer === 'favorites') {
            sourceIndex = customMenu.findIndex(item => getItemId(item) === originalActiveId);
            if (sourceIndex !== -1) {
                sourceItem = customMenu[sourceIndex];
            }
        } else if (sourceContainer.startsWith('group-')) {
            const groupId = sourceContainer.replace('group-', '');
            sourceGroupIndex = customMenu.findIndex(item => getItemId(item) === groupId);
            if (sourceGroupIndex !== -1 && customMenu[sourceGroupIndex].subMenu) {
                sourceIndex = customMenu[sourceGroupIndex].subMenu.findIndex(item => getItemId(item) === originalActiveId);
                if (sourceIndex !== -1) {
                    sourceItem = customMenu[sourceGroupIndex].subMenu[sourceIndex];
                }
            }
        }
        
        if (!sourceItem) {
            console.log('   ❌ No source item found for activeId:', activeId);
            return;
        }
        
        console.log('   ✅ Source item found:', sourceItem.title);
        
        // Handle the move
        const newCustomMenu = [...customMenu];
        
        // Remove from source
        if (sourceContainer === 'favorites') {
            newCustomMenu.splice(sourceIndex, 1);
        } else if (sourceContainer.startsWith('group-') && sourceGroupIndex !== -1) {
            newCustomMenu[sourceGroupIndex].subMenu.splice(sourceIndex, 1);
        }
        
        // Add to destination at the correct position
        if (destContainer === 'favorites') {
            // Check if this item already exists anywhere in custom menu (top-level or groups)
            const baseId = getItemId(sourceItem);
            
            // Check top-level items
            const topLevelIds = newCustomMenu.map(item => getItemId(item));
            if (topLevelIds.includes(baseId)) {
                console.log('🚫 Item already exists at top level:', baseId);
                return; // Prevent duplicate
            }
            
            // Check all group items
            for (const item of newCustomMenu) {
                if (item.subMenu) {
                    const groupItemIds = item.subMenu.map(subItem => getItemId(subItem));
                    if (groupItemIds.includes(baseId)) {
                        console.log('🚫 Item already exists in group:', getItemId(item), 'baseId:', baseId);
                        return; // Prevent duplicate
                    }
                }
            }
            
            const itemCopy = { ...sourceItem };
            
            // Handle drop zones - extract index from drop-zone-{index}
            if (overId.toString().startsWith('drop-zone-')) {
                if (overId === 'drop-zone-start') {
                    console.log('   Adding to favorites via start drop zone, index: 0');
                    // Insert at the beginning
                    newCustomMenu.splice(0, 0, itemCopy);
                    console.log('   ✅ Item inserted at start position');
                } else {
                    const dropZoneIndex = parseInt(overId.toString().replace('drop-zone-', ''));
                    console.log('   Adding to favorites via drop zone, index:', dropZoneIndex + 1);
                    // Insert after the item at dropZoneIndex
                    newCustomMenu.splice(dropZoneIndex + 1, 0, itemCopy);
                    console.log('   ✅ Item inserted at drop zone position', dropZoneIndex + 1);
                }
            } else {
                // Find the position to insert based on overId
                const overIndex = newCustomMenu.findIndex(item => getItemId(item) === overId);
                console.log('   Adding to favorites, overIndex:', overIndex, 'overId:', overId);
                if (overIndex !== -1) {
                    newCustomMenu.splice(overIndex, 0, itemCopy);
                    console.log('   ✅ Item inserted at position', overIndex);
                } else {
                    newCustomMenu.push(itemCopy);
                    console.log('   ✅ Item pushed to end');
                }
            }
        } else if (destContainer.startsWith('group-')) {
            // Add to a group at the correct position
            const destGroupId = destContainer.replace('group-', '');
            const destGroupIndex = newCustomMenu.findIndex(item => getItemId(item) === destGroupId);
            
            if (destGroupIndex !== -1) {
                if (!newCustomMenu[destGroupIndex].subMenu) {
                    newCustomMenu[destGroupIndex].subMenu = [];
                }
                
                const subMenu = newCustomMenu[destGroupIndex].subMenu;
                
                // Check if this item already exists anywhere in custom menu
                const baseId = getItemId(sourceItem);
                
                // Check top-level items
                const topLevelIds = newCustomMenu.map(item => getItemId(item));
                if (topLevelIds.includes(baseId)) {
                    console.log('🚫 Item already exists at top level, cannot add to group:', baseId);
                    return; // Prevent duplicate
                }
                
                // Check all group items (including current group)
                for (const item of newCustomMenu) {
                    if (item.subMenu) {
                        const groupItemIds = item.subMenu.map(subItem => getItemId(subItem));
                        if (groupItemIds.includes(baseId)) {
                            console.log('🚫 Item already exists in group:', getItemId(item), 'baseId:', baseId);
                            return; // Prevent duplicate
                        }
                    }
                }
                
                const itemCopy = { ...sourceItem };
                
                // Handle group drop zones
                if (overId.toString().includes('-drop-zone-')) {
                    const parts = overId.toString().split('-drop-zone-');
                    const dropZoneIndex = parseInt(parts[1]);
                    console.log('   Adding to group via drop zone, index:', dropZoneIndex + 1);
                    // Insert after the item at dropZoneIndex
                    subMenu.splice(dropZoneIndex + 1, 0, itemCopy);
                    console.log('   ✅ Item inserted at group drop zone position', dropZoneIndex + 1);
                } else {
                    // Extract the original ID from the compound overId for group items
                    const originalOverId = getOriginalId(overId);
                    const overIndex = subMenu.findIndex(item => getItemId(item) === originalOverId);
                    
                    console.log('   Adding to group, originalOverId:', originalOverId, 'overIndex:', overIndex);
                    if (overIndex !== -1) {
                        subMenu.splice(overIndex, 0, itemCopy);
                        console.log('   ✅ Item inserted at group position', overIndex);
                    } else {
                        subMenu.push(itemCopy);
                        console.log('   ✅ Item pushed to end of group');
                    }
                }
            }
        }
        
        console.log('   📋 Setting new custom menu:', newCustomMenu);
        console.log('   📋 Item IDs:', newCustomMenu.map(item => ({ id: getItemId(item), title: item.title })));
        setCustomMenu(newCustomMenu);
    };
    
    const getItemId = (item) => {
        return item.id || item.key || `fallback-${Date.now()}`;
    };

    // Extract original IDs from compound IDs
    const getOriginalId = (id) => {
        if (typeof id === 'string') {
            if (id.startsWith('custom-')) {
                return id.substring(7); // Remove 'custom-' prefix
            } else if (id.includes('-')) {
                const parts = id.split('-');
                return parts[parts.length - 1]; // Get the last part as the original ID
            }
        }
        return id;
    };

    const addNewGroup = () => {
        if (!newGroupName.trim()) {
            setError('Group name cannot be empty');
            return;
        }

        const groupId = `custom_group_${Date.now()}`;
        const newGroup = {
            id: groupId,
            title: newGroupName,
            subMenu: [],
        };

        setCustomMenu([...customMenu, newGroup]);
        setNewGroupName('');
        setShowGroupInput(false);
        setError(null);
    };

    const editGroup = (groupId, newName) => {
        if (!newName.trim()) {
            setError('Group name cannot be empty');
            return;
        }

        const newCustomMenu = customMenu.map((item) => {
            const itemId = item.id || item.key;
            if (itemId === groupId) {
                return { ...item, title: newName };
            }
            return item;
        });

        setCustomMenu(newCustomMenu);
        setEditingGroup(null);
        setError(null);
    };

    const removeItem = (itemId, groupId = null) => {
        console.log('🗑️ Removing item:', itemId, 'from group:', groupId);
        
        if (groupId) {
            // Remove item from group - use exact ID match
            const newCustomMenu = customMenu.map((item) => {
                const currentItemId = getItemId(item);
                if (currentItemId === groupId && item.subMenu) {
                    return {
                        ...item,
                        subMenu: item.subMenu.filter((subItem) => getItemId(subItem) !== itemId),
                    };
                }
                return item;
            });
            setCustomMenu(newCustomMenu);
            console.log('✅ Item removed from group');
        } else {
            // Remove item from top level - use exact ID match
            const newCustomMenu = customMenu.filter((item) => {
                const currentItemId = getItemId(item);
                return currentItemId !== itemId;
            });
            setCustomMenu(newCustomMenu);
            console.log('✅ Item removed from top level');
        }
    };

    const saveMenu = () => {
        setLoading(true);
        console.log('💾 Saving menu with', customMenu.length, 'items:', customMenu.map(item => ({ id: getItemId(item), title: item.title })));
        
        // Clean up the menu items before sending - remove unnecessary properties for leaf nodes
        const cleanedMenu = customMenu.map(item => {
            if (item.subMenu && item.subMenu.length > 0) {
                // For groups, clean up sub-items too
                return {
                    ...item,
                    subMenu: item.subMenu.map(subItem => {
                        const { subMenu, ...cleanSubItem } = subItem; // Remove subMenu property from leaf nodes
                        return cleanSubItem;
                    })
                };
            } else {
                // For leaf nodes, remove subMenu property
                const { subMenu, ...cleanItem } = item;
                return cleanItem;
            }
        });
        
        console.log('🧹 Cleaned menu data:', cleanedMenu.map(item => ({ id: getItemId(item), title: item.title, hasSubMenu: !!item.subMenu })));
        
        fetch(`${baseRestURL}/menu/customized`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ favoritesMenu: cleanedMenu }),
        })
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then((result) => {
                console.log('✅ Menu save response:', result);
                setSuccess('Menu saved successfully');
                setLoading(false);
                // Refresh the menu data
                loadMenuData();
            })
            .catch((err) => {
                // eslint-disable-next-line no-console
                console.error('❌ Error saving menu:', err);
                setError('Error saving menu. Please try again.');
                setLoading(false);
            });
    };

    const resetMenu = () => {
        // eslint-disable-next-line no-alert
        if (window.confirm('Are you sure you want to reset your menu to default?')) {
            setLoading(true);
            fetch(`${baseRestURL}/menu/reset`, {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({}),
            })
                .then(handleHTTPErrors)
                .then((response) => response.json())
                .then(() => {
                    setSuccess('Menu reset successfully');
                    setLoading(false);
                    // Refresh the menu data
                    loadMenuData();
                })
                .catch((err) => {
                    // eslint-disable-next-line no-console
                    console.error('Error resetting menu:', err);
                    setError('Error resetting menu. Please try again.');
                    setLoading(false);
                });
        }
    };

    // Create sequential indexes for main menu items (memoized to avoid recalculation)
    const mainMenuIndexMap = React.useMemo(() => {
        let currentIndex = 0;
        const indexMap = new Map();

        if (menuItems.mainMenuStructured) {
            menuItems.mainMenuStructured.forEach((category) => {
                if (category.subMenu) {
                    category.subMenu.forEach((item) => {
                        indexMap.set(item.id, currentIndex);
                        currentIndex += 1;
                    });
                }
            });
        }

        return indexMap;
    }, [menuItems.mainMenuStructured]);

    // Create a global list of all draggable item IDs
    const allDraggableIds = React.useMemo(() => {
        const ids = [];
        
        // Add custom menu item IDs including groups
        customMenu.forEach(item => {
            const itemId = getItemId(item);
            if (item.subMenu) {
                // For groups: add the group itself AND the sub-items
                ids.push(itemId); // Group ID without prefix
                item.subMenu.forEach(subItem => {
                    const subItemId = `${itemId}-${getItemId(subItem)}`;
                    ids.push(subItemId);
                });
            } else {
                // For regular items: add the item with custom prefix
                ids.push(`custom-${itemId}`);
            }
        });
        
        // Add main menu item IDs
        if (menuItems.mainMenu) {
            menuItems.mainMenu.forEach(item => {
                ids.push(item.id);
            });
        }
        
        return ids;
    }, [customMenu, menuItems.mainMenu]);

    // Sortable item component for @dnd-kit
    const SortableItem = ({ item, groupId = null, isMainMenu = false }) => {
        // Create unique ID for items to avoid conflicts
        const baseId = getItemId(item);
        let itemId;
        if (groupId) {
            itemId = `${groupId}-${baseId}`;
        } else if (isMainMenu) {
            itemId = baseId; // Template items keep original ID
        } else {
            itemId = `custom-${baseId}`; // Custom menu items get prefix
        }
        const {
            attributes,
            listeners,
            setNodeRef,
            transform,
            transition,
            isDragging,
        } = useSortable({ 
            id: itemId,
            data: {
                type: 'item',
                item,
                groupId,
                isMainMenu,
            }
        });


        const style = {
            transform: CSS.Transform.toString(transform),
            transition: isDragging ? 'none' : transition,
            opacity: isDragging ? 0.4 : 1,
        };

        // Determine if this item should show drop indicator
        const isDropTarget = overId === itemId;
        const isBeingDragged = activeId === itemId;
        
        let className = styles.menuItem;
        if (isDragging) {
            className += ` ${styles.dragging}`;
        }
        if (isDropTarget && !isBeingDragged) {
            // For horizontal layout, use left/right indicators
            className += ` ${styles.dropIndicatorLeft}`;
        }

        return (
            <div
                ref={setNodeRef}
                style={style}
                className={className}
                {...attributes}
                {...listeners}
            >
                <div className={styles.menuItemContent}>
                    <FontAwesomeIcon
                        icon={faEllipsisV}
                        className={styles.dragHandle}
                    />
                    <span className={styles.itemTitle}>{item.title}</span>
                    {groupId && (
                        <Button
                            color="link"
                            className={styles.actionButton}
                            onClick={() => removeItem(getItemId(item), groupId)}
                            title="Remove from group"
                        >
                            <FontAwesomeIcon icon={faMinus} />
                        </Button>
                    )}
                    {!groupId && !isMainMenu && (
                        <Button
                            color="link"
                            className={styles.actionButton}
                            onClick={() => removeItem(getItemId(item))}
                            title="Remove from favorites"
                        >
                            <FontAwesomeIcon icon={faTrash} />
                        </Button>
                    )}
                </div>
            </div>
        );
    };

    const DroppableArea = ({ id, children, className }) => {
        const {
            setNodeRef,
            isOver,
        } = useDroppable({
            id: id,
        });

        return (
            <div 
                ref={setNodeRef}
                data-droppable-id={id} 
                className={`${className} ${isOver ? styles.draggingOver : ''}`}
            >
                {children}
            </div>
        );
    };

    // Non-sortable item component for template menu
    const TemplateItem = ({ item }) => {
        const itemId = getItemId(item);
        const {
            attributes,
            listeners,
            setNodeRef,
            transform,
            transition,
            isDragging,
        } = useSortable({ 
            id: itemId,
            data: {
                type: 'item',
                item,
                groupId: null,
                isMainMenu: true,
            }
        });

        const style = {
            transform: CSS.Transform.toString(transform),
            transition: isDragging ? 'none' : transition,
            opacity: isDragging ? 0.4 : 1,
        };

        let className = styles.menuItem;
        if (isDragging) {
            className += ` ${styles.dragging}`;
        }

        return (
            <div
                ref={setNodeRef}
                style={style}
                className={className}
                {...attributes}
                {...listeners}
            >
                <div className={styles.menuItemContent}>
                    <FontAwesomeIcon
                        icon={faEllipsisV}
                        className={styles.dragHandle}
                    />
                    <span className={styles.itemTitle}>{item.title}</span>
                </div>
            </div>
        );
    };

    // Function to render menu categories without fixed columns - let CSS handle the layout
    const renderCategoryColumns = (menuStructure) => {
        // Return all categories and let CSS flexbox handle the column layout
        return menuStructure.map((category) => (
            <div key={category.id} className={styles.categoryColumn}>
                <div className={styles.categoryContainer}>
                    <button type="button" className={styles.categoryTitle}>
                        {category.title}
                    </button>
                    <div className="collapse show">
                        <ul className={styles.categoryLinks}>
                            {category.subMenu && category.subMenu.map((item) => (
                                <li key={item.id} className={styles.categoryLink}>
                                    <TemplateItem item={item} />
                                </li>
                            ))}
                            {(!category.subMenu || category.subMenu.length === 0) && (
                                <li className={styles.categoryLink}>
                                    <div className={styles.emptyGroup}>
                                        <p>No items in this category</p>
                                    </div>
                                </li>
                            )}
                        </ul>
                    </div>
                </div>
            </div>
        ));
    };

    const SortableGroup = ({ item }) => {
        const groupId = getItemId(item);
        
        // Make group sortable again for group reordering
        const {
            attributes,
            listeners,
            setNodeRef,
            transform,
            transition,
            isDragging,
        } = useSortable({ 
            id: groupId,
            data: {
                type: 'group',
                item,
            }
        });

        const style = {
            transform: CSS.Transform.toString(transform),
            transition: isDragging ? 'none' : transition,
            opacity: isDragging ? 0.4 : 1,
        };

        // Get all item IDs for this group's SortableContext (with unique group prefix)
        const groupItemIds = (item.subMenu || []).map(subItem => `${groupId}-${getItemId(subItem)}`);
        
        // Determine if this group should show drop indicator
        const isDropTarget = overId === groupId;
        const isBeingDragged = activeId === groupId;
        
        let className = `${styles.menuItem} ${styles.groupItem}`;
        if (isDragging) {
            className += ` ${styles.dragging}`;
        }
        if (isDropTarget && !isBeingDragged) {
            className += ` ${styles.dropIndicatorLeft}`;
        }
        
        // Don't hide group when one of its items is being dragged
        const isChildBeingDragged = activeId && activeId.startsWith(`${groupId}-`);
        if (isChildBeingDragged && !isDragging) {
            // Keep group visible when child item is being dragged
            className = className.replace(styles.dragging, '');
        }

        return (
            <div
                ref={setNodeRef}
                style={style}
                className={className}
            >
                <div className={styles.menuItemContent} {...attributes} {...listeners}>
                    <FontAwesomeIcon icon={faEllipsisV} className={styles.dragHandle} />
                    <FontAwesomeIcon icon={faFolder} className={styles.folderIcon} />

                    {editingGroup === groupId ? (
                        <div className={styles.groupEditForm}>
                            <input
                                type="text"
                                className={styles.groupNameInput}
                                value={newGroupName}
                                onChange={(e) => setNewGroupName(e.target.value)}
                                autoFocus
                            />
                            <Button
                                color="primary"
                                size="sm"
                                className={styles.saveGroupButton}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    editGroup(groupId, newGroupName);
                                }}
                                onMouseDown={(e) => e.stopPropagation()}
                            >
                                <FontAwesomeIcon icon={faSave} />
                            </Button>
                            <Button
                                color="secondary"
                                size="sm"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    setEditingGroup(null);
                                }}
                                onMouseDown={(e) => e.stopPropagation()}
                            >
                                <FontAwesomeIcon icon={faUndo} />
                            </Button>
                        </div>
                    ) : (
                        <>
                            <span className={styles.itemTitle}>{item.title}</span>
                            <Button
                                color="link"
                                className={styles.actionButton}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    setEditingGroup(groupId);
                                    setNewGroupName(item.title);
                                }}
                                onMouseDown={(e) => e.stopPropagation()}
                                title="Edit group name"
                            >
                                <FontAwesomeIcon icon={faPencilAlt} />
                            </Button>
                            <Button
                                color="link"
                                className={styles.actionButton}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    removeItem(groupId);
                                }}
                                onMouseDown={(e) => e.stopPropagation()}
                                title="Remove group"
                            >
                                <FontAwesomeIcon icon={faTrash} />
                            </Button>
                        </>
                    )}
                </div>

                <SortableContext 
                    id={`group-${groupId}`}
                    items={groupItemIds} 
                    strategy={verticalListSortingStrategy}
                >
                    <div className={styles.groupContent}>
                        {item.subMenu && item.subMenu.map((subItem, subIndex) => {
                            return (
                                <React.Fragment key={getItemId(subItem)}>
                                    <SortableItem 
                                        item={subItem} 
                                        groupId={groupId}
                                    />
                                    {/* Add drop zone after each group item */}
                                    <DroppableArea 
                                        id={`group-${groupId}-drop-zone-${subIndex}`} 
                                        className={styles.groupDropZone}
                                    >
                                        <div className={styles.groupDropZoneIndicator} />
                                    </DroppableArea>
                                </React.Fragment>
                            );
                        })}
                        {(!item.subMenu || item.subMenu.length === 0) && (
                            <div className={styles.emptyGroup}>
                                <p>Drop items here</p>
                            </div>
                        )}
                    </div>
                </SortableContext>
            </div>
        );
    };

    if (loading) {
        return <LoadingContainer />;
    }

    return (
        <DndContext
            sensors={sensors}
            collisionDetection={closestCorners}
            onDragStart={handleDragStart}
            onDragOver={handleDragOver}
            onDragEnd={handleDragEnd}
            autoScroll={false}
            measuring={{
                droppable: {
                    strategy: 'whenDragging'
                }
            }}
        >
            <SortableContext 
                id="global-sortable-context"
                items={allDraggableIds} 
                strategy={verticalListSortingStrategy}
            >
                <div className={styles.menuCustomizer}>
                <h2>Customize Your Menu</h2>
                {error && <Alert color="danger">{error}</Alert>}
                {success && <Alert color="success">{success}</Alert>}
                {/* Custom Menu Section */}
                <div className={styles.customMenuSection}>
                    <Card>
                        <CardHeader>
                            Your Custom Menu
                            <div className={styles.headerActions}>
                                {!showGroupInput ? (
                                    <Button
                                        color="primary"
                                        size="sm"
                                        onClick={() => setShowGroupInput(true)}
                                        title="Add a new group"
                                    >
                                        <FontAwesomeIcon icon={faPlus} />
                                        <span>Add Group</span>
                                    </Button>
                                ) : (
                                    <div className={styles.groupForm}>
                                        <input
                                            type="text"
                                            className={styles.groupNameInput}
                                            value={newGroupName}
                                            onChange={(e) => setNewGroupName(e.target.value)}
                                            placeholder="Group name"
                                            autoFocus
                                        />
                                        <Button
                                            color="primary"
                                            size="sm"
                                            onClick={addNewGroup}
                                        >
                                            Add
                                        </Button>
                                        <Button
                                            color="secondary"
                                            size="sm"
                                            onClick={() => {
                                                setShowGroupInput(false);
                                                setNewGroupName('');
                                            }}
                                        >
                                            Cancel
                                        </Button>
                                    </div>
                                )}
                            </div>
                        </CardHeader>
                        <CardBody>
                            <DroppableArea id="favorites" className={styles.horizontalMenuList}>
                                {/* Add drop zone at the beginning */}
                                <DroppableArea 
                                    id="drop-zone-start" 
                                    className={styles.dropZone}
                                >
                                    <div className={styles.dropZoneIndicator} />
                                </DroppableArea>
                                {customMenu.map((item, index) => {
                                    const itemKey = item.subMenu ? getItemId(item) : `custom-${getItemId(item)}`;
                                    return (
                                        <React.Fragment key={itemKey}>
                                            {item.subMenu ? (
                                                <SortableGroup item={item} />
                                            ) : (
                                                <SortableItem 
                                                    item={item} 
                                                    groupId={null} 
                                                    isMainMenu={false} 
                                                />
                                            )}
                                            {/* Add drop zone after each item */}
                                            <DroppableArea 
                                                id={`drop-zone-${index}`} 
                                                className={styles.dropZone}
                                            >
                                                <div className={styles.dropZoneIndicator} />
                                            </DroppableArea>
                                        </React.Fragment>
                                    );
                                })}
                                {customMenu.length === 0 && (
                                    <div className={styles.emptyMenu}>
                                        <p>Drag items from template below</p>
                                    </div>
                                )}
                            </DroppableArea>
                        </CardBody>
                    </Card>
                </div>

                {/* Action Buttons */}
                <div className={styles.actionButtons}>
                    <Button color="primary" onClick={saveMenu}>
                        <FontAwesomeIcon icon={faSave} />
                        <span>Save Changes</span>
                    </Button>
                    <Button color="secondary" onClick={resetMenu}>
                        <FontAwesomeIcon icon={faUndo} />
                        <span>Reset to Default</span>
                    </Button>
                </div>

                {/* Template Menu Section */}
                <div className={styles.templateMenuSection}>
                    <Card>
                        <CardHeader>Available Menu Items (Template)</CardHeader>
                        <CardBody className={styles.templateMenuBody}>
                            {menuItems.mainMenuStructured && renderCategoryColumns(menuItems.mainMenuStructured)}
                        </CardBody>
                    </Card>
                </div>

                </div>
                
                <DragOverlay>
                    {activeId && draggedItem ? (
                        <div className={styles.dragOverlayItem}>
                            <FontAwesomeIcon icon={faEllipsisV} className={styles.dragHandle} />
                            {draggedItem.subMenu ? (
                                <>
                                    <FontAwesomeIcon icon={faFolder} className={styles.folderIcon} />
                                    <span>{draggedItem.title}</span>
                                </>
                            ) : (
                                <span>{draggedItem.title}</span>
                            )}
                        </div>
                    ) : null}
                </DragOverlay>
                
            </SortableContext>
        </DndContext>
    );
}

export default MenuCustomizer;
