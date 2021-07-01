package com.axis.demo;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class test {
	
	 @InjectMocks
	 Upload d;

	 @Test
	public void test() throws IOException
	{
		String str  = d.addDetails();
		System.out.println(str);
		assertEquals(str, "Added");
	
	}
	
}
