/*
 * $Id: ObjectFactoryTestCase.java 10489 2008-01-23 17:53:38Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.test.filters.FilterCounter;

/**
 * Test for MULE-4412 : selective-consumer filter is applied twice. We test that the
 * filter is only applied once in the positive case, plus make sure it doesn't get
 * filtered at all when the message does not meet the filter criteria
 */
public class Mule4412TestCase extends FunctionalTestCase
{
    private int RECEIVE_TIMEOUT_MS = 3000;
    
    protected String getConfigResources()
    {
        return "mule-4412.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // reset the counter for every test
        FilterCounter.counter.set(0);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        // reset the counter for every test
        FilterCounter.counter.set(0);
    }

    /**
     * Make sure that the message only gets filtered once
     * 
     * @throws Exception
     */
    public void testFilterOnce() throws Exception
    {
        DefaultMuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        msg.setProperty("pass", "true", PropertyScope.INBOUND);
        MuleClient client = new MuleClient(muleContext);
        client.send("vm://async", msg);
        MuleMessage reply = client.request("vm://asyncResponse", RECEIVE_TIMEOUT_MS);
        int times = FilterCounter.counter.get();
        assertTrue("did not filter one time as expected, times filtered " + times, times == 1);
        assertNotNull(reply);
        assertEquals("wrong message received : " + reply.getPayloadAsString(), TEST_MESSAGE,
            reply.getPayloadAsString());
        assertEquals("'pass' property value not correct", "true", reply.getProperty("pass", PropertyScope.INBOUND));

        // make sure there are no more messages
        assertNull(client.request("vm://asyncResponse", RECEIVE_TIMEOUT_MS));
    }

    /**
     * Make sure the message does not get filtered when the property key is incorrect
     * 
     * @throws Exception
     */
    public void testWrongPropertyKey() throws Exception
    {
        DefaultMuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        msg.setProperty("fail", "true");
        MuleClient client = new MuleClient(muleContext);
        client.send("vm://async", msg);
        MuleMessage reply = client.request("vm://asyncResponse", RECEIVE_TIMEOUT_MS);
        assertNull(reply);
        assertTrue("should not have filtered", FilterCounter.counter.get() == 0);
    }

    /**
     * Make sure the message does not get filtered when the property value is not as
     * expected
     * 
     * @throws Exception
     */
    public void testWrongPropertyValue() throws Exception
    {
        DefaultMuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        msg.setProperty("pass", "false");
        MuleClient client = new MuleClient(muleContext);
        client.send("vm://async", msg);
        MuleMessage reply = client.request("vm://asyncResponse", RECEIVE_TIMEOUT_MS);
        assertNull(reply);
        assertTrue("should not have filtered", FilterCounter.counter.get() == 0);
    }

    /**
     * Make sure the message does not get filtered at all when the expected property
     * is not defined
     * 
     * @throws Exception
     */
    public void testNoProperty() throws Exception
    {
        DefaultMuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        MuleClient client = new MuleClient(muleContext);
        client.send("vm://async", msg);
        MuleMessage reply = client.request("vm://asyncResponse", RECEIVE_TIMEOUT_MS);
        assertNull(reply);
        assertTrue("should not have filtered", FilterCounter.counter.get() == 0);
    }
}
