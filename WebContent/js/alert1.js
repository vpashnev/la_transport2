function save(form) {
	v1 = form.elements.namedItem("comm33").value;
	v2 = form.elements.namedItem("comm34").value;
	if (v1.trim().length > 0 && v2.trim().length == 0) {
		alert("Complete the phone ending ..."+v1+
			" \r\n(example 905-876-0918@txt.bell.ca)");
		return false;
	}
	v1 = form.elements.namedItem("comm43").value;
	v2 = form.elements.namedItem("comm44").value;
	if (v1.trim().length > 0 && v2.trim().length == 0) {
		alert("Complete the phone ending ..."+v1+
			" \r\n(example 905-876-0918@txt.bell.ca)");
		return false;
	}
	if (confirm("Save parameters?")) {
		form.parentNode.setAttribute("style","cursor:wait;");
	}
	else {
		return false;
	}
}
function select1(box, sfx) {
	if (box.checked) {
		document.getElementById("DCB"+sfx).checked = true;
		document.getElementById("DCV"+sfx).checked = true;
		document.getElementById("DCX"+sfx).checked = true;
		document.getElementById("DCF"+sfx).checked = true;
		document.getElementById("EVT"+sfx).checked = true;
		document.getElementById("EVT2"+sfx).checked = true;
	}
}
