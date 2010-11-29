package com.mySampleApplication.client.jit;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;

public class RGraph extends Widget implements NativeJITGraph {

	private final static String CSS_CLASS = "infovis-RGraph";
	private static int count = 1;
	private final String graphId;
	private JavaScriptObject nativeGraph;
	private final Element graphContainer;

	public RGraph() {
		graphId = CSS_CLASS + "-" + count++;

		graphContainer = DOM.createElement("div");
		DOM.setElementProperty(graphContainer, "id", graphId);
		setElement(graphContainer);
		setStyleName(CSS_CLASS);
	}

	@Override
	protected void onLoad() {
		nativeGraph = createGraph(graphId);
		super.onLoad();
	}

	private native JavaScriptObject createGraph(String injectedDivId)/*-{
		//init RGraph
		var rgraph = new $wnd.$jit.RGraph({
		    //Where to append the visualization
		    injectInto: injectedDivId,
		    //Optional: create a background canvas that plots
		    //concentric circles.
		    background: {
		      CanvasStyles: {
		        strokeStyle: '#555'
		      }
		    },
		    //Add navigation capabilities:
		    //zooming by scrolling and panning.
		    Navigation: {
		      enable: true,
		      panning: true,
		      zooming: 10
		    },
		    //Set Node and Edge styles.
		    Node: {
		        color: '#ddeeff'
		    },

		    Edge: {
		      color: '#C17878',
		      lineWidth:1.5
		    },

		    onBeforeCompute: function(node){

		    },

		    onAfterCompute: function(){
		    },
		    //Add the name of the node in the correponding label
		    //and a click handler to move the graph.
		    //This method is called once, on label creation.
		    onCreateLabel: function(domElement, node){
		    	domElement.innerHTML = node.name;
		        domElement.onclick = function(){
		            rgraph.onClick(node.id);
		        };
		    },
		    //Change some label dom properties.
		    //This method is called each time a label is plotted.
		    onPlaceLabel: function(domElement, node){
		        var style = domElement.style;
		        style.display = '';
		        style.cursor = 'pointer';

		        if (node._depth <= 1) {
		            style.fontSize = "0.8em";
		            style.color = "#ccc";

		        } else if(node._depth == 2){
		            style.fontSize = "0.7em";
		            style.color = "#494949";

		        } else {
		            style.display = 'none';
		        }

		        var left = parseInt(style.left);
		        var w = domElement.offsetWidth;
		        style.left = (left - w / 2) + 'px';
		    }
		});

		return rgraph;
	}-*/;

	private native JavaScriptObject toJavaScriptObject(String json) /*-{
		return eval(json);
	}-*/;
	
	public void loadData(String data) {
		loadData(toJavaScriptObject(data));
	}

	public void loadData(JavaScriptObject data) {
		loadData(nativeGraph, data);
	}

	private native void loadData(JavaScriptObject nativeGraph,
			JavaScriptObject data)/*-{
		//load JSON data
		nativeGraph.loadJSON(data);
		//trigger small animation
		nativeGraph.graph.eachNode(function(n) {
		  var pos = n.getPos();
		  pos.setc(-200, -200);
		});
		nativeGraph.compute('end');
		nativeGraph.fx.animate({
		  modes:['polar'],
		  duration: 2000
		});
	}-*/;
}
