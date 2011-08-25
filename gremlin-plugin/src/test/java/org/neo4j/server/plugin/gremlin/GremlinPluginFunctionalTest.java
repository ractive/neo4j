/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.plugin.gremlin;

import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.rest.DocsGenerator;
import org.neo4j.server.rest.JSONPrettifier;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.test.GraphDescription;
import org.neo4j.test.GraphDescription.Graph;
import org.neo4j.test.GraphHolder;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.neo4j.test.TestData;
import org.neo4j.test.TestData.Title;

public class GremlinPluginFunctionalTest implements GraphHolder
{
    private static final String ENDPOINT = "http://localhost:7474/db/data/ext/GremlinPlugin/graphdb/execute_script";
    private static org.neo4j.test.ImpermanentGraphDatabase graphdb;
    public @Rule
    TestData<Map<String, Node>> data = TestData.producedThrough( GraphDescription.createGraphFor(
            this, true ) );

    public @Rule
    TestData<DocsGenerator> gen = TestData.producedThrough( DocsGenerator.PRODUCER );
    private static WrappingNeoServerBootstrapper server;

    /**
     * Send a Gremlin Script, URL-encoded with UTF-8 encoding, e.g.
     * the equivalent of the Gremlin Script `i = g.v(1);i.outE.inV`
     */
    @Test
    @Title("Send a Gremlin Script - URL encoded")
    @Documented
    @Graph( value = { "I know you" } )
    public void testGremlinPostURLEncoded() throws UnsupportedEncodingException
    {
        String response = gen.get()
        .expectedStatus( Status.OK.getStatusCode() )
        .payload( "script=" + URLEncoder.encode( "i = g.v("+data.get().get( "I" ).getId() +");i.out", "UTF-8") )
        .payloadType( MediaType.APPLICATION_FORM_URLENCODED_TYPE )
        .post( ENDPOINT )
        .entity();
        assertTrue(response.contains( "you" ));
    }



    /**
     * Send a Gremlin Script, URL-encoded with UTF-8 encoding, e.g.
     * the equivalent of the Gremlin Script `g.v(me).outE.inV`
     * with additional parameters as { "me" : 123 }.
     */
    //TODO: enable this. Needs SimpleJSON as dependency to ParameterList and Server-API @Test
    @Title("Send a Gremlin Script with variables in a JSON Map - URL encoded")
    @Documented
    @Graph( value = { "I know you" } )
    public void testGremlinPostWithVariablesURLEncoded() throws UnsupportedEncodingException
    {
        final String script = "g.v(me).out";
        final String params = "{ \"me\" : "+data.get().get("I").getId()+" }";
        String response = gen.get()
        .expectedStatus(Status.OK.getStatusCode())
        .payload( "script=" + URLEncoder.encode(script, "UTF-8")+
                "&params=" + URLEncoder.encode(params, "UTF-8")
        )

        .payloadType( MediaType.APPLICATION_FORM_URLENCODED_TYPE )
        .post( ENDPOINT )
        .entity();
        assertTrue(response.contains( "you" ));
    }

    /**
     * Send a Gremlin Script, as JSON payload and additional parameters
     */
    @Test
    @Title("Send a Gremlin Script with variables in a JSON Map")
    @Documented
    @Graph( value = { "I know you" } )
    public void testGremlinPostWithVariablesAsJson() throws UnsupportedEncodingException
    {
        final String script = "g.v(me).out";
        final String params = "{ \"me\" : "+data.get().get("I").getId()+" }";
        final String payload = String.format("{ \"script\" : \"%s\", \"params\" : %s }", script, params);
        String response = gen.get()
        .expectedStatus( Status.OK.getStatusCode() )
                .payload( JSONPrettifier.parse( payload ) )
        .payloadType( MediaType.APPLICATION_JSON_TYPE )
        .post( ENDPOINT )
        .entity();
        assertTrue(response.contains( "you" ));
    }

    /**
     * Import a graph form a http://graphml.graphdrawing.org/[GraphML] file
     * can be achieved through the Gremlin GraphMLReader.
     * The following script imports a small graphml file from an URL into Neo4j
     * and then returns a list of all nodes in the graph.
     */
    @Test
    @Documented
    @Title( "Load a sample graph" )
    public void testGremlinImportGraph() throws UnsupportedEncodingException
    {
        String payload = "{\"script\":\"" +
        "g.loadGraphML('https://raw.github.com/neo4j/gremlin-plugin/master/src/data/graphml1.xml');" +
        "g.V\"}";
        String response = gen.get()
        .expectedStatus( Status.OK.getStatusCode() )
                .payload( JSONPrettifier.parse( payload ) )
        .payloadType( MediaType.APPLICATION_JSON_TYPE )
        .post( ENDPOINT )
        .entity();
        assertTrue(response.contains( "you" ));
        assertTrue(response.contains( "him" ));
    }


    /**
     * To set variables in the bindings for the Gremlin Script
     * Engine on the server, you can include a +params+ parameter
     * with a String representing a JSON map of variables to set
     * to initial values. These can then be accessed as normal
     * variables within the script.
     */
    @Test
    @Documented
    @Title("Set script variables")
    public void setVariables() throws UnsupportedEncodingException
    {
        String payload = "{\"script\":\"meaning_of_life\","
            + "\"params\":{\"meaning_of_life\" : 42.0}}";
        String response = gen.get()
        .expectedStatus( Status.OK.getStatusCode() )
                .payload( JSONPrettifier.parse( payload ) )
        .payloadType( MediaType.APPLICATION_JSON_TYPE )
        .post( ENDPOINT )
        .entity();
        assertTrue(response.contains( "42.0" ));
    }


    /**
     * The following script returns a sorted list
     * of all nodes connected via outgoing relationships
     * to node 1, sorted by their `name`-property.
     */
    @Test
    @Documented
    @Title("Sort a result using raw Groovy operations")
    @Graph( value = { "I know you", "I know him" } )
    public void testSortResults() throws UnsupportedEncodingException
    {
        String payload = "{\"script\":\"g.v(" + data.get()
        .get( "I" )
        .getId() + ").out.sort{it.name}.toList()\"}";
        String response = gen.get()
        .expectedStatus( Status.OK.getStatusCode() )
        .payload( JSONPrettifier.parse( payload ) )
        .payloadType( MediaType.APPLICATION_JSON_TYPE )
        .post( ENDPOINT )
        .entity();
        assertTrue(response.contains( "you" ));
        assertTrue(response.contains( "him" ));
        assertTrue(response.indexOf( "you" ) > response.indexOf( "him" ));
    }
    
    /**
     * The following script returns a sorted list
     * of all nodes connected via outgoing relationships
     * to node 1, sorted by their `name`-property.
     */
    @Test
    @Title("Return paths from a Gremlin script")
    @Documented
    @Graph( value = { "I know you", "I know him" } )
    public void testScriptWithPaths()
    {
        String payload = "{\"script\":\"g.v(" + data.get()
        .get( "I" )
        .getId() + ").out.name.paths\"}";
        String response = gen.get()
        .expectedStatus( Status.OK.getStatusCode() )
        .payload( JSONPrettifier.parse( payload ) )
        .payloadType( MediaType.APPLICATION_JSON_TYPE )
        .post( ENDPOINT )
        .entity();
        System.out.println(response);
        assertTrue(response.contains( ", you]" ));
    }


    @Test
    public void testLineBreaks() throws UnsupportedEncodingException
    {
        //be aware that the string is parsed in Java before hitting the wire,
        //so escape the backslash once in order to get \n on the wire.
        String payload = "{\"script\":\"1\\n2\"}";
        String response = gen.get()
        .expectedStatus( Status.OK.getStatusCode() )
        .payload( JSONPrettifier.parse( payload ) )
        .payloadType( MediaType.APPLICATION_JSON_TYPE )
        .post( ENDPOINT )
        .entity();
        assertTrue(response.contains( "2" ));
    }

    
    /**
     * To send a Script JSON encoded, set the payload Content-Type Header.
     * In this example, find all the things that my friends like,
     * and return a table listing my friends by their name,
     * and the names of the things they like in a table with two columns,
     * ignoring the third named step variable +I+.
     * Remember that everything in Gremlin is an iterator - in order
     * to populate the result table +t+, iterate through the pipes with
     * +>> -1+.
     */
    @Test
    @Title("Send a Gremlin Script - JSON encoded with table results")
    @Documented
    @Graph( value = { "I know Joe", "I like cats", "Joe like cats", "Joe like dogs" } )
    public void testGremlinPostJSONWithTableResult()
    {
        String payload = "{\"script\":\"i = g.v("
            + data.get()
            .get( "I" )
            .getId()
            + ");"
            + "t= new Table();"
            + "i.as('I').out('know').as('friend').out('like').as('likes').table(t,['friend','likes']){it.name}{it.name} >> -1;t;\"}";
        String response = gen.get()
        .expectedStatus( Status.OK.getStatusCode() )
                .payload( JSONPrettifier.parse( payload ) )
        .payloadType( MediaType.APPLICATION_JSON_TYPE )
        .post( ENDPOINT )
        .entity();
        System.out.println(response);
        //there is nothing returned at all.
        assertTrue(response.contains( "cats" ));
    }
    @BeforeClass
    public static void startDatabase()
    {
        graphdb = new ImpermanentGraphDatabase("target/db"+System.currentTimeMillis());
    }

    @AfterClass
    public static void stopDatabase()
    {
    }

    @Override
    public GraphDatabaseService graphdb()
    {
        return graphdb;
    }

    @Before
    public void startServer() {
        server = new WrappingNeoServerBootstrapper(
                graphdb );
        server.start();
    }

    @After
    public void shutdownServer() {
        server.stop();
    }
}