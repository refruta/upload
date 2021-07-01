package com.axis.demo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "documentDetails")
public class Model {
    private String ApplicantNo;
 
 
    @DynamoDBHashKey
    public String getApplicantNo() {
		return ApplicantNo;
	}

	public void setApplicantNo(String applicantNo) {
		ApplicantNo = applicantNo;
	}
	
	@DynamoDBAttribute(attributeName = "AadharCard" )
	private String AadharCard;


	public String getAadharCard() {
		return AadharCard;
	}

	public void setAadharCard(String aadharCard) {
		AadharCard = aadharCard;
	}
    }
