package sql.generator.hackathon.controller;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.jsqlparser.JSQLParserException;
import sql.generator.hackathon.create.CreateData;
import sql.generator.hackathon.model.ColumnInfo;
import sql.generator.hackathon.model.CreateObject;
import sql.generator.hackathon.model.FlowDataQuery;
import sql.generator.hackathon.model.ObjectGenate;
import sql.generator.hackathon.model.ParseObject;
import sql.generator.hackathon.model.ViewQuery;
import sql.generator.hackathon.model.createdata.constant.Constant;
import sql.generator.hackathon.service.ExcelExporter;
import sql.generator.hackathon.service.ExecuteDBSQLServer;
import sql.generator.hackathon.service.ServiceDatabase;
import sql.generator.hackathon.service.ServiceParse;
import sql.generator.hackathon.service.createdata.ServiceCreateData;

@Controller
public class GenController {

	@Autowired
	ServiceDatabase serviceDatabase;

	@Autowired
	ExecuteDBSQLServer executeDBServer;

	@Autowired
	private ServiceParse serviceParse;
	
//	@Autowired
//	private CreateData createData;
	
	@Autowired
	private ServiceCreateData serviceCreateData;
	
	@Autowired
	private ExcelExporter excelExporter;

	@GetMapping(value = "/")
	public String index() {
		return "index";
	}

	@PostMapping("/uploadFile")
	public @ResponseBody String singleFileUpload(@RequestParam("file") MultipartFile file)
			throws IOException, JSQLParserException {
		String query = new BufferedReader(new InputStreamReader(file.getInputStream())).lines()
				.collect(Collectors.joining("\n"));
		ViewQuery viewQuery = ViewQuery.builder().listTable(serviceParse.getListTableByStatement(query)).query(query)
				.build();
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(viewQuery);
	}

	@GetMapping(value = "/updateQuery")
	public @ResponseBody String updateQuery(@RequestParam String query, @RequestParam String url, @RequestParam String schema,
			@RequestParam String user, @RequestParam String pass, @RequestParam String typeConnection) throws Exception {
		FlowDataQuery response = new FlowDataQuery();
		try {
			if ("No database".equals(typeConnection)) {
				response.listInfo = serviceParse.getColumnInfoView(query);
			} else {
				executeDBServer.connectDB(typeConnection, url, schema, user, pass);
				List<String> listTable = serviceParse.getListTableByStatement(query);
				response.listInfo = executeDBServer.getDataDisplay(schema, listTable);
			}
			response.flows = serviceParse.parseSelectStatement(query);
		} catch (JSQLParserException e) {
			response.message = "Query is unvalid";
		} finally {
			executeDBServer.disconnectDB();
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}

	@GetMapping(value = "/testConnection")
	public @ResponseBody String testConnection(@RequestParam String url, @RequestParam String schema,
			@RequestParam String user, @RequestParam String pass, @RequestParam String tableSelected) throws Exception {
		boolean isConnect = executeDBServer.connectDB(tableSelected, url, schema, user, pass);
		ObjectMapper mapper = new ObjectMapper();
		if (isConnect) {
			System.out.println(isConnect);
			executeDBServer.disconnectDB();
			return mapper.writeValueAsString("Connect success");
		}
		return mapper.writeValueAsString("Connect error");
	}

	@PostMapping(value = "/generate")
	public ResponseEntity<InputStreamResource> generate(@RequestBody ObjectGenate objectGenate) throws Exception {
		Map<String, List<List<ColumnInfo>>> dataPick = new HashMap<>();
		objectGenate.dataPicker.forEach(x -> {
			dataPick.put(x.getTableName(), x.getListColumnInfo());
		});

		String type = objectGenate.infoDatabase.getType();
		
		try {
			if (!type.equalsIgnoreCase(Constant.STR_NO_CONNECTION)) {
				executeDBServer.connectDB(objectGenate.infoDatabase.getType(), objectGenate.infoDatabase.getUrl(), 
						objectGenate.infoDatabase.getSchema(), objectGenate.infoDatabase.getUser(), 
						objectGenate.infoDatabase.getPassword());
			}
			ParseObject parseObject = serviceParse.parseSelectStatement(objectGenate.queryInput);
//			Map<String, TableSQL> fullTableInfo = serviceParse.getColumnInfo(objectGenate.queryInput);
//			if (type.equalsIgnoreCase("No database")) {
//				createData.init(type, null, objectGenate.infoDatabase.getSchema(), 
//						serviceParse.getListTableByStatement(objectGenate.getQueryInput()),
//						parseObject.getListTableSQL(), parseObject.getMappingKey(), fullTableInfo);
//			}  else {
//				createData.init(type, executeDBServer, objectGenate.infoDatabase.getSchema(), 
//						serviceParse.getListTableByStatement(objectGenate.getQueryInput()),
//						parseObject.getListTableSQL(), parseObject.getMappingKey(), null);
//			}
			
			
//			CreateObject createObj = createData.multipleCreate(dataPick, objectGenate.row, false);
//			Map<String, List<List<ColumnInfo>>> response = createObj.listData;
			
			// Ver2 create data
			CreateObject createObj = serviceCreateData.process(executeDBServer, objectGenate, parseObject, dataPick, false, objectGenate.getLanguage());
			Map<String, List<List<ColumnInfo>>> response = createObj.listData;
			
			ByteArrayInputStream resource ;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			HttpHeaders header = new HttpHeaders();
			MediaType typeMedia;
			if (objectGenate.typeExport.equals("SQL")) {
				resource = new ByteArrayInputStream(excelExporter.outputFieSql(serviceParse.dataToSqlInsert(response)));
				header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data.txt");
				typeMedia = MediaType.parseMediaType("text/plain");
			} else {
				//list markcolor
				List<String> listMarkColor = createObj.listMarkColor;
//				System.out.println(listMarkColor.toString());
				HSSFWorkbook workbook = excelExporter.createEex(response, listMarkColor);
		        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data.xls");
		        workbook.write(bos);
		        typeMedia = MediaType.parseMediaType("application/vnd.ms-excel");
		        resource = new ByteArrayInputStream(bos.toByteArray());
			}
			return ResponseEntity.ok()
	                .headers(header)
	                .contentType(typeMedia)
	                .body(new InputStreamResource(resource));
		} catch (JSQLParserException e) {
			// sql is not valid
			e.printStackTrace();
		} finally {
			if (!type.equalsIgnoreCase("No database")) {
				executeDBServer.disconnectDB();
			}
		}
		System.out.println(dataPick.toString());
		return null;
	}
	
	
}
