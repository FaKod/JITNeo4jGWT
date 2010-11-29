package com.mySampleApplication.client.jit;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;

public class ForceDirected extends Widget implements NativeJITGraph {

    private final static String CSS_CLASS = "infovis-ForceDirected";
    private static int count = 1;
    private final String graphId;
    private JavaScriptObject nativeGraph;
    private final Element graphContainer;

    public ForceDirected() {
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
    var labelType,
useGradients,
nativeTextSupport,
animate;

 (function() {
    var ua = navigator.userAgent,
    iStuff = ua.match(/iPhone/i) || ua.match(/iPad/i),
    typeOfCanvas = typeof HTMLCanvasElement,
    nativeCanvasSupport = (typeOfCanvas == 'object' || typeOfCanvas == 'function'),
    textSupport = nativeCanvasSupport
    && (typeof document.createElement('canvas').getContext('2d').fillText == 'function');
    //I'm setting this based on the fact that ExCanvas provides text support for IE
    //and that as of today iPhone/iPad current text support is lame
    labelType = (!nativeCanvasSupport || (textSupport && !iStuff)) ? 'Native': 'HTML';
    nativeTextSupport = labelType == 'Native';
    useGradients = nativeCanvasSupport;
    animate = !(iStuff || !nativeCanvasSupport);
})();
		//init RGraph
		var fd = new $wnd.$jit.ForceDirected({
        //id of the visualization container
        injectInto: injectedDivId,
        //Enable zooming and panning
        //by scrolling and DnD
        Navigation: {
            enable: true,
            //Enable panning events only if we're dragging the empty
            //canvas (and not a node).
            panning: 'avoid nodes',
            zooming: 10
            //zoom speed. higher is more sensible
        },
        // Change node and edge styles such as
        // color and width.
        // These properties are also set per node
        // with dollar prefixed data-properties in the
        // JSON structure.
        Node: {
            overridable: true
        },
        Edge: {
            overridable: true,
            color: '#FFFFFF',
            lineWidth: 1
        },
        //Native canvas text styling
        Label: {
            type: labelType,
            //Native or HTML
            size: 10,
            style: 'bold'
        },
        //Add Tips
        Tips: {
            enable: true,
            onShow: function(tip, node) {
                //count connections
                var count = 0;
                node.eachAdjacency(function() {
                    count++;
                });
                //display node info in tooltip
                tip.innerHTML = "<div class=\"tip-title\">Node: " + node.name + "</div>"
                + "<div class=\"tip-text\"><b>Relations:</b> " + count + "</div>";
            }
        },
        // Add node events
        Events: {
            enable: true,
            //Change cursor style when hovering a node
            onMouseEnter: function() {
                fd.canvas.getElement().style.cursor = 'move';
            },
            onMouseLeave: function() {
                fd.canvas.getElement().style.cursor = '';
            },
            //Update node positions when dragged
            onDragMove: function(node, eventInfo, e) {
                var pos = eventInfo.getPos();
                node.pos.setc(pos.x, pos.y);
                fd.plot();
            },
            //Implement the same handler for touchscreens
            onTouchMove: function(node, eventInfo, e) {
                $wnd.$jit.util.event.stop(e);
                //stop default touchmove event
                this.onDragMove(node, eventInfo, e);
            },
            //Add also a click handler to nodes
            onClick: function(node) {
                //set final styles
                fd.graph.eachNode(function(n) {
                    if (n.id != node.id) delete n.selected;
                    n.setData('dim', 20, 'end');
                    n.eachAdjacency(function(adj) {
                        adj.setDataset('end', {
                            lineWidth: 0.4,
                            color: '#23a4ff'
                        });
                    });
                });
                if (!node.selected) {
                    node.selected = true;
                    node.setData('dim', 35, 'end');
                    node.eachAdjacency(function(adj) {
                        adj.setDataset('end', {
                            lineWidth: 6,
                            color: '#23a4ff'
                        });
                    });
                } else {
                    delete node.selected;
                }
                //trigger animation to final styles
                fd.fx.animate({
                    modes: ['node-property:dim',
                    'edge-property:lineWidth:color'],
                    duration: 500
                });
                // Build the right column relations list.
                // This is done by traversing the clicked node connections.
                var html = "<h4>" + node.name + "</h4><b> has relation:</b><ul><li>",
                list = [];
                node.eachAdjacency(function(adj) {
                    if (adj.getData('alpha')) list.push(adj.data["nodeAttr"] + " to node:<br>" + adj.nodeTo.name + "<br>.");
                });
                //append connections information
                $wnd.$jit.id('inner-details').innerHTML = html + list.join("</li><li>") + "</li></ul>";
            }
        },
        //Number of iterations for the FD algorithm
        iterations: 500,
        //Edge length
        levelDistance: 130,
        // Add text to the labels. This method is only triggered
        // on label creation and only for DOM labels (not native canvas ones).
        onCreateLabel: function(domElement, node) {
            //domElement.innerHTML = node.name;
            //var style = domElement.style;
            //style.fontSize = "0.8em";
            //style.color = "#ddd";

        },
        // Change node styles when DOM labels are placed
        // or moved.
        onPlaceLabel: function(domElement, node) {
            var style = domElement.style;
            var left = parseInt(style.left);
            var top = parseInt(style.top);
            var w = domElement.offsetWidth;
            style.left = (left - w / 2) + 'px';
            style.top = (top + 10) + 'px';
            style.display = '';
        }
    });

		return fd;
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

    private native void loadData(JavaScriptObject nativeGraph, JavaScriptObject data)/*-{
		nativeGraph.loadJSON(data);
		nativeGraph.compute('end');
        nativeGraph.animate({
            modes: ['linear'],
            transition: $wnd.$jit.Trans.Elastic.easeOut,
            duration: 3000
        });
	}-*/;
}
