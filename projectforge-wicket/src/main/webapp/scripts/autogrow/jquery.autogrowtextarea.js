/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE" (Revision 42):
 * <jevin9@gmail.com> wrote this file. As long as you retain this notice you
 * can do whatever you want with this stuff. If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return. Jevin O. Sewaruth
 * ----------------------------------------------------------------------------
 *
 * Autogrow Textarea Plugin Version v3.0
 * http://www.technoreply.com/autogrow-textarea-plugin-3-0
 * 
 * THIS PLUGIN IS DELIVERD ON A PAY WHAT YOU WHANT BASIS. IF THE PLUGIN WAS USEFUL TO YOU, PLEASE CONSIDER BUYING THE PLUGIN HERE :
 * https://sites.fastspring.com/technoreply/instant/autogrowtextareaplugin
 *
 * Date: October 15, 2012
 */

jQuery.fn.autoGrow = function() {
	return this.each(function() {

		var createMirror = function(textarea) {
			if (jQuery(textarea).next().hasClass('autogrow-textarea-mirror') == false)
				jQuery(textarea).after('<div class="autogrow-textarea-mirror"></div>');
			return jQuery(textarea).next('.autogrow-textarea-mirror')[0];
		}

		var sendContentToMirror = function (textarea) {
			// set height restrictions
			if (!maxHeight) maxHeight = 500;

			mirror.innerHTML = textarea.value.replace(/\n/g, '<br/>') + '.<br/>.';
			var mirrorHeight = jQuery(mirror).height();
			var textAreaHeight = jQuery(textarea).height();
			if (textAreaHeight < mirrorHeight) {
				// Kai Reinhard
				if (mirrorHeight > maxHeight)
					jQuery(textarea).height(maxHeight);
				else jQuery(textarea).height(mirrorHeight);
			} else {
				if (minHeight && mirrorHeight < minHeight)
					jQuery(textarea).height(minHeight);
				else jQuery(textarea).height(mirrorHeight);
			}
		}

		var growTextarea = function () {
			sendContentToMirror(this);
		}

		// Create a mirror
		var mirror = createMirror(this);
		var minHeight = jQuery(this).attr("autogrowMinHeight"); // Kai Reinhard (KR)
		var maxHeight = jQuery(this).attr("autogrowMaxHeight"); // (KR)
		if (maxHeight) maxHeight *= 20; // maxHeight is pixel and Java parameter in em as well as minHeight.
		
		// Style the mirror
		mirror.style.display = 'none';
		mirror.style.wordWrap = 'break-word';
		mirror.style.padding = jQuery(this).css('padding');
		mirror.style.width = jQuery(this).css('width');
		mirror.style.fontFamily = jQuery(this).css('font-family');
		mirror.style.fontSize = jQuery(this).css('font-size');
		mirror.style.lineHeight = jQuery(this).css('line-height');

		// Style the textarea
		//this.style.overflow = "hidden";
		if (minHeight)
			this.style.minHeight = minHeight+"em";
		else
			this.style.minHeight = this.rows+"em";

		// Bind the textarea's event
		this.onkeyup = growTextarea;

		// Fire the event for text already present
		sendContentToMirror(this);

	});
};
