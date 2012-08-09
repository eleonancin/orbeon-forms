/**
 * Copyright (C) 2010 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.test;

import org.junit.Test;
import org.mockito.Mockito;
import org.orbeon.oxf.externalcontext.LocalRequest;
import org.orbeon.oxf.externalcontext.RequestAdapter;
import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.util.Connection;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SubmissionTest extends ResourceManagerTestBase {

    @Test
    public void forwardHeaders() {

        // Custom headers
        final Map<String, String[]> customHeaderValuesMap = new LinkedHashMap<String, String[]>();
        customHeaderValuesMap.put("my-stuff", new String[] { "my-value" });
        customHeaderValuesMap.put("your-stuff", new String[] { "your-value-1", "your-value-2" });

        // Create request and wrapper
        final ExternalContext.Request incomingRequest = new RequestAdapter() {

            // Fake standard headers
            final Map<String, String[]> incomingHeaderValuesMap = new LinkedHashMap<String, String[]>();
            {
                incomingHeaderValuesMap.put("user-agent", new String[] { "Mozilla 12.1" });
                incomingHeaderValuesMap.put("authorization", new String[] { "xsifj1skf3" });
                incomingHeaderValuesMap.put("host", new String[] { "localhost" });
                incomingHeaderValuesMap.put("cookie", new String[] { "JSESSIONID=4FF78C3BD70905FAB502BC989450E40C" });
            }

            @Override
            public Map<String, String[]> getHeaderValuesMap() {
                return incomingHeaderValuesMap;
            }
        };

        final ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        Mockito.when(externalContext.getRequest()).thenReturn(incomingRequest);

        // NOTE: Should instead use withExternalContext()
        PipelineContext.get().setAttribute(PipelineContext.EXTERNAL_CONTEXT, externalContext);

        final Map<String, String[]> headers =
            Connection.jBuildConnectionHeadersWithSOAP("GET", null, null, "UTF-8", customHeaderValuesMap, "cookie authorization user-agent", newIndentedLogger());

        final LocalRequest request = new LocalRequest(externalContext, null, "/orbeon", "/foo/bar", "GET", headers);

        // Test standard headers received
        final Map<String, String[]> headerValuesMap = request.getHeaderValuesMap();

        assertEquals("Mozilla 12.1", (headerValuesMap.get("user-agent"))[0]);

        assertEquals("xsifj1skf3", (headerValuesMap.get("authorization"))[0]);

        assertEquals("JSESSIONID=4FF78C3BD70905FAB502BC989450E40C", (headerValuesMap.get("cookie"))[0]);

        assertNull(headerValuesMap.get("host"));
        assertNull(headerValuesMap.get("foobar"));

        // Test custom headers received
        assertEquals("my-value", (headerValuesMap.get("my-stuff"))[0]);

        assertEquals("your-value-1", (headerValuesMap.get("your-stuff"))[0]);
        assertEquals("your-value-2", (headerValuesMap.get("your-stuff"))[1]);
    }
}
