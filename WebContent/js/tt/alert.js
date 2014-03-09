function save(form) {
	var b = validate(form, "email1", "phone11", "phone12", "phone13", "phone14", "1");
	if (!b) {
		return false;
	}
	b = validate(form, "email21", "phone211", "phone212", "phone213", "phone214", "1");
	if (!b) {
		return false;
	}
	b = validate(form, "email2", "phone21", "phone22", "phone23", "phone24", "2");
	if (!b) {
		return false;
	}
	b = validate(form, "email22", "phone221", "phone222", "phone223", "phone224", "2");
	if (!b) {
		return false;
	}
	b = validate(form, "email3", "phone31", "phone32", "phone33", "phone34", "3");
	if (!b) {
		return false;
	}
	b = validate(form, "email23", "phone231", "phone232", "phone233", "phone234", "3");
	if (!b) {
		return false;
	}
	if (confirm("Save parameters?")) {
		form.parentNode.setAttribute("style","cursor:wait;");
	}
	else {
		return false;
	}
}
function validate(form, id0, id1, id2, id3, id4, sfx) {
	var v1 = form.elements.namedItem(id1).value;
	var v2 = form.elements.namedItem(id2).value;
	var v3 = form.elements.namedItem(id3).value;
	var v = v1 + v2 + v3;
	var n = new Number(v);
	if (v.trim().length > 0 && (v.trim().length != 10 || isNaN(n))) {
		alert("Invalid phone: "+v1+"-"+v2+"-"+v3);
		return false;
	}
	var v4 = form.elements.namedItem(id4).value;
	if (v3.trim().length > 0 && v4.trim().length == 0) {
		alert("Incomplete phone ending ..."+v3+
			" \r\n(example 905-876-0918@txt.bell.ca)");
		return false;
	}
	var v0 = form.elements.namedItem(id0).value;
	if ((v0.trim().length > 0 || v.trim().length > 0) && !hasCmdty(sfx)) {
		if (v0.trim().length > 0) {
			alert("At least one commodity must be selected for: "+v0);
		}
		else if (v.trim().length > 0) {
			alert("At least one commodity must be selected for: "+v1+"-"+v2+"-"+v3);
		}
		return false;
	}
	return true;
}
function hasCmdty(sfx) {
	var chk;
	chk = document.getElementById("DCB"+sfx).checked;
	chk = document.getElementById("DCV"+sfx).checked || chk;
	chk = document.getElementById("DCX"+sfx).checked || chk;
	chk = document.getElementById("DCF"+sfx).checked || chk;
	chk = document.getElementById("EVT"+sfx).checked || chk;
	chk = document.getElementById("EVT2"+sfx).checked || chk;
	chk = document.getElementById("RX"+sfx).checked || chk;
	return chk;
}
function selectAll(box, sfx) {
	var chk = box.checked;
	document.getElementById("DCB"+sfx).checked = chk;
	document.getElementById("DCV"+sfx).checked = chk;
	document.getElementById("DCX"+sfx).checked = chk;
	document.getElementById("DCF"+sfx).checked = chk;
	document.getElementById("EVT"+sfx).checked = chk;
	document.getElementById("EVT2"+sfx).checked = chk;
	document.getElementById("RX"+sfx).checked = chk;
}
function selectItem(textID, box) {
	txt = document.getElementById(textID);  
	var i = box.selectedIndex;
	txt.value = box.options[i].innerHTML;
	box.selectedIndex = 0;
}