/*
 * Copyright 2003 - 2010 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */


package org.efaps.esjp.issues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIIssues;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("9b759945-427f-47ad-9cf2-575d1cef3c5b")
@EFapsRevision("$Rev$")
public class Ticket_Base
{
    /**
     * Method for auto-complete Tickets.
     * @param _parameter Parameter as passed from the eFaps API.
     * @return Return with values.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Ticket(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String typeStr = properties.containsKey("Type") ? (String) properties.get("Type")
                                                           : CIIssues.TicketAbstract.getType().getName();
        final String keyStr = properties.containsKey("Key") ? (String) properties.get("Key") : "OID";
        final Map<String, Map<String, String>> tmpMap = new TreeMap<String, Map<String, String>>();
        if (input.length() > 0) {
            final String[] types = typeStr.split(";");
            for (final String type : types) {
                final QueryBuilder queryBldr = new QueryBuilder(Type.get(type));
                queryBldr.addWhereAttrMatchValue(CIIssues.TicketAbstract.Name, input + "*").setIgnoreCase(true);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CIIssues.TicketAbstract.Name,
                                   CIIssues.TicketAbstract.Description);
                multi.addAttribute(keyStr);
                multi.execute();
                while (multi.next()) {
                    final String name = multi.<String>getAttribute(CIIssues.TicketAbstract.Name);
                    final String descr = multi.<String>getAttribute(CIIssues.TicketAbstract.Description);
                    final String choice = name + " - " + descr.substring(0, descr.length() > 20 ? 20 : descr.length())
                        + (descr.length() > 20 ? "..." : "");
                    final Object key = multi.getAttribute(keyStr);
                    final Map<String, String> map = new HashMap<String, String>();
                    map.put("eFapsAutoCompleteKEY", key.toString());
                    map.put("eFapsAutoCompleteVALUE", name);
                    map.put("eFapsAutoCompleteCHOICE", choice);
                    tmpMap.put(choice, map);
                }
            }
        }
        final Return retVal = new Return();
        list.addAll(tmpMap.values());
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }
}
