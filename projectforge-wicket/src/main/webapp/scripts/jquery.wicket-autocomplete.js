jQuery.autocomplete = function(input, options) {
	// Create a link to self
	var me = this;

	var isSafari = (navigator.appVersion.indexOf('Safari') >= 0);

	// Create jQuery object for input element
	var $input = $(input).attr("autocomplete", "off");

	// Apply inputClass if necessary
	if (options.inputClass) $input.addClass(options.inputClass);

	// Create results
	var results = document.createElement("div");
	// Create jQuery object for results
	var $results = $(results);
    $results.hide().addClass(options.resultsClass).css("position", "absolute");
    if( options.width > 0 ) $results.css("width", options.width);

	// Add to body element
    //    var parentContainer = $input.parents(".modal");
    //    if (parentContainer.length == 0) {
    //    	parentContainer = $("body");
    //    }
    var parentContainer = $("body");
    parentContainer.append(results);
    var $parentContainer = $(parentContainer);
    
	input.autocompleter = me;

	var timeout = null;
	var prev = "";
	var active = -1;
	var cache = {};
	var keyb = false;
	var hasFocus = false;
	var lastKeyPressCode = null;
	var search;

	// flush cache
	function flushCache(){
		cache = {};
		cache.data = {};
		cache.length = 0;
	};

	// flush cache
	flushCache();

	// if there is a data array supplied
	if( options.data != null ){
		var sFirstChar = "", stMatchSets = {}, row = [];

		// no url was specified, we need to adjust the cache length to make sure
		// it fits the local data store
		if( typeof options.url != "string" ) options.cacheLength = 1;

		// loop through the array and create a lookup structure
		for( var i=0; i < options.data.length; i++ ){
			// if row is a string, make an array otherwise just reference the
			// array
			row = ((typeof options.data[i] == "string") ? [options.data[i]] : options.data[i]);

			// if the length is zero, don't add to list
			if( row[0].length > 0 ){
				// get the first character
				sFirstChar = row[0].substring(0, 1).toLowerCase();
				// if no lookup array for this character exists, look it up now
				if( !stMatchSets[sFirstChar] ) stMatchSets[sFirstChar] = [];
				// if the match is a string
				stMatchSets[sFirstChar].push(row);
			}
		}

		// add the data items to the cache
		for( var k in stMatchSets ){
			// increase the cache size
			options.cacheLength++;
			// add to the cache
			addToCache(k, stMatchSets[k]);
		}
	}

	var KEY = {
		UP: 38,
		DOWN: 40,
		DEL: 46,
		TAB: 9,
		RETURN: 13,
		ESC: 27,
		COMMA: 188,
		PAGEUP: 33,
		PAGEDOWN: 34
	};

	$input.keypress(function(e) {
		if (isSafari == false) {
		  handleKeyEvent(e); // Does not work in Safari.
		}
	})
	.keydown(function(e) {
		if (isSafari == true) {
	    	handleKeyEvent(e); // Works for Safari.
	    }
	})
	.focus(function(){
		// track whether the field has focus, we shouldn't process any results
		// if the field no longer has focus
		hasFocus = true;
	})
	.blur(function() {
		// track whether the field has focus
		hasFocus = false;
		hideResults();
	})
	.dblclick(function() {
		if (options.favoriteEntries != 0 && options.favoriteEntries[0]) {
			data = options.favoriteEntries;
            receiveData("", data);
		}
	});

	// hideResultsNow();
	
	function handleKeyEvent(e) {
		// track last key pressed
		lastKeyPressCode = e.keyCode;
		switch(e.keyCode) {
			case KEY.UP:
				e.preventDefault();
				moveSelect(-1);
				break;
			case KEY.DOWN:
				e.preventDefault();
				moveSelect(1);
				break;
			case KEY.PAGEUP:
				e.preventDefault();
				moveSelect(-8);
				break;
			case KEY.PAGEDOWN:
				e.preventDefault();
				moveSelect(8);
				break;
			case KEY.TAB:  // tab
			case KEY.RETURN: // return
				if( selectCurrent() ){
					// make sure to blur off the current field
					$input.get(0).blur();
					$input.get(0).focus();
					e.preventDefault();
					if (options.autoSubmit == true)
						input.form.submit();
				}
				break;
			case KEY.ESC:
				hideResults();
				break;
			default:
				active = -1;
				if (timeout) clearTimeout(timeout);
				timeout = setTimeout(function(){onChange();}, options.delay);
				break;
		}
	}

	function onChange() {
		// ignore if the following keys are pressed: [del] [shift] [capslock]
		if( lastKeyPressCode == 46 || (lastKeyPressCode > 8 && lastKeyPressCode < 32) ) return $results.hide();
		var v = $input.val();
		if (v == prev) return;
		prev = v;
		if (v.length >= options.minChars) {
			$input.addClass(options.loadingClass);
			requestData(v);
		} else {
			$input.removeClass(options.loadingClass);
			$results.hide();
		}
	};
	
 	function moveSelect(step) {

		var listItems = $("li", results);
		var list = $results;
		if (!listItems) return;

		active += step;

		if (active < 0) {
			if (step < -1) { // PAGE_UP
				active = 0;
			} else {
				active = listItems.size() - 1;
			}
		} else if (active >= listItems.size()) {
			if (step > 1) { // PAGE_DOWN
				active = listItems.size() - 1;
			} else {
				active = 0;
			}
		}
		
		listItems.removeClass("ac_over");
        var activeItem = listItems.slice(active, active + 1).addClass("ac_over");
		if(options.scroll) {
			var offset = 0;
			listItems.slice(0, active).each(function() {
				offset += this.offsetHeight;
			});
			if((offset + activeItem[0].offsetHeight - list.scrollTop()) > list[0].clientHeight) {
				list.scrollTop(offset + activeItem[0].offsetHeight - list.innerHeight());
			} else if(offset < list.scrollTop()) {
				list.scrollTop(offset);
			}
		}
		



		// Weird behaviour in IE
		// if ([active] && [active].scrollIntoView) {
		// [active].scrollIntoView(false);
		// }

	};
	

	function selectCurrent() {
		var li = $("li.ac_over", results)[0];
		if (!li) {
			var $li = $("li", results);
			if (options.selectOnly) {
				if ($li.length == 1) li = $li[0];
			} else if (options.selectFirst) {
				li = $li[0];
			}
		}
		if (li) {
			selectItem(li);
			return true;
		} else {
			return false;
		}
	};

	function selectItem(li) {
		if (!li) {
			li = document.createElement("li");
			li.extra = [];
			li.selectValue = "";
		}
		var v = $.trim(li.selectValue ? li.selectValue : li.innerHTML);
		input.lastSelected = v;
		prev = v;
		$results.html("");
		newVal = $input.html(v).text(); // Unescape needed for avoiding multiple escaping (Kai R.)
		$input.val(newVal).change();    // Johannes Unterstein, Kai Dorschner: Added
                                        // .change();
		hideResultsNow();
		if (options.onItemSelect) setTimeout(function() { options.onItemSelect(li) }, 1);
	};

	// selects a portion of the input string
	function createSelection(start, end){
		// get a reference to the input element
		var field = $input.get(0);
		if( field.createTextRange ){
			var selRange = field.createTextRange();
			selRange.collapse(true);
			selRange.moveStart("character", start);
			selRange.moveEnd("character", end);
			selRange.select();
		} else if( field.setSelectionRange ){
			field.setSelectionRange(start, end);
		} else {
			if( field.selectionStart ){
				field.selectionStart = start;
				field.selectionEnd = end;
			}
		}
		field.focus();
	};

	// fills in the input box w/the first match (assumed to be the best match)
	function autoFill(sValue){
		// if the last user key pressed was backspace, don't autofill
		if( lastKeyPressCode != 8 ){
			// fill in the value (keep the case the user has typed)
			$input.val($input.val() + sValue.substring(prev.length));
			// select the portion of the value not typed by the user (so the
			// next character will erase)
			createSelection(prev.length, sValue.length);
		}
	};

	function showResults() {
		// either use the specified width, or autocalculate based on form
		// element
        var pos = findPos(input);
        var iWidth = (options.width > 0) ? options.width : $input.width();
		// reposition
		$results.css({
			width: parseInt(iWidth) + "px",
			top: (pos.y + input.offsetHeight) + "px",
			left: pos.x + "px"
		}).show();
		// get the position of the input field right now (in case the DOM is
		// shifted)
		if(options.scroll) {
			$results.scrollTop(0);
			$results.css({
				maxHeight: options.scrollHeight,
				overflow: 'auto'
			});
		}
	};

	function hideResults() {
		if (timeout) clearTimeout(timeout);
		timeout = setTimeout(hideResultsNow, 200);
		active = -1;
	};

	function hideResultsNow() {
		if (timeout) clearTimeout(timeout);
		$input.removeClass(options.loadingClass);
		if ($results.is(":visible")) {
			$results.hide();
		}
		if (options.mustMatch) {
			var v = $input.val();
			if (v != input.lastSelected) {
				selectItem(null);
			}
		}
	};

	function receiveData(q, data) {
		if (data) {
			$input.removeClass(options.loadingClass);
			results.innerHTML = "";

			// if the field no longer has focus or if there are no matches, do
			// not display the drop down
			if( !hasFocus || data.length == 0 ) return hideResultsNow();
			if (navigator.appVersion.indexOf("MSIE") != -1) {
				// we put a styled iframe behind the calendar so HTML SELECT
				// elements don't show through
				$results.append(document.createElement('iframe'));
			}
			results.appendChild(dataToDom(data, q));
			// autofill in the complete box w/the first match as long as the
			// user hasn't entered in more data
			if( options.autoFill && ($input.val().toLowerCase() == q.toLowerCase()) ) autoFill(data[0][0]);
			showResults();
		} else {
			hideResultsNow();
		}
	};

	function dataToDom(data, q) {
		var ul = document.createElement("ul");
		var num = data.length;

		// limited results to a max number
		if( (options.maxItemsToShow > 0) && (options.maxItemsToShow < num) ) num = options.maxItemsToShow;

		for (var i=0; i < num; i++) {
			var row = data[i];
			if (!row) continue;
			var li = document.createElement("li");
			if (options.formatItem) {
				li.innerHTML = options.formatItem(row, q, i, num);
				if (options.selectValue) {
				  li.selectValue = options.selectValue(row);
				} else  if (options.labelValue) {
			      li.selectValue = row[1];
				} else {
				  li.selectValue = row[0];
				}
		    } else if (options.labelValue) {
		        // First col contains label second col the value.
		        // Johannes Unterstein, Kai Dorschner: Added deletableItem
		        li.innerHTML = defaultFormat(row[0], q) + (options.deletableItem ? "<i class='icon-remove-sign red pf_deleteLink' onclick='pf_deleteClick(\"#"+ $input.attr("id") +"\", \""+row[0]+"\", this); return false;'></i>" : ""); 
		        $(li).data('me', me); // Johannes & Kai
		        if (options.selectValue) {
		          li.selectValue = options.selectValue(row);
		        } else {
		          li.selectValue = row[1];
		        }
			} else {
		        // Johannes Unterstein, Kai Dorschner: Added deletableItem
				li.innerHTML = defaultFormat(row[0], q) + (options.deletableItem ? "<i class='icon-remove-sign red pf_deleteLink' onclick='pf_deleteClick(\"#"+ $input.attr("id") +"\", \""+row[0]+"\", this); return false;'></i>" : "");
				$(li).data('me', me); // Johannes & Kai
				if (options.selectValue) {
				  li.selectValue = options.selectValue(row);
				} else {
				  li.selectValue = row[0];
				}
			}
			var extra = null;
			if (row.length > 1) {
				extra = [];
				for (var j=1; j < row.length; j++) {
					extra[extra.length] = row[j];
				}
			}
			li.extra = extra;
			ul.appendChild(li);
			$(li).hover(
				function() { $("li", ul).removeClass("ac_over"); $(this).addClass("ac_over"); active = $("li", ul).indexOf($(this).get(0)); },
				function() { $(this).removeClass("ac_over"); }
			).click(function(e) {
				e.preventDefault();
				e.stopPropagation();
				selectItem(this)
				if (options.autoSubmit == true)
					input.form.submit();
			});
		}
		return ul;
	};

	function requestData(q) {
		if (!options.matchCase) q = q.toLowerCase();
		var data = options.cacheLength ? loadFromCache(q) : null;
		// recieve the cached data
		if (data) {
			receiveData(q, data);
		// if an AJAX url has been supplied, try loading the data now
		} else if( (typeof options.url == "string") && (options.url.length > 0) ){
		  search = q;
		  var attrs = {
					u: options.url+'&q='+q,
					dt: 'json', // datatype
					wr: false, // not Wicket's <ajax-response>
					sh: [function(attributes, jqXHR, data, textStatus) {
						  doUpdateChoices(data);
					}
					]
					};
          var request = Wicket.Ajax.get(attrs);
          // var call = new Wicket.Ajax.ajax(attrs)
		// if there's been no data found, remove the loading class
		} else {
			$input.removeClass(options.loadingClass);
		}
	};

  function doUpdateChoices(data) {
// Wicket.Log.info("data: " + data + ", q=" + search);
// Wicket.Log.info("Response processed successfully.");
// Wicket.Ajax.invokePostCallHandlers();
    addToCache(search, data);
    receiveData(search, data);
  }
  
	function loadFromCache(q) {
		if (!q) return null;
		if (cache.data[q]) return cache.data[q];
		if (options.matchSubset) {
			for (var i = q.length - 1; i >= options.minChars; i--) {
				var qs = q.substr(0, i);
				var c = cache.data[qs];
				if (c) {
					var csub = [];
					for (var j = 0; j < c.length; j++) {
						var x = c[j]; // row
						if (matchSubset(x, q)) {
							csub[csub.length] = x;
						}
					}
					return csub;
				}
			}
		}
		return null;
	};

    function matchToken(x, token) {
	  var hit = false;
	  for (var k = 0; k < x.length; k++) {
	    var s = x[k];
	    if (!options.matchCase)
	      s = s.toLowerCase();
		if (s.indexOf(token) < 0) {
		  continue; // does not match.
	    }
	    if (s.indexOf(token) == 0 || options.matchContains) {
	      hit = true;
	      break;
		}
	  }
	  return hit;
    }
    
	function matchSubset(x, sub) {
	  var token = sub.split(" "); // For AND search.
	  for (var i = 0; i < token.length; i++) {
	  	if (token[i].length == 0) continue;
	    if (matchToken(x, token[i]) == false)
	      return false;
	  }
	  return true;
	};

	this.flushCache = function() {
		flushCache();
	};

	// function by Johannes & Kai 
	this.clearHideTimeout = function() {
		if (timeout) clearTimeout(timeout)
	}

	this.setExtraParams = function(p) {
		options.extraParams = p;
	};

	function addToCache(q, data) {
		if (!data || !q || !options.cacheLength) return;
		if (!cache.length || cache.length > options.cacheLength) {
			flushCache();
			cache.length++;
		} else if (!cache[q]) {
			cache.length++;
		}
		cache.data[q] = data;
	};
	

	function findPos(obj) {
		var offset = $(obj).offset();
		var posY = offset.top ;//- $(window).scrollTop();
		var posX = offset.left;// - $(window).scrollLeft(); 
		return {x:posX, y:posY};
	}
}

jQuery.fn.autocomplete = function(url, options, data) {
	// Make sure options exists
	options = options || {};
	// Set url as option
	options.url = url;
	// set some bulk local data
	options.data = ((typeof data == "object") && (data.constructor == Array)) ? data : null;

	// Set default values for required options
	options.inputClass = options.inputClass || "ac_input";
	options.resultsClass = options.resultsClass || "ac_results";
	options.minChars = options.minChars || 2;
	options.delay = options.delay || 200;
	options.matchCase = options.matchCase || 0;
	options.matchSubset = options.matchSubset || 1;
	options.matchContains = options.matchContains || 0;
	options.cacheLength = options.cacheLength || 1;
	options.mustMatch = options.mustMatch || 0;
	options.extraParams = options.extraParams || {};
	options.loadingClass = options.loadingClass || "ac_loading";
	options.selectFirst = options.selectFirst || false;
	options.selectOnly = options.selectOnly || false;
	options.maxItemsToShow = options.maxItemsToShow || -1;
	options.autoFill = options.autoFill || false;
	options.width = parseInt(options.width, 10) || 0;
	options.autoSubmit = options.autoSubmit || false;
	options.favoriteEntries = options.favoriteEntries || 0; // List of top
															// entries to show
															// on double click
	options.scroll = options.scroll || true; // Show scrollbars if result
												// list is larger than
												// scrollHeight?
	options.scrollHeight = options.scrollHeight || 400; // Height of result area
														// in pixel
	options.labelValue = options.labelValue || false; // If true then first
    // col is the label to
    // show in drop down
    // list and second col
    // value to fill in
    // input field after
    // selection.

	this.each(function() {
		var input = this;
		new jQuery.autocomplete(input, options);
	});

	// Don't break the chain
	return this;
}

function defaultFormat(str, q) {
	var token = q.split(" "); // For AND search.
	var s = str.toLowerCase();
	var result = "";
	for (var pos = 0; pos < str.length; pos++) {
	  var replaced = false;
	  for (var i = 0; i < token.length; i++) {
	    if (token[i].length == 0) continue;
	    if (s.substr(pos, token[i].length) == token[i]) {
	      result += str.substr(pos, token[i].length).bold();
	      pos += token[i].length - 1;
	      replaced = true;
	      break;
	    }
	  }
	  if (replaced == false) {
	    result += str[pos];
	  }
	}
	return result;
}


jQuery.fn.autocompleteArray = function(data, options) {
	return this.autocomplete(null, options, data);
}

jQuery.fn.indexOf = function(e){
	for( var i=0; i<this.length; i++ ){
		if( this[i] == e ) return i;
	}
	return -1;
};
