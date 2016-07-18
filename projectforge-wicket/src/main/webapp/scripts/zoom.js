// The colors to fade step by step:
var zColor = new Array('#C0C0C0', '#A0A0A0', '#808080', '#606060', '#404040', '#202020', '#000000');
var timeout1, timeout2;

function zoom(text) {
  this.obj = document.getElementById('divZoom');
  this.css = this.obj.style;
  this.css.color = zColor[0];
  this.obj.innerHTML='<span class="zoomText">' + text + '</span>';
  this.css.visibility = 'visible';
  increaseFontSize(10);
}

function increaseFontSize(size)
{
  this.css.fontSize = size + "px";
  size += 5;
  if (size <= 90) {
    timeout1 = window.setTimeout('increaseFontSize(' + size + ')', 20);
  } else {
    fadeFont(1);
  }
}

function fadeFont(cnum) {
  this.css.color = zColor[cnum];
  cnum++;
  if (cnum < zColor.length) {
    timeout2 = window.setTimeout('fadeFont(' + cnum + ')', 50);
  }
}

function hide() {
  this.css.visibility = 'hidden';
  if (timeout1) window.clearTimeout(timeout1);
  if (timeout2) window.clearTimeout(timeout2);
}
