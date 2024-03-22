var editor;
var inforTable = [];
var tableDataSet;
var $flowchart;
var $container;
var operatorList = [];
$(document).ready(function() {
	var myTextarea = $("#inputQuerySQL")[0];
	editor = CodeMirror.fromTextArea(myTextarea, {
		lineNumbers: true,
		mode: 'text/x-sql',
		lineWrapping: true,
	});
	$('input[type="text"]#searchColumn').keyup(function() {
		console.log('xxxxxxxxxx');
		var searchText = $(this).val().toUpperCase();

		$('.node-child > li').each(function() {

			var currentLiText = $(this).text().toUpperCase(),
				showCurrentLi = currentLiText.indexOf(searchText) !== -1;
			if (showCurrentLi) {
				$(this).addClass('tv-in-tm').removeClass('tv-out-tm');
			} else {
				$(this).addClass('tv-out-tm').removeClass('tv-in-tm');
			}

		});
	});


	$flowchart = $('#flowchartworkspace');
	$container = $flowchart.parent();

	$flowchart.panzoom();

	var cx = $flowchart.width() / 2;
	var cy = $flowchart.height() / 2;
	$flowchart.panzoom('pan', -cx + $container.width() / 2, -cy + $container.height() / 2);
	var possibleZooms = [0.5, 0.75, 1, 2, 3];
	var currentZoom = 2;
	$container.on('mousewheel.focal', function(e) {
		e.preventDefault();
		var delta = (e.delta || e.originalEvent.wheelDelta) || e.originalEvent.detail;
		var zoomOut = delta ? delta < 0 : e.originalEvent.deltaY > 0;
		currentZoom = Math.max(0, Math.min(possibleZooms.length - 1, (currentZoom + (zoomOut * 2 - 1))));
		$flowchart.flowchart('setPositionRatio', possibleZooms[currentZoom]);
		$flowchart.panzoom('zoom', possibleZooms[currentZoom], {
			animate: false,
			focal: e
		});
	});
	$flowchart.flowchart({
		defaultSelectedLinkColor: '#000055',
		grid: 10,
		multipleLinksOnInput: true,
		multipleLinksOnOutput: true,
	});



	$flowchart.flowchart({
		onOperatorCreate: function(operationId, operatorData) {
			console.log('onCreate', operationId);
			const operatorAdd = {
				operationId: operationId,
				tableName: operatorData.properties.title,
				listInput: [operatorData.properties.inputs.input_1.label],
				listOutput: [operatorData.properties.outputs.output_1.label],
			}
			let isExist = operatorList.find(item => item.operationId == operationId);
			if (!isExist) {
				operatorList.push(operatorAdd);
			}
			return true;
		}
	});
});
function updateDataSet(nameTable) {
	let tableTarget = inforTable.find(item => item.tableName == nameTable);
	if (!tableTarget) return;
	let listDataTable = $('#tableDataSet table.table').find('tbody tr');
	let listDataInforUpdate = [];
	for (let i = 0; i < listDataTable.length; i++) {
		let listDataCell = $(listDataTable[i]).find('td');
		let listDataUpdate = [];
		for (let j = 0; j < listDataCell.length; j++) {
			if (j == 0) {
				let isChecked = $(listDataCell[0]).find('input').is(':checked') ? "true" : "false";
				listDataUpdate.push(isChecked);
			} else {
				listDataUpdate.push(listDataCell[j].innerHTML);
			}
		}
		listDataInforUpdate.push(listDataUpdate);
	}
	tableTarget.listData = listDataInforUpdate;

	//update column

	let listColumnTable = $('#tableDataSet table.table').find('thead tr th');

	let listColumnUpdate = [];

	for (let i = 0; i < listColumnTable.length; i++) {
		if (i != 0) {
			listColumnUpdate.push(listColumnTable[i].innerHTML);
		}
	}
	tableTarget.listColumnName = listColumnUpdate;
	console.log('listColumnUpdate', listColumnUpdate);
}
function showTable(table) {
	$('.list-table-item > ul > li').removeClass('active-li');
	$(table).addClass('active-li');
	let nameTable = table.innerHTML;
	if (tableDataSet && tableDataSet == nameTable) {
		return;
	}
	if (tableDataSet) {
		updateDataSet(tableDataSet);
	}
	tableDataSet = nameTable;
	let tableTarget = inforTable.find(item => item.tableName == nameTable);

	let tableTag = document.createElement('table');
	let theadTag = document.createElement('thead');
	let tbodyTag = document.createElement('tbody');
	if (tableTarget) {
		let trTag = document.createElement('tr');

		let thAction = document.createElement('th');
		thAction.appendChild(document.createTextNode('#'));
		trTag.appendChild(thAction);

		for (const column in tableTarget.listColumnName) {
			let th = document.createElement('th');
			th.appendChild(document.createTextNode(tableTarget.listColumnName[column]));
			trTag.appendChild(th);
		}
		theadTag.appendChild(trTag);

		for (const dataIndex in tableTarget.listData) {
			let trBodyTag = document.createElement('tr');
			for (const cellIndex in tableTarget.listData[dataIndex]) {
				let tdBodyTag = document.createElement('td');
				if (cellIndex == 0) {
					let checkbox = document.createElement('input');
					checkbox.type = 'checkbox';
					let isCheck = tableTarget.listData[dataIndex][cellIndex] == "true";
					checkbox.checked = isCheck;
					tdBodyTag.appendChild(checkbox);
				} else {
					tdBodyTag.appendChild(document.createTextNode(tableTarget.listData[dataIndex][cellIndex]));
					tdBodyTag.setAttribute('contenteditable', 'true');
				}
				trBodyTag.appendChild(tdBodyTag);
			}
			tbodyTag.appendChild(trBodyTag);
		}
	}

	tableTag.appendChild(theadTag);
	tableTag.appendChild(tbodyTag);
	tableTag.setAttribute('class', 'table');
	$('#tableDataSet table').remove();
	$('#tableDataSet').append(tableTag);
}

function addColumnDataSet() {
	let tableDataSet = $('table.table');
	let headerDataSet = $(tableDataSet).find('thead tr th');
	let rowDataSet = $(tableDataSet).find('tbody');
	let listRowDataSet = $(rowDataSet).find('tr');
	for (let i = 0; i < listRowDataSet.length; i++) {
		let listTD = $(listRowDataSet[i]).find('td');
		let cell = document.createElement('td');
		cell.setAttribute('contenteditable', 'true');
		cell.appendChild(document.createTextNode(''));
		$(listTD).first().after(cell);
	}

	let cellColumn = document.createElement('th');
	cellColumn.setAttribute('contenteditable', 'true');
	cellColumn.appendChild(document.createTextNode('column'));
	$(headerDataSet).first().after(cellColumn);
}

function addRowDataSet() {
	let listColumn = $('table.table thead tr th');
	let tbl = $("table.table > tbody");
	let row = document.createElement('tr');
	for (let i = 0; i < listColumn.length; i++) {
		let cell = document.createElement('td');
		if (i == 0) {
			let input = document.createElement('input');
			input.setAttribute('id', tableDataSet);
			input.type = "checkbox";
			cell.appendChild(input);
		} else {
			cell.setAttribute('contenteditable', 'true');
			cell.appendChild(document.createTextNode(''));
		}
		row.appendChild(cell);
	}
	tbl.prepend(row);
}
function switchNavigation(id) {
	stateNav = id;
	$('.box-content-nav').hide();
	$('#' + stateNav).show();
	$('.icon-nav').removeClass('active');
	$('#' + id + '-nav').addClass('active');

	$('.content-right-layout').hide();
	$('#' + id + '-content').show();
	console.log('idddd', id);

}
function openModalDownload() {
	updateDataSet(tableDataSet);
	var modal = document.getElementById("myModal");
	modal.style.display = "block";
}
function closeModal() {
	var modal = document.getElementById("myModal");
	modal.style.display = "none";
}

var operationId = 1;
var sizeTop = 20;
var sizeLeft = 20;
var stateOperation = 1;

function flowChart(data) {
	for (const operator of operatorList) {
		//console.log('of', operator);
		$flowchart.flowchart('deleteOperator', operator.operationId);
	}
	if (!data || !data.mappingKey) {
		return;
	}
	let listTableSQL = data.listTableSQL;

	sizeTop = 20;
	sizeLeft = 20;

	
	operatorList.length = 0;
	stateOperation = 1;
	for (const [key, value] of Object.entries(data.mappingKey)) {
		let arrValue = value[0].split(".");
		let arrValue2 = value[1].split(".");
		let findTable1 = listTableSQL.find(item => item.alias == arrValue[0]);

		let tableNameOperator1;

		let tableNameOperator2;

		if (findTable1) {
			tableNameOperator1 = findTable1.tableName;
		} else {
			tableNameOperator1 = arrValue[0];
		}

		let findTable2 = listTableSQL.find(item => item.alias == arrValue2[0]);

		if (findTable2) {
			tableNameOperator2 = findTable2.tableName;
		} else {
			tableNameOperator2 = arrValue2[0];
		}

		let operatorFilter = operatorList.find(item => item.tableName == tableNameOperator1);

		let operatorFilter2 = operatorList.find(item => item.tableName == tableNameOperator2);

		let isNoRelation = !operatorFilter && !operatorFilter2 && operatorList.length > 0;

		if (isNoRelation) {
			sizeTop = 150 * stateOperation;
			sizeLeft = 20;
			stateOperation++;
		}

		console.log('isNoRelation', isNoRelation);

		let outputTo;

		let operatorIdInput;

		if (operatorFilter) {
			let dataOperator = $flowchart.flowchart('getOperatorData', operatorFilter.operationId);
			operatorIdInput = operatorFilter.operationId;
			let filterData;
			for (const [keyInput, valueInput] of Object.entries(dataOperator.properties.inputs)) {
				if (valueInput.label == arrValue[1]) {
					filterData = keyInput;
					break;
				}
			}
			console.log('is check1', filterData);
			if (filterData) {
				let outputSplit = filterData.split("_");
				outputTo = "output_" + outputSplit[1];

			} else {
				let idNext = Object.entries(dataOperator.properties.inputs).length + 1;
				outputTo = 'output_' + idNext;
				dataOperator.properties.inputs['input_' + idNext] = { label: arrValue[1] };
				dataOperator.properties.outputs['output_' + idNext] = { label: '1' };
				$flowchart.flowchart('setOperatorData', operatorFilter.operationId, dataOperator);
			}

		} else {
			let operatorId = 'operator' + operationId;
			operatorIdInput = operatorId;
			outputTo = 'output_1';
			var operatorData = {
				top: sizeTop,
				left: sizeLeft,
				properties: {
					title: tableNameOperator1,
					inputs: {
						input_1: {
							label: arrValue[1],
						}
					},
					outputs: {
						output_1: {
							label: '1',
						}
					}
				}
			};
			sizeTop += 30;
			sizeLeft += 220;
			$flowchart.flowchart('createOperator', operatorId, operatorData);
			operationId++;
		}



		let inputTo;

		let operatorIdOutput;
		if (operatorFilter2) {
			let dataOperator = $flowchart.flowchart('getOperatorData', operatorFilter2.operationId);
			operatorIdOutput = operatorFilter2.operationId;
			let filterData;
			for (const [keyInput, valueInput] of Object.entries(dataOperator.properties.inputs)) {
				console.log('State', arrValue2[0]);
				console.log('test', valueInput.label);
				console.log('test1', arrValue2[1]);
				if (valueInput.label == arrValue2[1]) {
					filterData = keyInput;
					break;
				}
			}
			console.log('is check2', filterData);
			if (filterData) {
				inputTo = filterData;
			} else {
				//add them input
				let idNext = Object.entries(dataOperator.properties.inputs).length + 1;
				inputTo = 'input_' + idNext;
				dataOperator.properties.inputs['input_' + idNext] = { label: arrValue2[1] };
				dataOperator.properties.outputs['output_' + idNext] = { label: '1' };

				$flowchart.flowchart('setOperatorData', operatorFilter2.operationId, dataOperator);
			}
		} else {
			let operatorId = 'operator' + operationId;
			inputTo = 'input_1';
			operatorIdOutput = operatorId;
			var operatorData = {
				top: sizeTop,
				left: sizeLeft,
				properties: {
					title: tableNameOperator2,
					inputs: {
						input_1: {
							label: arrValue2[1],
						}
					},
					outputs: {
						output_1: {
							label: '1',
						}
					}
				}
			};
			sizeTop += 30;
			sizeLeft += 220;
			$flowchart.flowchart('createOperator', operatorId, operatorData);
			operationId++;
		}

		const link = {
			fromOperator: operatorIdInput,
			fromConnector: outputTo,
			toOperator: operatorIdOutput,
			toConnector: inputTo,
		}

		console.log('link', link);

		$flowchart.flowchart('addLink', link);
	}
}
