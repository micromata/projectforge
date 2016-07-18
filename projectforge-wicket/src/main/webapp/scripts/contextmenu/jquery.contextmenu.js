/**
 * Copyright (c)2005-2009 Matt Kruse (javascripttoolbox.com)
 * 
 * Dual licensed under the MIT and GPL licenses. 
 * This basically means you can use this code however you want for
 * free, but don't claim to have written it yourself!
 * Donations always accepted: http://www.JavascriptToolbox.com/donate/
 * 
 * Please do not link to the .js files on javascripttoolbox.com from
 * your site. Copy the files locally to your server instead.
 * 
 */
/**
 * jquery.contextmenu.js
 * jQuery Plugin for Context Menus
 * http://www.JavascriptToolbox.com/lib/contextmenu/
 *
 * Copyright (c) 2008 Matt Kruse (javascripttoolbox.com)
 * Dual licensed under the MIT and GPL licenses. 
 *
 * @version 1.1
 * @history 1.1 2010-01-25 Fixed a problem with 1.4 which caused undesired show/hide animations
 * @history 1.0 2008-10-20 Initial Release
 * @todo Hide all other menus when contextmenu is shown?
 * @todo More themes
 * @todo Nested context menus
 */
;(function($){
	$.contextMenu = {

		offsetX:0,
		offsetY:0,
		appendTo:'body',
		direction:'down',
		constrainToScreen:true,
				
		showTransition:'show',
		hideTransition:'hide',
		showSpeed:null,
		hideSpeed:null,
		showCallback:null,
		hideCallback:null,
		
		className:'context-menu',
		itemClassName:'context-menu-item',
		itemHoverClassName:'context-menu-item-hover',
		disabledItemClassName:'context-menu-item-disabled',
		disabledItemHoverClassName:'context-menu-item-disabled-hover',
		separatorClassName:'divider',
		innerDivClassName:'context-menu-item-inner',
		themePrefix:'context-menu-theme-',
		theme:'default',

		separator:'context-menu-separator', // A specific key to identify a separator
		target:null, // The target of the context click, to be populated when triggered
		menu:null, // The jQuery object containing the HTML object that is the menu itself
		shown:false, // Currently being shown?
		
		// Create the menu instance
		create: function(menu,opts) {
			var cmenu = $.extend({},this,opts); // Clone all default properties to created object
			
			// If a selector has been passed in, then use that as the menu
			if (typeof menu=="string") {
				cmenu.menu = $(menu);
			} 
			// If a function has been passed in, call it each time the menu is shown to create the menu
			else if (typeof menu=="function") {
				cmenu.menuFunction = menu;
			}
			// Otherwise parse the Array passed in
			else {
				cmenu.menu = cmenu.createMenu(menu,cmenu);
			}
			if (cmenu.menu) {
				cmenu.menu.css({display:'none'});
				$(cmenu.appendTo).append(cmenu.menu);
			}

			$('body').bind('contextmenu',function(){cmenu.hide();}); // If right-clicked somewhere else in the document, hide this menu
			return cmenu;
		},

		// Accept an Array representing a menu structure and turn it into HTML
		createMenu: function(menu,cmenu) {
			var className = cmenu.className;
			$.each(cmenu.theme.split(","),function(i,n){className+=' '+cmenu.themePrefix+n});
			var $container	= $('<ul class="dropdown-menu"/>').mousedown(function(){cmenu.hide(); return false;}); // We wrap a table around it so width can be flexible

			// Each menu item is specified as either:
			//     title:function
			// or  title: { property:value ... }
			for (var i=0; i<menu.length; i++) {
				var m = menu[i];
				if (m==$.contextMenu.separator) {
					$container.append(cmenu.createSeparator());
				}
				else {
					for (var opt in menu[i]) {
						$container.append(cmenu.createMenuItem(opt,menu[i][opt])); // Extracted to method for extensibility
					}
				}
			}
			return $container;
		},
		
		// Create an individual menu item
		createMenuItem: function(label,obj) {
			var cmenu = this;
			if (typeof obj=="function") { obj={onclick:obj}; } // If passed a simple function, turn it into a property of an object
			// Default properties, extended in case properties are passed
			var o = $.extend({
				onclick:function() { },
				className:'',
				hoverClassName:cmenu.itemHoverClassName,
				icon:'',
				disabled:false,
				title:'',
				hoverItem:cmenu.hoverItem,
				hoverItemOut:cmenu.hoverItemOut
			}, obj);
			// If an icon is specified, hard-code the background-image style. Themes that don't show images should take this into account in their CSS
			var iconStyle = (o.icon)?'background-image:url('+o.icon+');':'';
			var $element = $('<li title="'+o.title+'"/>')
							// If the item is disabled, don't do anything when it is clicked
							.mousedown(function (e) { e.preventDefault(); return false; })
							.click(function (e) {
								if (cmenu.isItemDisabled(this)) {return false;}
								else {return o.onclick.call(cmenu.target,this,cmenu,e)}
							})
							// Change the class of the item when hovered over
							.hover( function () { o.hoverItem.call(this,(cmenu.isItemDisabled(this))?cmenu.disabledItemHoverClassName:o.hoverClassName); }
								  , function () { o.hoverItemOut.call(this,(cmenu.isItemDisabled(this))?cmenu.disabledItemHoverClassName:o.hoverClassName); }
							);
			var $inner = $('<a href="#" style="'+iconStyle+'"><span>'+label+'</span></a>').click(function (e) {e.preventDefault();});
			$element.append($inner);
			return $element;
		},
		
		// Create a separator row
		createSeparator: function() {
			return $('<li class="'+this.separatorClassName+'"></li>');
		},
		
		// Determine if an individual item is currently disabled. This is called each time the item is hovered or clicked because the disabled status may change at any time
		isItemDisabled: function(item) { return $(item).is('.'+this.disabledItemClassName); },
		
		// Functions to fire on hover. Extracted to methods for extensibility
		hoverItem: function(c) { $(this).addClass(c); },
		hoverItemOut: function(c) { $(this).removeClass(c); },
		
		// A hook to call before the menu is shown, in case special processing needs to be done.
		// Return false to cancel the default show operation
		beforeShow: function() { return true; },
		
		// Show the context menu
		show: function(t,e) {
			var cmenu=this, x=e.pageX, y=e.pageY;
			cmenu.target = t; // Preserve the object that triggered this context menu so menu item click methods can see it
			if (cmenu.beforeShow()!==false) {
				// If the menu content is a function, call it to populate the menu each time it is displayed
				if (cmenu.menuFunction) {
					if (cmenu.menu) { $(cmenu.menu).remove(); }
					cmenu.menu = cmenu.createMenu(cmenu.menuFunction(cmenu,t),cmenu);
					cmenu.menu.css({display:'none'});
					$(cmenu.appendTo).append(cmenu.menu);
				}
				var $c = cmenu.menu;
				x += cmenu.offsetX;
				y += cmenu.offsetY;
				var pos = cmenu.getPosition(x,y,cmenu,e); // Extracted to method for extensibility
				$c.css( {top:pos.y+"px", left:pos.x+"px", position:"absolute",zIndex:9999} )[cmenu.showTransition](cmenu.showSpeed,((cmenu.showCallback)?function(){cmenu.showCallback.call(cmenu);}:null));
				cmenu.shown = true;
				$(document).bind('mousedown.contextmenu keydown.contextmenu', function (e) {
					if (!e.keyCode || e.keyCode == 27) { // 27 == Escape
						cmenu.hide();
						$(this).unbind('mousedown.contextmenu keydown.contextmenu');
					}
				});
			}
		},
		
		// Find the position where the menu should appear, given an x,y of the click event
		getPosition: function(clickX,clickY,cmenu,e) {
			var x = clickX + cmenu.offsetX;
			var y = clickY + cmenu.offsetY
			var h = $(cmenu.menu).height();
			var w = $(cmenu.menu).width();
			var dir = cmenu.direction;
			if (cmenu.constrainToScreen) {
				var $w = $(window);
				var wh = $w.height();
				var ww = $w.width();
				if (dir=="down" && (y+h-$w.scrollTop() > wh)) { dir = "up"; }
				var maxRight = x + w - $w.scrollLeft();
				if (maxRight > ww) { x -= (maxRight-ww); }
			}
			if (dir == "up") { y -= h; }
			return {'x':x,'y':y};
		},
		
		// Hide the menu, of course
		 hide: function() {
	            var cmenu = this;
	            if (cmenu.shown) {
	                if (cmenu.iframe) {
	                    $(cmenu.iframe).hide();
	                }
	                if (cmenu.menu) {
	                	cmenu.menu[cmenu.hideTransition](cmenu.hideSpeed, ( typeof cmenu.hideCallback === 'function'
	                		? function () { return cmenu.hideCallback.call(cmenu); }
                			: null
                			)
	                	);
	                	if(cmenu.hideCallback != undefined) {
	                	  cmenu.hideCallback.call(cmenu);
	                	}
	                }
	                if (cmenu.shadow) {
	                    cmenu.shadowObj[cmenu.hideTransition](cmenu.hideSpeed);
	                }
	            }
	            cmenu.shown = false;
	        }
	};

	// This actually adds the .contextMenu() function to the jQuery namespace
	$.fn.contextMenu = function(menu,options) {
		var cmenu = $.contextMenu.create(menu,options);
		return this.each(function(){
			$(this).bind('contextmenu',function(e){cmenu.show(this,e);return false;});
		});
	};
})(jQuery);
