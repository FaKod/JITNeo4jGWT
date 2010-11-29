package com.mySampleApplication.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Timer;
import com.mySampleApplication.client.jit.ForceDirected;
import com.mySampleApplication.client.jit.NativeJITGraph;
import com.mySampleApplication.client.jit.RGraph;

import java.util.*;

/**
 * Entry point classes define <code>onModuleLoad()</code>
 */
public class MySampleApplication implements EntryPoint {

    static class JITGraph {

        /**
         * store for the graph array JSON object
         */
        JSONArray graphArray = null;

        /**
         * converts the given String to a JavScript Object
         * @param json JSON as String
         * @return JavaScriptObject
         */
        native JavaScriptObject buildJavaScriptObject(String json) /*-{
    return eval('(' + json + ')');
}-*/;

        /**
         * extracts the id from a given URL. removes last " as well
         */
        String extractId(String url) {
            return url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("\""));
        }

        /**
         * filles the given adjacencies Array with the given relation data
         * @param adjacenciesArray
         * @param nodeFrom
         * @param nodeTo
         * @param color
         * @param nodeAttr
         */
        void fillAdjacenciesArray(JSONArray adjacenciesArray, String nodeFrom, String nodeTo, String color, String nodeAttr) {
            JSONObject adjacenciesObject = new JSONObject();
            adjacenciesObject.put("nodeTo", new JSONString(nodeTo));
            adjacenciesObject.put("nodeFrom", new JSONString(nodeFrom));

            JSONObject dataObject = new JSONObject();
            dataObject.put("$color", new JSONString(color));
            dataObject.put("nodeAttr", new JSONString(nodeAttr));
            adjacenciesObject.put("data", dataObject);
            adjacenciesArray.set(adjacenciesArray.size(), adjacenciesObject);
        }

        /**
         * tests if the Node is present.
         * @param id
         * @return Node or null if Node is not existent
         */
        JSONObject hasNode(String id) {
            for (int i = 0; i < graphArray.size(); i++)
                if (graphArray.get(i).isObject().get("id").toString().equalsIgnoreCase("\"" + id + "\""))
                    return graphArray.get(i).isObject();
            return null;
        }

        /**
         * returns the Node if it is existent or creates new one
         * @param id
         * @param name
         * @param color
         * @param type
         * @param dim
         * @return JSONObject as new or old Node object
         */
        JSONObject getNode(String id, String name, String color, String type, int dim) {
            JSONObject nodeObject = hasNode(id);
            if (nodeObject == null) {
                nodeObject = new JSONObject();

                JSONObject dataObject = new JSONObject();
                dataObject.put("$color", new JSONString(color));
                dataObject.put("$type", new JSONString(type));
                dataObject.put("$dim", new JSONNumber(dim));
                nodeObject.put("data", dataObject);

                nodeObject.put("id", new JSONString(id));
                nodeObject.put("name", new JSONString(name));
                graphArray.set(graphArray.size(), nodeObject);
            }
            return nodeObject;
        }

        /**
         * returns a existing Adjacencies Array or creates new one.
         * @param node
         * @return
         */
        JSONArray getAdjacenciesArray(JSONObject node) {
            JSONValue adjacenciesArray = node.get("adjacencies");
            if (adjacenciesArray == null) {
                adjacenciesArray = new JSONArray();
                node.put("adjacencies", adjacenciesArray);
            }
            return (JSONArray) adjacenciesArray;
        }

        /**
         * main method to fill the graph structure
         * @param id Id from what Node to start
         * @param callBack call back method after load finished
         */
        public void getPathREST(final String id, final AsyncCallback<String> callBack) {
            graphArray = new JSONArray();
            
            String url = "neo4j-rest/node/" + id + "/traverse/path";

            RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
            builder.setHeader("Accept", "application/json");
            builder.setHeader("Content-Type", "application/json");

            try {
                builder.sendRequest("{\"order\":\"depth first\", \"max depth\": 4, \"uniqueness\": \"node path\"}", new RequestCallback() {
                    public void onError(Request request, Throwable exception) {
                    }

                    public void onResponseReceived(Request request, Response response) {
                        if (200 == response.getStatusCode()) {

                            ArrayList<Relation> listOfRelations = new ArrayList<Relation>();
                            HashSet<String> listOfNodes = new HashSet<String>();
                            Set<String> blockDoubleRelations = new HashSet<String>();

                            JSONArray json = new JSONArray(buildJavaScriptObject(response.getText()));
                            for (int i = 0; i < json.size(); i++) {

                                JSONArray nodesArray = json.get(i).isObject().get("nodes").isArray();
                                JSONArray relationArray = json.get(i).isObject().get("relationships").isArray();
                                for (int n = 0; n < nodesArray.size() - 1; n++) {
                                    String n1 = extractId(nodesArray.get(n).isString().toString());
                                    String n2 = extractId(nodesArray.get(n + 1).isString().toString());
                                    String relation = extractId(relationArray.get(n).toString());

                                    if (!blockDoubleRelations.contains(relation)) {
                                        blockDoubleRelations.add(relation);
                                        listOfRelations.add(new Relation(n1, n2, relation));
                                    }
                                    listOfNodes.add(n1);
                                    listOfNodes.add(n2);
                                }
                            }
                            waitForLoadingNodesAndLoadRelations(listOfNodes, listOfRelations);
                            loadAllNodes(listOfNodes);
                            waitForLoadingRelationsAndCallCallBack(listOfRelations, callBack);
                        }
                    }
                });
            } catch (RequestException e) {
            }
        }

        /**
         * initiates to load all Nodes
         * @param listOfNodes
         */
        void loadAllNodes(HashSet<String> listOfNodes) {
            Set<String> listOfNodesClone = (Set<String>) listOfNodes.clone();
            for (String fetchId : listOfNodesClone)
                loadNode(fetchId, listOfNodes);
        }

        /**
         * load one Node and removes the Id from the listOfNodes
         * @param fetchId
         * @param listOfNodes
         */
        void loadNode(final String fetchId, final Set<String> listOfNodes) {
            if (fetchId.equalsIgnoreCase("0")) {
                listOfNodes.remove(fetchId);
                return;
            }

            String url = "neo4j-rest/node/" + fetchId;

            RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
            builder.setHeader("Accept", "application/json");
            try {
                builder.sendRequest(null, new RequestCallback() {
                    public void onError(Request request, Throwable exception) {
                    }

                    public void onResponseReceived(Request request, Response response) {
                        if (200 == response.getStatusCode()) {

                            JSONObject json = new JSONObject(buildJavaScriptObject(response.getText()));
                            String nodeName = json.get("data").isObject().get("name").toString();
                            getNode(fetchId, nodeName, "#C74243", "star", 30);
                            listOfNodes.remove(fetchId);
                        }
                    }
                });
            } catch (RequestException e) {
            }
        }

        /**
         * waites till all nodes are loaded and initiates loading of all relations
         * @param listOfNodes
         * @param listOfRelations
         */
        void waitForLoadingNodesAndLoadRelations(final Set<String> listOfNodes, final ArrayList<Relation> listOfRelations) {
            Timer t = new Timer() {
                @Override
                public void run() {
                    if (!listOfNodes.isEmpty()) {
                        waitForLoadingNodesAndLoadRelations(listOfNodes, listOfRelations);
                        return;
                    }
                    loadAllRelations(listOfRelations);
                }
            };
            t.schedule(500);
        }

        /**
         * waits till all relations are loaded and calls the "final!" call back
         * @param listOfRelations
         * @param callBack
         */
        void waitForLoadingRelationsAndCallCallBack(final ArrayList<Relation> listOfRelations, final AsyncCallback<String> callBack) {
            Timer t = new Timer() {
                @Override
                public void run() {
                    if (!listOfRelations.isEmpty()) {
                        waitForLoadingRelationsAndCallCallBack(listOfRelations, callBack);
                        return;
                    }
                    callBack.onSuccess(graphArray.toString());
                }
            };
            t.schedule(500);
        }

        /**
         * initiates the load of all relations
         * @param listOfRelations
         */
        void loadAllRelations(ArrayList<Relation> listOfRelations) {
            ArrayList<Relation> listOfRelationsClone = (ArrayList<Relation>) listOfRelations.clone();
            for (Relation rel : listOfRelationsClone)
                loadRelation(rel, listOfRelations);
        }

        /**
         * loads one relation and removes it from listOfRelations
         * @param rel
         * @param listOfRelations
         */
        void loadRelation(final Relation rel, final ArrayList<Relation> listOfRelations) {
            final JSONObject n1o = hasNode(rel.n1);
            final JSONObject n2o = hasNode(rel.n2);
            final String n1 = rel.n1;
            final String n2 = rel.n2;

            String url = "neo4j-rest/relationship/" + rel.relation;

            RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
            builder.setHeader("Accept", "application/json");
            try {
                builder.sendRequest(null, new RequestCallback() {
                    public void onError(Request request, Throwable exception) {
                    }

                    public void onResponseReceived(Request request, Response response) {
                        if (200 == response.getStatusCode()) {

                            JSONObject json = new JSONObject(buildJavaScriptObject(response.getText()));
                            String relationType = json.get("type").toString();
                            JSONArray adjacenciesArrayN1 = getAdjacenciesArray(n1o);

                            fillAdjacenciesArray(adjacenciesArrayN1, n1, n2, "#C74243", relationType);
                            listOfRelations.remove(rel);
                        }
                    }
                });
            } catch (RequestException e) {
            }
        }

    }


    /**
     * static JITGraph objects (sufficient here)
     */
    static JITGraph jitGraph = new JITGraph();

    private NativeJITGraph graph;

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        VerticalPanel mainPanel = new VerticalPanel();
        graph = new ForceDirected();

        final Button button = new Button("Load Path");

        button.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                jitGraph.getPathREST("3", new MyAsyncCallback(graph));
            }
        });

        mainPanel.add((Widget)graph);
		mainPanel.add(button);

        RootPanel.get("mainPanel").add(mainPanel);
    }

    /**
     * callback class which displays the JSON object
     */
    private static class MyAsyncCallback implements AsyncCallback<String> {
        private NativeJITGraph graph;

        public MyAsyncCallback(NativeJITGraph graph) {
            this.graph = graph;
        }

        public void onSuccess(String data) {
            graph.loadData(data);
        }

        public void onFailure(Throwable throwable) {
        }
    }
}
