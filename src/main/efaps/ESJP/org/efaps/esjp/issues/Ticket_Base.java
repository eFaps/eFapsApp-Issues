/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.issues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIFormIssues;
import org.efaps.esjp.ci.CIIssues;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("9b759945-427f-47ad-9cf2-575d1cef3c5b")
@EFapsApplication("eFapsApp-Issues")
public class Ticket_Base
{

    /**
     * Method to create a ticket.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return Return with values.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Create create = new Create()
        {

            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final Insert _insert)
                throws EFapsException
            {
                add2basicInsert4create(_parameter, _insert);
            }

            @Override
            public void connect(final Parameter _parameter,
                                final Instance _instance)
                throws EFapsException
            {
                connect4create(_parameter, _instance);
            }
        };
        return create.execute(_parameter);
    }

    /**
     * To allow easy override.
     *
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _insert       Insert the values can be added to
     * @throws EFapsException on error
     */
    protected void connect4create(final Parameter _parameter,
                                  final Instance _instance)
        throws EFapsException
    {
        final Instance projectInst = Instance.get(_parameter
                        .getParameterValue(CIFormIssues.Issues_TicketForm.project.name));
        if (projectInst.isValid()) {
            // CIProjects.Issues_Ticket2ProjectService
            final Insert insert = new Insert(UUID.fromString("a685e79a-c320-4dcb-ad3f-6bbbfecc5850"));
            insert.add("FromLink", _instance.getId());
            insert.add("ToLink", projectInst.getId());
            insert.execute();
        }
    }

    /**
     * Add additional values to the basic insert, prior to execution.
     * To allow easy override.
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _insert       Insert the values can be added to
     * @throws EFapsException on error
     */
    protected void add2basicInsert4create(final Parameter _parameter,
                                          final Insert _insert)
        throws EFapsException
    {
        final Instance projectInst = Instance.get(_parameter
                        .getParameterValue(CIFormIssues.Issues_TicketForm.project.name));

        if (projectInst.isValid()) {
            final PrintQuery print = new PrintQuery(projectInst);
            print.addAttribute("Contact");
            print.execute();

            _insert.add(CIIssues.Ticket.ContactLink, print.<Long>getAttribute("Contact"));

            // Issues_Configuration
            if (SystemConfiguration.get(UUID.fromString("1aadb5f5-c762-46ce-8bb0-071605f382e1"))
                            .getAttributeValueAsBoolean("Ticket_AutomaticNumbering")) {
                // Issues_TicketSequence
                final NumberGenerator numGen = NumberGenerator.get(
                                UUID.fromString("5ea2e67b-7155-499c-b160-ae4bc2fc2f7e"));
                _insert.add(CIIssues.Ticket.Name, numGen.getNextVal());
            }
        }
    }

    /**
     * Method for auto-complete Tickets.
     *
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
