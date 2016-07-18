var mouseX, mouseY;
var initializing = false;
var initializingTimeout = 300; // timeout for initializing

function showConfirmDialog(text) {
	return window.confirm(text);
}

function toggle(component) {
	$(component).toggle('fast');
}

function rowClick(row) {
	if (suppressRowClick != 'true') {
		link = $(row).find("a:first");
		if ($(link).attr('onclick')) {
			suppressNextRowClick();
			$(link).click();
		} else {
			window.location.href = $(link).attr("href");
		}
	}
	suppressRowClick = 'false';
}

function initAceEditor(editorId, textAreaId) {
    $(function () {

        var $el = $('#' + textAreaId);

        var timeout = null;
        function updateBackend() {
          $el.trigger("timerchange");
        }

        var editor = ace.edit(editorId);
        editor.setTheme("ace/theme/merbivore");
        editor.getSession().setMode("ace/mode/groovy");
        editor.setShowInvisibles(true);
        editor.setHighlightActiveLine(true);
        editor.setShowPrintMargin(false);
        // keep textArea and ace in sync
        editor.getSession().setValue($el.val());
        editor.getSession().on('change', function () {
            $el.val(editor.getSession().getValue());
            if(timeout != null) {
              clearTimeout(timeout);
            }
            timeout = setTimeout(updateBackend, 60000); // 60 seconds timeout
        });
        // hide text area
        $el.hide();
        // auto grow
        var heightUpdateFunction = function () {

            // http://stackoverflow.com/questions/11584061/
            var newHeight =
                editor.getSession().getScreenLength()
                    * editor.renderer.lineHeight
                    + editor.renderer.scrollBar.getWidth();

            $('#' + editorId).height(newHeight.toString() + "px");
            // This call is required for the editor to fix all of
            // its inner structure for adapting to a change in size
            editor.resize();
        };
        // Set initial size to match initial content
        heightUpdateFunction();
        // Whenever a change happens inside the ACE editor, update
        // the size again
        editor.getSession().on('change', heightUpdateFunction);
        // set focus
        editor.focus();
    });
}

function rowCheckboxClick(row, event) {
	if (!event)
		event = window.event; // For ie.
	var t = event.target || event.srcElement;
	if (t.type != "checkbox") { /*
								 * disables tableRowClickFunction if you are
								 * over the checkbox
								 */
		cb = $(row).find("input[type='checkbox']");
		cb.attr('checked', !cb.is(':checked'));
	}
}

function suppressNextRowClick() {
	suppressRowClick = 'true';
}
function preventBubble(e) {
	if (!e)
		var e = window.event;
	if (e.stopPropagation) { // if stopPropagation method supported
		e.stopPropagation();
		e.preventDefault();
	} else {
		e.cancelBubble = true;
		e.returnValue = false;
	}
	return false;
}

(function() {
	function submenueMakeClass() {
		$.each($('.navbar-collapse ul.nav > li.dropdown > a'), function(){
			$(this).click(function() {
				$('.navbar-collapse ul.nav li.dropdown li').removeClass('mm_open_sub');
				$('.navbar-collapse ul.nav li').removeClass('mm_open');
				$(this).parent().addClass('mm_open');
			});
		});
		$.each($('.navbar-collapse ul.nav li.dropdown li a'), function(){
			$(this).click(function() {
				$('.navbar-collapse ul.nav li.dropdown li').removeClass('mm_open_sub');
				$(this).parent().addClass('mm_open_sub');
			});
		});
	}

	$(function() {
		submenueMakeClass();
	});

})();

(function() {
	var timeoutMillis = 100;
	var timeout = null;

	$(function() {
		adaptSize();
	});

	$(window).resize(function() {
		if(timeout != null) {
			clearTimeout(timeout);
		}
		timeout = setTimeout(adaptSize, timeoutMillis);
	});

	window.adaptSize = function adaptSize() {
		$.each($('.controls'), function(){
			if($(this).parent().width() > 0) {
				if($(this).parent().hasClass('vertical')){
					$(this).css('width', $(this).parent().width()-10);
				} else {
					if($(this).parent().width() > 300){
						$(this).css('width', $(this).parent().width()-160);
					} else {
						$(this).css('width', $(this).parent().width()-10);
					}
				}
			}
		});
	}
})();

function initializeComponents() {
    if(initializing == true) {
        // the initializing is less then the configured timeout ago
        return;
    }
    initializing = true;
    window.setTimeout(function () {
        initializing = false;
    }, initializingTimeout);
    $('[checked="checked"]').parent().addClass("active");
    $('.btn').button();
    if ($("textarea.autogrow").length) {
		$("textarea.autogrow").autoGrow();
	}
	$('textarea').each(function(my){
	    $(this).keypress(function(e) {
	        if (e.ctrlKey && e.keyCode == 13 || e.ctrlKey && e.keyCode == 10) {
	            $(this).closest('form').find('.btn-success').click();
	        }
	    });
	});
    hideAllTooltips();
	$('[rel=mypopup]').popover({
        container: 'body',
        placement: 'bottom auto',
        trigger: 'hover'
    });

	$('[rel=mypopup-right]').popover({
        container: 'body',
        placement: 'right auto',
        trigger: 'hover'
    });

    $('[rel=mytooltip]').tooltip({
    	container: 'body',
    	placement: 'bottom auto',
        trigger: 'hover'
    });

    $('[rel=mytooltip-right]').tooltip({
    	container: 'body',
    	placement: 'right auto',
        trigger: 'hover'
    });
    $(document).on("show.bs.tooltip, show.bs.popover", function() {
        hideAllTooltips();
    });
}

function hideAllTooltips() {
    $("div.popover").remove();
    $("div.tooltip").remove();
}

/*
 * Only used if the ToggleContainer works without Ajax (wantOnToggleNotification =
 * false)
 */
function toggleCollapseIcon(icon, iconStatusOpened, iconOpened, iconClosed) {
	if ($(icon).hasClass(iconStatusOpened)) {
		$(icon).removeClass().addClass(iconClosed);
	} else {
		$(icon).removeClass().addClass(iconOpened);
	}
}

function showBookmark() {
	$("#bookmark").toggle("normal");
}

var suppressRowClick = 'false';

// Begin: Functionality for DropDownMenu
var timeout = 500;
var closetimer = 0;
var ddmenuitem = 0;

// open hidden layer
function mopen(id) {
	// cancel close timer
	mcancelclosetime();

	// close old layer
	if (ddmenuitem)
		ddmenuitem.style.visibility = 'hidden';

	// get new layer and show it
	ddmenuitem = document.getElementById(id);
	ddmenuitem.style.visibility = 'visible';

}
// close showed layer
function mclose() {
	if (ddmenuitem)
		ddmenuitem.style.visibility = 'hidden';
}

// go close timer
function mclosetime() {
	closetimer = window.setTimeout(mclose, timeout);
}

// cancel close timer
function mcancelclosetime() {
	if (closetimer) {
		window.clearTimeout(closetimer);
		closetimer = null;
	}
}

// close layer when click-out
document.onclick = mclose;
// End: Functionality for DropDownMenu

function openDialog(element, closeScript) {
	$('#' + element).dialog({
		'resizable' : false,
		'draggable' : false,
		'width' : 'auto',
		'height' : 'auto',
		'position' : 'center',
		'modal' : true,
		close : closeScript
	}).dialog('open');
}

$(function() {
	$(".dialog_content").on("dialogopen", function(event, ui) {
		disableScroll();
	});
	$(".dialog_content").on("dialogclose", function(event, ui) {
		enableScroll();
	});

	initColorPicker();
	doAfterAjaxHandling();

	if (typeof (Wicket) != "undefined" && typeof (Wicket.Event) != "undefined") {
		Wicket.Event.subscribe('/ajax/call/complete', function(jqEvent,
				attributes, jqXHR, errorThrown, textStatus) {
			// handle after AJAX actions
			doAfterAjaxHandling();
            // hide double click layer after request
            //$("#mm_transparentOverlay").hide();
		});
        Wicket.Event.subscribe('/ajax/call/before', function (jqEvent, attributes, jqXHR, errorThrown, textStatus) {
            // show double click layer before request
            //$("#mm_transparentOverlay").show();
        });
	}
	$('.pf_preventClickBubble').on("contextmenu", function(e) {
		e.stopImmediatePropagation();
	});
	$('.pf_preventClickBubble').click(function(e) {
		e.stopImmediatePropagation();
	});
});

$(function() {
	/* Scroll to the highlighted table row if exist: */
	var w = $(window);
	var row = $('td.highlighted:first').parent('tr');
	if (row.length){
	    $('html,body').animate({scrollTop: row.offset().top - (w.height()/2)}, 500 );
	}
});

function doAfterAjaxHandling() {
	var $uploadProxy = $('.pf_uploadField button[name="fileUploadProxy"], .pf_uploadField .label');
	$uploadProxy.unbind('click').click(function(e) {
		$(this).siblings('input[type="file"]').click();
		e.preventDefault();
	}).siblings('input[type="file"]').change(function(e) {
		$(this).siblings('.label').val(/([^\\\/]+)$/.exec(this.value)[1]); // Extract
		// the
		// filename
		$(this).siblings('.label').change();
	});
	$("fieldset > div > input[type=checkbox]").addClass("checkbox");
	//$(".jqui_checkbox").buttonset();
	initializeComponents();
  adaptSize();
  $(".mm_delayBlur").blur(function(e) {
    var that = $(this);
    setTimeout(function() {
      that.siblings(".icon-remove").click();
    }, 400);
  });
  // quickfix to handle wicket checkboxes to work with bootstrap3
  $(document).on("change", "[data-toggle^=button] [type=checkbox]", function() {
    eval($(this).attr("onclick"));
  });
  $(document).on("change", "[data-toggle^=button] [type=radio]", function() {
	    eval($(this).attr("onclick"));
	  });
}

function initColorPicker() {
	$('.pf_colorPreview').on('click', function() {
		$(this).siblings('.pf_colorForm').find('.pf_colorPickerField').click();
	});
}

function disableScroll() {
	var before = $(document).width();
	$("html").css("overflow", "hidden");
	var after = $(document).width();
	$("body").css("padding-right", after - before);
}

function enableScroll() {
	$("html").css("overflow", "auto");
	$("body").css("padding-right", 0);
}

function pf_deleteClick(element, content, liElement) {
	var callback = $(element).data("callback");
	callback = callback + "&delete=" + content;
	var wcal = $.get(callback);
	if (wcal != null) {
		var li = $(liElement).parents('li');
		$(li).data("me").flushCache();
		$(li).data("me").clearHideTimeout();
		$(li).remove();
	}
}

/**
 * Drag and drop inspired by http://www.sitepoint.com/html5-file-drag-and-drop,
 * <br/> but was mixed and enhanced with jQuery and the HTML5 file API by
 * Johannes.
 */
(function() {

	$(function() {
		// call initialization file only if API is available
		if (window.File && window.FileList && window.FileReader) {
			initDragAndDrop();
		} else {
            // disable dnd
            $('.pf_dnd').addClass('disabled');
        }
	});

	// initialize drag and drop
	function initDragAndDrop() {
		var fileselect = $(".pf_dnd .pf_fileselect");
		var filedrag = $(".pf_dnd .pf_filedrag");
		// file select
		$(fileselect).on("change", fileSelectHandler);
		try {
			// is XHR2 available?
			var xhr = new XMLHttpRequest();
			if (xhr.upload) {
				// file drop
				$(filedrag).on("dragover", fileDragHover);
				$(filedrag).on("dragleave", fileDragHover);
				$(filedrag).on("drop", fileSelectHandler);
				$(filedrag).show();
				$(fileselect).hide();
			}
		} catch (e) { /* just do nothing, no XHR2 available */
		}
		;
	}

	// file drag hover
	function fileDragHover(e) {
		e.stopPropagation();
		e.preventDefault();
		if (e.type == "dragover") {
			$(e.target).addClass("hover");
		} else {
			$(e.target).removeClass("hover");
		}
	}

	// file selection
	function fileSelectHandler(e) {
		// cancel event and hover styling
		fileDragHover(e);
		// fetch file object
		var files = e.originalEvent.target.files
				|| e.originalEvent.dataTransfer.files;
		if (files == null) {
			console.log("*** Files are null!");
			return;
		}
		if (files.length != 1) {
			console.log("*** Number of files is " + files.length + ", but 1 is expected!");
			// TODO ju: error handling
			return;
		}
		var file = files[0];
		if (file == null) {
			console.log("*** File is null!");
			return;
		}
		if (file.size > 1048576) {
			/* 1 mbyte max */
			console.log("*** File is to big: " + file.size + " > 1MB.");
			return;
		}
    var hiddenForm = $(e.originalEvent.target).closest(".pf_dnd").children(".pf_hiddenForm");
    var mimeType = hiddenForm.data("mimetype");
    if (mimeType != undefined && file.type != mimeType) {
      console.log("*** File of type '" + file.type + "' not supported. '" + mimeType + "' expected instead.");
      return;
    }
    hiddenForm.children(".pf_name").val(file.name);
		try {
			var reader = new FileReader();
			reader.onload = function(event) {
				var result = event.target.result;
				hiddenForm.children(".pf_text").val(result);
				hiddenForm.children(".pf_submit").click();
			}
			reader.readAsText(file);
		} catch (e) {
			// TODO ju: error handling
		}
	}
})();

// source: https://developer.mozilla.org/en-US/docs/JavaScript/Reference/Global_Objects/JSON
if (!window.JSON) {
window.JSON = {
  parse: function (sJSON) { return eval("(" + sJSON + ")"); },
  stringify: function (vContent) {
    if (vContent instanceof Object) {
      var sOutput = "";
      if (vContent.constructor === Array) {
        for (var nId = 0; nId < vContent.length; sOutput += this.stringify(vContent[nId]) + ",", nId++);
        return "[" + sOutput.substr(0, sOutput.length - 1) + "]";
      }
      if (vContent.toString !== Object.prototype.toString) { return "\"" + vContent.toString().replace(/"/g, "\\$&") + "\""; }
      for (var sProp in vContent) { sOutput += "\"" + sProp.replace(/"/g, "\\$&") + "\":" + this.stringify(vContent[sProp]) + ","; }
      return "{" + sOutput.substr(0, sOutput.length - 1) + "}";
    }
    return typeof vContent === "string" ? "\"" + vContent.replace(/"/g, "\\$&") + "\"" : String(vContent);
  }
};
}
