package org.genepattern.server.genepattern;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestGpQueryParam {
    private GpQueryParam param;

    @Test
    public void fieldAndValue() {
        param=new GpQueryParam("field", "value");
        assertEquals("field=value", param.toString());
    }
    
    @Test
    public void encodeValue() {
        param=new GpQueryParam("field", "my value");
        assertEquals("field=my+value", param.toString());
    }
    
    @Test
    public void encodeGpFileUrl() {
        final String url="http://127.0.0.1:8080/gp/users/test%40email.com/all_aml%20test.cls";
        final String encodedUrl="http%3A%2F%2F127.0.0.1%3A8080%2Fgp%2Fusers%2Ftest%2540email.com%2Fall_aml%2520test.cls";
        param=new GpQueryParam("input.file", url);
        assertEquals("input.file="+encodedUrl, param.toString());
    }

    @Test
    public void fieldNoValue() {
        param=new GpQueryParam("field");
        assertEquals("field", param.toString());
    }
    
    @Test
    public void fieldNullValue() {
        param=new GpQueryParam("field", null);
        assertEquals("field", param.toString());
    }
    
    @Test
    public void emptyArg() {
        param=new GpQueryParam("");
        assertEquals("", param.toString());
    }

    @Test
    public void emptyArgNullValue() {
        param=new GpQueryParam("", null);
        assertEquals("", param.toString());
    }

    @Test
    public void emptyArgs() {
        param=new GpQueryParam("", "");
        assertEquals("=", param.toString());
    }

    @Test
    public void nullArg() {
        param=new GpQueryParam(null);
        assertEquals("", param.toString());
    }
    
    @Test
    public void nullArgs() {
        param=new GpQueryParam(null, null);
        assertEquals("", param.toString());
    }

}
