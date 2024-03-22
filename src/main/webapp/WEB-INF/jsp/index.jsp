<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
	<title>Data Generator</title>
	<link rel="stylesheet" href="/css/codemirror.css">
	<link rel="stylesheet" href="/css/style.css">
	<link rel="stylesheet" href="/css/jquery.flowchart.css">
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
	<script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.12.1/jquery-ui.min.js"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery.panzoom/3.2.2/jquery.panzoom.min.js"></script>
	<script src="/js/jquery.flowchart.js"></script>
	<script src="/js/codemirror.js"></script>
	<script src="/js/sql.js"></script>
	<script src="/js/datagenerator.js"></script>
	<link rel="preconnect" href="https://fonts.googleapis.com">
	<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
	<link
		href="https://fonts.googleapis.com/css2?family=Roboto:wght@100;300&display=swap"
		rel="stylesheet">
</head>
<body>
	<main>
		<div class="left-layout">
			<div class="container-navigation">
				<div class="box-navigation">
					<div class="icon-nav active" id="box-sql-nav" onclick="switchNavigation('box-sql')">
						<svg viewBox="0 0 1024 1024" version="1.1"><path d="M128 384h384V0h384v1024H128V384z m1.408-85.312L426.688 0.256v298.432H129.408z m119.488 472.704c1.6 18.496 8.384 33.728 20.416 45.76 11.968 11.968 33.536 17.92 64.704 17.92 17.792 0 32.512-2.56 44.16-7.68a60.992 60.992 0 0 0 27.264-22.528 58.112 58.112 0 0 0 9.728-32.512 51.52 51.52 0 0 0-7.296-27.072 55.936 55.936 0 0 0-23.424-20.352c-10.688-5.44-28.416-10.88-53.12-16.192-10.048-2.048-16.384-4.288-19.008-6.72-2.752-2.368-4.16-4.992-4.16-7.872a12.8 12.8 0 0 1 4.992-10.24c3.328-2.752 8.32-4.16 14.912-4.16 8 0 14.272 1.92 18.816 5.632 4.48 3.776 7.488 9.728 8.96 17.984l53.312-3.136c-2.304-18.944-9.6-32.832-21.952-41.536-12.288-8.704-30.08-13.056-53.504-13.056-19.136 0-34.112 2.368-45.12 7.168a54.336 54.336 0 0 0-24.576 19.776 48 48 0 0 0-8.192 26.688 44.8 44.8 0 0 0 16 35.392c10.56 9.216 28.288 16.64 53.12 22.208 15.168 3.328 24.832 6.912 28.992 10.624a16.832 16.832 0 0 1 6.272 12.8 17.024 17.024 0 0 1-6.592 13.12 27.712 27.712 0 0 1-18.688 5.76c-10.88 0-19.2-3.776-24.96-11.2A41.088 41.088 0 0 1 302.72 768l-53.888 3.392z m391.36-122.624V832h145.024v-45.12h-88.32v-138.112h-56.704z m-42.176 156.48c13.376-16.256 20.032-37.888 20.032-65.024 0-30.4-8.32-53.76-24.832-70.08-16.512-16.384-40-24.512-70.4-24.512-29.888 0-53.12 8.32-69.76 25.024-16.64 16.768-24.896 40.192-24.896 70.4 0 31.04 9.472 55.168 28.48 72.32 16.128 14.528 38.08 21.76 65.92 21.76 18.816 0 34.496-2.688 46.976-8.128 3.072 2.688 8.512 6.656 16.256 11.904 7.744 5.312 15.552 9.856 23.488 13.696l15.744-31.744a152.832 152.832 0 0 1-13.12-6.4 264.768 264.768 0 0 1-13.888-9.216z m-43.328-28.992c4.48-8.064 6.72-20.16 6.72-36.224 0-18.56-3.456-31.744-10.304-39.68a35.712 35.712 0 0 0-28.416-11.84 34.496 34.496 0 0 0-27.392 12.16c-7.04 8.064-10.496 20.672-10.496 37.824 0 20.032 3.392 34.048 10.24 42.112 6.848 8.128 16.192 12.16 28.16 12.16 3.84 0 7.424-0.384 10.88-1.152a70.08 70.08 0 0 0-22.528-12.992l8.832-20.352a48.128 48.128 0 0 1 11.712 3.328c2.816 1.344 8.256 4.864 16.32 10.496 1.92 1.344 3.968 2.752 6.272 4.16z"></path></svg>
					</div>
					<div class="icon-nav" id="box-table-info-nav" onclick="switchNavigation('box-table-info')">
						<svg viewBox="0 0 1024 1024" version="1.1"><path d="M512 1024c-212.032 0-384-85.952-384-192v-108.032C128 830.08 299.968 896 512 896s384-65.92 384-172.032V832c0 106.048-171.968 192-384 192z m0-192c-212.032 0-384-85.952-384-192v-108.032C128 638.08 299.968 704 512 704s384-65.92 384-172.032V640c0 106.048-171.968 192-384 192z m0-192c-212.032 0-384-85.952-384-192v-108.032C128 446.08 299.968 512 512 512s384-65.92 384-172.032V448c0 106.048-171.968 192-384 192z m0-192c-212.032 0-384-85.952-384-192V192c0-106.048 171.968-192 384-192s384 85.952 384 192v64c0 106.048-171.968 192-384 192z"></path></svg>
					</div>
					<div class="icon-nav" onclick="openModalDownload()">
						<svg viewBox="0 0 1024 1024" version="1.1"><path d="M512 0C229.2224 0 0 229.2224 0 512s229.2224 512 512 512 512-229.2224 512-512S794.7776 0 512 0z m182.971733 618.171733l-156.296533 156.296534a33.928533 33.928533 0 0 1-5.2224 4.266666c-0.836267 0.5632-1.757867 0.955733-2.645333 1.4336-1.0752 0.580267-2.0992 1.2288-3.242667 1.706667-1.1264 0.4608-2.304 0.733867-3.464533 1.0752-0.9728 0.290133-1.911467 0.6656-2.9184 0.8704a33.826133 33.826133 0 0 1-13.380267 0c-1.024-0.2048-1.962667-0.580267-2.952533-0.8704-1.143467-0.341333-2.304-0.597333-3.413334-1.058133-1.1776-0.477867-2.235733-1.143467-3.345066-1.757867-0.836267-0.4608-1.723733-0.836267-2.525867-1.3824a35.003733 35.003733 0 0 1-5.239467-4.283733l-156.3136-156.3136c-12.032-12.032-14.984533-31.3856-5.051733-45.2096a34.133333 34.133333 0 0 1 51.985067-4.386134l70.263466 70.263467c10.752 10.752 29.1328 3.140267 29.1328-12.066133v-352.426667c0-16.9984 11.6224-32.768 28.398934-35.498667a34.1504 34.1504 0 0 1 39.867733 33.672534v354.2528c0 15.2064 18.3808 22.818133 29.1328 12.066133l70.263467-70.2464a34.133333 34.133333 0 0 1 51.985066 4.386133c9.966933 13.824 7.0144 33.1776-5.0176 45.2096z"></path></svg>
					</div>
				</div>
				<div class="box-content-nav" id="box-sql">
					<div class="box-button">
						<div class="box-control">
							<select class="select-database" id="select-database"
								style="margin-right: 2.5px;">
								<option value="No database">No database</option>
								<option value="Oracle">Oracle</option>
								<option value="Mysql">Mysql</option>
								<option value="H2">H2</option>
							</select> <input type="submit" value="Import SQL" id="importFile"
								style="margin-left: 2.5px;"> <input type="file"
								name="inputFile" id="inputFile" style="display: none;">
						</div>
						<div class="box-input" style="display: none;">
							<input type="text" placeholder="URL" id="url"> <input
								type="text" placeholder="Schema" id="schema"> <input
								type="text" placeholder="User" id="user"> <input
								type="text" placeholder="Password" id="pass">
						</div>
						<div class="box-connect">
							<input type="submit" value="Update query" id="updateQuery"
								style="margin-right: 2.5px;"> <input type="submit"
								value="Test Connection" id="testConnection"
								onclick="testConnection()"
								style="margin-left: 2.5px; display: none;">
						</div>
					</div>
					<div class="box-input-sql">
						<textarea id="inputQuerySQL" rows="10" cols="40"></textarea>
					</div>

				</div>
				<div class="box-content-nav" id="box-table-info" style="display:none;">
					<div class="box-searching">
						<input type="text" placeholder="Search" id="searchColumn"/>
					</div>
					<div class="list-table-item" id="jstree">
					</div>
				</div>
			</div>

		</div>
		<div class="right-layout">
			<div class="content-right-layout" id="box-table-info-content">
				<h4 style="margin-top: 0px;">DATA SET</h4>
				<div class="box-action-table">
					<select class="select-database" id="select-language"
						style="margin-right: 2.5px;">
						<option value="en-us">EN</option>
						<option value="ja-jp">JP</option>
					</select>
					<input type="submit" id="addColumn" value="Add column"
						onclick="addColumnDataSet()"> <input type="submit"
						value="Add row" onclick="addRowDataSet()">
				</div>
				<div id="tableDataSet"></div>
			</div>
			<div class="content-right-layout" id="box-sql-content">
				<div class="flowchart-example-container" id="flowchartworkspace"></div>
			</div>
		</div>
		
	</main>
	<div id="myModal" class="modal">

	  <!-- Modal content -->
	  <div class="modal-content">
	    <span class="close" onclick="closeModal()">&times;</span>
	    <div class="content-download">
	    	<span><img alt="" width="50px" height="50px" src="/image/excel.svg" onclick="generate('Excel')"></span>
	    	<span><img alt="" width="50px" height="50px" src="/image/sql.svg" onclick="generate('SQL')"></span>
	    </div>
	  </div>
	
	</div>
	<script>
	var stateNav = 0;
	
	var dataPicker = [];
	
	var stateId = 0;
	
	var stateIdPicker = 0;
	
	$('#select-database').on('change', function() {
		if ($(this).val() == 'No database') {
			$('#testConnection').hide();
			$('.box-input').hide();
			$('#addColumn').show();
		} else {
			$('#testConnection').show();
			$('.box-input').show();
			$('#addColumn').hide();
		}
	});
	
		//import file
		$('#importFile').on("click", function(){
			$('#inputFile').trigger('click');
		});
		
		$('#updateQuery').on("click", function(){
			$('.list-table-item').empty();
			tableDataSet = undefined;
			const queryInput = editor.getValue();
			let url = $('#url').val();
			let schema = $('#schema').val();
			let user = $('#user').val();
			let pass = $('#pass').val();
			let tableSelected = $('#select-database :selected').val();
			$('table.table').find('thead').empty();
			$('table.table').find('tbody').empty();
			$.ajax({
			       url : '/updateQuery',
			       type : 'GET',
			       data : {
						"query" : queryInput,
						"url" : url,
						"schema" : schema,
						"user" : user,
						"pass" :pass,
						"typeConnection" : tableSelected,
					},
					contentType : "application/json",
					dataType : 'json',
			       success : function(dataResponse) {
			    	   if (dataResponse.mess == null) {
			    		   flowChart(dataResponse.flows);
			    		   let data = dataResponse.listInfo;
			    		   inforTable = dataResponse.listInfo;
			    		   alert("Update success");
				           for (let i = 0 ; i < data.length ; i++) {
				        	   let ul = document.createElement('ul');
				        	   let li = document.createElement('li');
				        	   li.innerHTML = data[i].tableName;
				        	   li.setAttribute('onclick', 'showTable(this)');
				        	   ul.appendChild(li);
				        	   let ulNode = document.createElement('ul');
				        	   ulNode.setAttribute('class', 'node-child')
				        	   let listColumn = data[i].listColumnName
				        	   for(const j in listColumn) {
				        		   let node = document.createElement('li');
				        		   node.innerHTML = listColumn[j];
				        		   ulNode.appendChild(node);
				        	   }
				        	   ul.appendChild(ulNode);
				        	   $('.list-table-item').append(ul);
				           }
			    	   } else {
			    		   console.log(data.mess);
			    		   $("#selectBox").append(data.mess);
			    		   alert(data.mess);
			    	   }
			           
			       }
			});
		});
		
		//import file
		$('#inputFile').on("change", function(e){
			var formData = new FormData();
			formData.append('file', $('#inputFile')[0].files[0]);
			$.ajax({
			       url : '/uploadFile',
			       type : 'POST',
			       data : formData,
			       processData: false,  // tell jQuery not to process the data
			       contentType: false,  // tell jQuery not to set contentType
			       success : function(data) {
			    	   console.log('data', data);
			    	   const dataSQL = JSON.parse(data);
			           console.log('dataSQL', dataSQL);
			           console.log('table', dataSQL.listTable);
			           console.log('query', dataSQL.query);
			           $('textarea#inputQuerySQL').val(dataSQL.query);
			           editor.getDoc().setValue(dataSQL.query);
			           $("#selectBox").empty();
			           $("#selectBox").append('<option value="None" disabled="disabled" selected="selected">Select table</option>');
			           for (const tbl in dataSQL.listTable) {
			        	   console.log('tbl', tbl);
			        	   $("#selectBox").append(new Option(dataSQL.listTable[tbl], dataSQL.listTable[tbl]));
			           }
			       }
			});
		});
		
		function testConnection() {
			console.log('xxx');
			let url = $('#url').val();
			let schema = $('#schema').val();
			let user = $('#user').val();
			let pass = $('#pass').val();
			let tableSelected = $('#select-database :selected').val();
			$.ajax({
			       url : '/testConnection',
			       type : 'GET',
			       data : {
						"url" : url,
						"schema" : schema,
						"user" : user,
						"pass" :pass,
						"tableSelected" : tableSelected
					},
					contentType : "application/json",
					dataType : 'json',
			       success : function(data) {
			           alert(data);
			       }
					
			});
		}
		
		function generate(type) {
			console.log('genate');
			
			const infoDatabase = {
					type : $('#select-database :selected').val(),
					url : $('#url').val(),
					schema : $('#schema').val(),
					user : $('#user').val(),
					password : $('#pass').val(),
			}
			
			const dataClient = [];
			
			for (const table in inforTable) {
				let tableName = inforTable[table].tableName;
				let listColumn = inforTable[table].listColumnName;
				let listData = inforTable[table].listData;
				let listInfo = [];
				for (const data in listData) {
					let listCell = listData[data];
					let listColumnInfo = [];
					if (listCell && listCell[0] && listCell[0] == "true"){
						for (const cell in listCell) {
							if (cell != 0) {
								const columnInfo = {
										name : listColumn[cell-1],
										val : listCell[cell]
								}
								listColumnInfo.push(columnInfo);
							}
							
						}
					}
					if (listColumnInfo && listColumnInfo.length > 0) {
						listInfo.push(listColumnInfo);
					}
				}
				if (listInfo && listInfo.length > 0) {
					const dataPick = {
							tableName : tableName,
							listColumnInfo : listInfo
					}
					dataClient.push(dataPick);
				}
				
				
			}
			var xhr = new XMLHttpRequest();
			xhr.open('POST', '/generate', true);
			xhr.responseType = 'blob';
			xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8');
			xhr.onload = function(e) {
			    if (this.status == 200) {
			    	var blob = new Blob([xhr.response], {type: xhr.getResponseHeader("Content-Type")});
			        var downloadUrl = URL.createObjectURL(blob);
			        var a = document.createElement("a");
			        a.href = downloadUrl;
			        if (xhr.getResponseHeader("Content-Type") == 'application/vnd.ms-excel') {
			        	a.download = "data.xls";
			        } else {
			        	a.download = "data.txt";
			        }
			        document.body.appendChild(a);
			        a.click();
			    } else {
			        alert('Unable to download excel.')
			    }
			};
			
			console.log('dataPicker', dataClient);
			const jsonData = {
					queryInput : editor.getValue(),
					typeExport : type,
					dataPicker : dataClient,
					infoDatabase : infoDatabase,
					row :1,
					language : $('#select-language').val(),
			}
			xhr.send(JSON.stringify(jsonData));
			
		}
		
	</script>
</body>
</html>