package com.axis.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;

@CrossOrigin(origins = "*", maxAge = 3600)
public class Upload implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
	
	public String generateTable() {
        AmazonDynamoDB database = AmazonDynamoDBClientBuilder.standard().defaultClient();
        DynamoDB db = new DynamoDB(database);
        DynamoDBMapper mapper = new DynamoDBMapper(database);
        CreateTableRequest req = mapper.generateCreateTableRequest(Model.class);
        req.setProvisionedThroughput(new ProvisionedThroughput(5L, 5L));
        database.createTable(req);
		return "Table Created";
	}
	
	public String addDetails() throws IOException {
	    URL urlForGetRequest = new URL("http://refr.mocklab.io/user");
	    String readLine = null;
	    HttpURLConnection conection = (HttpURLConnection) urlForGetRequest.openConnection();
	    conection.setRequestMethod("GET");
	    conection.setRequestProperty("userId", "a1bcdef"); // set userId its a sample here
	    int responseCode = conection.getResponseCode();

	    if (responseCode == HttpURLConnection.HTTP_OK) {
	        BufferedReader in = new BufferedReader(
	            new InputStreamReader(conection.getInputStream()));
	        StringBuffer response = new StringBuffer();
	        while ((readLine = in .readLine()) != null) {
	            response.append(readLine);
	        } 
	        in .close();
	        // print result
	        String res = response.toString();
	        String uid = res.substring(12,15) + "_"+res.substring(31,39) ;
	        String vendor = res.substring(31,39);
	        System.out.println(uid);
	        System.out.println(vendor);
	        AmazonDynamoDB database = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_2).build(); //.defaultClient();

	        DynamoDB db = new DynamoDB(database);
	         String tableName="documentDetails";
	         DynamoDBMapper mapper = new DynamoDBMapper(database);
	         Table t = db.getTable(tableName);
	         t.putItem(new Item().withPrimaryKey("applicantNo",uid).withKeyComponent("IssuedBy", vendor));
	        
	    } else {
	        System.out.println("GET NOT WORKED");
	    }
		return "Added";
	}
	
	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
	
        LambdaLogger logger = context.getLogger();
        logger.log("Loading Java Lambda handler of Proxy");
        //logger.log(String.valueOf(event.getBody().getBytes().length));
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        
        String contentType = "";
        
        try {
        	AmazonS3 s3Client = AmazonS3ClientBuilder.standard().defaultClient();

        	System.out.println( event.getQueryStringParameters().values());;
           Map<String,String> m = event.getQueryStringParameters();
           String bucketName = m.get("partner"); 
           String uid = m.get("applicantno")+"_"+m.get("partner");
           String tod = m.get("typeofdocs");
           String fileObjKeyName=(m.get("applicantno")+"_"+m.get("typeofdocs")+".png");
           
       	URL urlForGetRequest = new URL("http://refr.mocklab.io/"+tod);
	    HttpURLConnection conection = (HttpURLConnection) urlForGetRequest.openConnection();
	    conection.setRequestMethod("GET");
	    conection.setRequestProperty("userId", "a1bcdef"); // set userId its a sample here
	    int responseCode = conection.getResponseCode();

	    if (responseCode == HttpURLConnection.HTTP_OK) {
	        InputStream f = conection.getInputStream();
	        System.out.println(f);
            PutObjectRequest pu = new PutObjectRequest(bucketName, fileObjKeyName, f,null);
            pu.withCannedAcl(CannedAccessControlList.PublicRead);
            s3Client.putObject(pu);
	    }

           
                       
            AmazonDynamoDB database = AmazonDynamoDBClientBuilder.standard().defaultClient();
            DynamoDB db = new DynamoDB(database);
            DynamoDBMapper mapper = new DynamoDBMapper(database);
            String tableName="documentDetails";
            Table t = db.getTable(tableName);

            UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("applicantNo",uid)
                    .withUpdateExpression("set #na = :val1").withNameMap(new NameMap().with("#na",tod))
                    .withValueMap(new ValueMap().withString(":val1",fileObjKeyName));
            t.updateItem(updateItemSpec);
            logger.log("Item Added");
        
            response.setStatusCode(200);
            Map<String, String> responseBody = new HashMap<String, String>();
            responseBody.put("Status", "File stored in S3");
            String responseBodyString = new JSONObject(responseBody).toJSONString();
            response.setBody(responseBodyString);

        } 

        catch(AmazonServiceException e) {
            logger.log(e.getMessage());
        }
        catch(SdkClientException e) {

        	logger.log(e.getMessage());
        } 
        catch (IOException e) {

        	logger.log(e.getMessage());
        }

        logger.log(response.toString());
        return response;
	}

}