var dx = 338, dy = 72, dToX, dToY, curRowIdx;

function unfold(but) {
	var s = but.innerHTML;
	var b = s == "+";
	r = but.parentNode.parentNode;
	var start = r.rowIndex;
	t = r.parentNode;
	for (var i = start; i < t.rows.length; i++) {
		r = t.rows[i];
		c = r.cells[4];
		var v = c.innerHTML;
		if (v == null || v.length == 0) {
			if (b) {
				r.style.display = "";
			}
			else {
				r.style.display = "none";
			}
		}
		else {
			break;
		}
	}
	but.innerHTML = b ? "&#8722" : "+";
}
function getTable() {
	return document.getElementById("table");
}
function browseInfo() {
	d = document.getElementById("info");
	if (d.style.visibility != "visible") {
		if (curRowIdx == -1) {
			fillRow(getTable(), 0);
		}
		else {
			focusCurRow();
		}
		d.style.visibility = "visible";
	}
}
function selPriorRow() {
	t = getTable();
	var i = curRowIdx-1;
	while (t.rows[i].style.display == "none") {
		i--;
	}
	fillRow(t, i);
}
function selNextRow() {
	t = getTable();
	var i = curRowIdx+1;
	while (t.rows[i].style.display == "none") {
		i++;
	}
	fillRow(t, i);
}
function selRow(row) {
	fillRow(getTable(), row.rowIndex-1);
}
function fillRow(table, rowIdx) {
	if (rowIdx < 0 || rowIdx >= table.rows.length) {
		return;
	}
	r = table.rows[rowIdx];
	if (curRowIdx != -1) {
		r0 = table.rows[curRowIdx];
		r0.style.background = r.style.background;
	}
	curRowIdx = rowIdx;
	r.style.background = "lightblue";
	setInfo(table, r);
	focusCurRow();
}
function setInfo(table, row) {
	c = row.cells[7].childNodes;
	document.getElementById("reason").value = c[1].textContent;
	document.getElementById("status").value = row.cells[5].innerHTML;
	document.getElementById("comment").value = row.cells[6].innerHTML;

	var v = row.cells[4].innerHTML;
	for (var i = row.rowIndex-2; i >= 0 && (v == null || v.length == 0); i--) {
		row = table.rows[i];
		v = row.cells[4].innerHTML;
	}
	document.getElementById("skids").value = row.cells[3].innerHTML;
	document.getElementById("cmdty1").value = v;

	v = row.cells[1].innerHTML;
	for (var i = row.rowIndex-2; i >= 0 && (v == null || v.length == 0); i--) {
		row = table.rows[i];
		v = row.cells[1].innerHTML;
	}
	document.getElementById("date").value = v;
	document.getElementById("time").value = row.cells[2].innerHTML;
	c = row.cells[7].childNodes;
	document.getElementById("stime").value = c[3].textContent;
	document.getElementById("car").value = c[5].textContent;
	document.getElementById("stop").value = c[7].textContent;
}
function focusCurRow() {
	var i = curRowIdx-1;
	if (i < 0) {
		i = 0;
	}
	row = table.rows[i];
	c = row.cells[0].childNodes;
	c[0].focus(); c[0].blur();

	i = curRowIdx+1;
	if (i >= table.rows.length) {
		i = table.rows.length-1;
	}
	row = table.rows[i];
	c = row.cells[0].childNodes;
	c[0].focus(); c[0].blur();
}
function closeInfo() {
	d = document.getElementById("info");
	d.style.visibility = "hidden";
}

function allowDrop(ev) {
	ev.preventDefault();
}

function drag(ev) {
	ev.dataTransfer.setData("Text", ev.target.id);
	var x = ev.clientX;
	var y = ev.clientY;
	dToX = dx - x;
	dToY = dy - y;
}

function drop(ev) {
	ev.preventDefault();
	var id = ev.dataTransfer.getData("Text");
	d = document.getElementById(id);
	dx = ev.clientX + dToX;
	dy = ev.clientY + dToY;
	d.parentNode.style.left = dx;
	d.parentNode.style.top = dy;
}
