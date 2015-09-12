package joints2;

import static java.lang.Math.abs;
import static java.lang.Math.random;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static multij.tools.Tools.*;
import static multij.xml.XMLTools.getBoolean;
import static multij.xml.XMLTools.getNodes;
import static multij.xml.XMLTools.getNumber;
import static multij.xml.XMLTools.getString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.vecmath.Point3f;

import multij.tools.Pair;
import multij.tools.Scripting;
import multij.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author codistmonk (creation 2015-07-31)
 */
public final class JointsModel implements Serializable {
	
	private final String name;
	
	private final List<Point3f> jointLocations;
	
	private final Map<Point3f, Point3f> previousJointLocations;
	
	private final List<Segment> segments;
	
	private final Map<String, Group> groups;
	
	private final ScriptEngine scriptEngine;
	
	public JointsModel(final Scene scene, final String name) {
		this.name = name;
		this.jointLocations = scene.getLocations().computeIfAbsent(name, k -> new ArrayList<>());
		this.previousJointLocations = new IdentityHashMap<>();
		this.segments = new ArrayList<>();
		this.groups = new LinkedHashMap<>();
		this.scriptEngine = Scripting.getEngine("");
	}
	
	public final String getName() {
		return this.name;
	}
	
	public final List<Point3f> getJointLocations() {
		return this.jointLocations;
	}
	
	public final List<JointsModel.Segment> getSegments() {
		return this.segments;
	}
	
	public final Map<String, Group> getGroups() {
		return this.groups;
	}
	
	public final ScriptEngine getScriptEngine() {
		return this.scriptEngine;
	}
	
	public final Object evaluate(final String script) {
		try {
			return this.getScriptEngine().eval(script);
		} catch (final ScriptException exception) {
			throw unchecked(exception);
		}
	}
	
	public final JointsModel clear() {
		getJointLocations().clear();
		getSegments().clear();
		
		return this;
	}
	
	public final JointsModel addFromXML(final Document xml) {
		final List<Point3f> newJointLocations = getNodes(xml, "/model/joint").stream().map(n ->
				new Point3f(getFloat(n, "@x"), getFloat(n, "@y"), getFloat(n, "@z")))
				.collect(toList());
		final List<Segment> newSegments = getNodes(xml, "/model/segment").stream().map(n ->
				new Segment(getPoint1(n, newJointLocations), getPoint2(n, newJointLocations))
				.setConstraint(getConstraint(n)).setStyle(XMLTools.getString(n, "@style")))
				.collect(toList());
		final Map<String, Group> newGroups = getNodes(xml, "/model/group").stream().map(n ->
				new Pair<>(getString(n, "@name"), new Group().setSegmentSynchronizer(getBoolean(n, "@segmentSynchronizer")).addAll(getNodes(n, "joint|segment").stream().map(
						c -> "joint".equals(c.getNodeName()) ? newJointLocations.get(getInt(c, "@index")) : newSegments.get(getInt(c, "@index"))).collect(toList()))))
						.collect(toMap(Pair::getFirst, Pair::getSecond));
		
		this.getJointLocations().addAll(newJointLocations);
		this.getSegments().addAll(newSegments);
		this.getGroups().putAll(newGroups);
		
		return this;
	}
	
	public final Document toXML() {
		final Document result = XMLTools.parse("<model/>");
		final Element root = result.getDocumentElement();
		
		for (final Point3f p : getJointLocations()) {
			final Element element = (Element) root.appendChild(result.createElement("joint"));
			
			element.setAttribute("x", Float.toString(p.x));
			element.setAttribute("y", Float.toString(p.y));
			element.setAttribute("z", Float.toString(p.z));
		}
		
		for (final Segment segment : getSegments()) {
			final Element element = (Element) root.appendChild(result.createElement("segment"));
			
			element.setAttribute("point1", Integer.toString(indexOf(segment.getPoint1(), getJointLocations())));
			element.setAttribute("point2", Integer.toString(indexOf(segment.getPoint2(), getJointLocations())));
			element.setAttribute("constraint", segment.getConstraint());
			element.setAttribute("style", segment.getStyleAsString());
		}
		
		for (final Map.Entry<String, Group> entry: this.getGroups().entrySet()) {
			final Element element = (Element) root.appendChild(result.createElement("group"));
			
			element.setAttribute("name", entry.getKey());
			element.setAttribute("segmentSynchronizer", Boolean.toString(entry.getValue().isSegmentSynchronizer()));
			
			for (final Object object : entry.getValue().getObjects()) {
				if (object instanceof Segment) {
					final Element child = (Element) element.appendChild(result.createElement("segment"));
					
					child.setAttribute("index", Integer.toString(this.getSegments().indexOf(object)));
				} else {
					final Element child = (Element) element.appendChild(result.createElement("joint"));
					
					child.setAttribute("index", Integer.toString(this.getJointLocations().indexOf(object)));
				}
			}
		}
		
		return result;
	}
	
	public final void applyConstraints(final AtomicBoolean updateNeeded) {
		final float momentum = 0.4F;
		final float springiness = 0.9F;
		final int rigidity = 32;
		
		for (int i = 0; i < rigidity; ++i) {
			for (final Point3f point : this.getJointLocations()) {
				final Point3f previous = this.previousJointLocations.computeIfAbsent(point, p -> new Point3f(p));
				
				point.x += (point.x - previous.x) * momentum;
				point.y += (point.y - previous.y) * momentum;
				point.z += (point.z - previous.z) * momentum;
				
				previous.set(point);
			}
			
			for (final Segment segment : this.getSegments()) {
				final Point3f point1 = segment.getPoint1();
				final Point3f point2 = segment.getPoint2();
				final double constraint = this.evaluateConstraint(segment);
				double distance = point1.distance(point2);
				
				if (constraint * 0.1 < abs(distance - constraint)) {
					final Point3f middle = JointsEditorPanel.middle(point1, point2);
					
					if (distance == 0.0) {
						final Point3f delta = new Point3f((float) (random() - 0.5), (float) (random() - 0.5), (float) (random() - 0.5));
						point1.add(delta);
						point2.sub(delta);
						
						distance = point1.distance(point2);
					}
					
					if (distance != 0.0) {
						final float k = (float) (lerp(distance, springiness, constraint) / distance);
						
						middle.scale(1F - k);
						point1.scale(k);
						point1.add(middle);
						point2.scale(k);
						point2.add(middle);
					}
					
					updateNeeded.set(true);
				}
			}
		}
	}
	
	public final double evaluateConstraint(final Segment segment) {
		try {
			return segment.evaluateConstraint(this.getScriptEngine());
		} catch (final Exception exception) {
			debugError(exception);
			
			return 0.0;
		}
	}
	
	private static final long serialVersionUID = -7402680801009890782L;
	
	public static final int indexOf(final Object needle, final List<? extends Object> haystack) {
		int i = 0;
		
		for (final Object object : haystack) {
			if (needle == object) {
				return i;
			}
			
			++i;
		}
		
		return -1;
	}
	
	public static final double lerp(final double a, final double t, final double b) {
		return a * (1.0 - t) + b * t;
	}
	
	public static final double parseDouble(final String s) {
		return s.isEmpty() ? 0.0 : Double.parseDouble(s);
	}
	
	public static final int getInt(final Node node, final String xPath) {
		return getNumber(node, xPath).intValue();
	}
	
	public static final float getFloat(final Node node, final String xPath) {
		return Float.parseFloat(getString(node, xPath));
	}
	
	public static final Point3f getPoint1(final Node segmentNode, final List<Point3f> points) {
		return points.get(getInt(segmentNode, "@point1"));
	}
	
	public static final Point3f getPoint2(final Node segmentNode, final List<Point3f> points) {
		return points.get(getInt(segmentNode, "@point2"));
	}
	
	public static final String getConstraint(final Node segmentNode) {
		return getString(segmentNode, "@constraint");
	}
	
	/**
	 * @author codistmonk (creation 2015-07-30)
	 */
	public static final class Segment implements Serializable {
		
		private final Point3f point1;
		
		private final Point3f point2;
		
		private String constraint;
		
		private final Map<Object, Object> style;
		
		public Segment(final Point3f point1, final Point3f point2) {
			this.point1 = point1;
			this.point2 = point2;
			this.style = new LinkedHashMap<>();
			
			this.setConstraint("" + point1.distance(point2));
			this.setStyle("visible", "true").setStyle("color", "#FF0000FF");
		}
		
		public final double evaluateConstraint(final ScriptEngine scriptEngine) {
			try {
				return ((Number) scriptEngine.eval(this.getConstraint())).doubleValue();
			} catch (final ScriptException exception) {
				throw unchecked(exception);
			}
		}
		
		public final String getConstraint() {
			return this.constraint;
		}
		
		public final Segment updateConstraint(final String constraint) {
			return "NaN".equals(constraint) ? this : this.setConstraint(constraint);
		}
		
		public final Segment setConstraint(final String constraint) {
			this.constraint = constraint;
			
			return this;
		}
		
		public final Map<Object, Object> getStyle() {
			return this.style;
		}
		
		public final String getStyleAsString() {
			return this.getStyle().entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).reduce("", (s1, s2) -> s1 + "; " + s2);
		}
		
		@SuppressWarnings("unchecked")
		public final <V> V getStyle(final Object key) {
			return (V) this.getStyle().get(key);
		}
		
		public final Segment setStyle(final String style) {
			for (final String keyValue : style.split("; ")) {
				if (!keyValue.isEmpty()) {
					final String[] kv = keyValue.split(": ");
					
					if (kv.length == 2) {
						this.setStyle(kv[0], kv[1]);
					} else {
						debugError((Object[]) kv);
					}
				}
			}
			
			return this;
		}
		
		public final Segment setStyle(final Object key, final Object value) {
			this.getStyle().put(key, value);
			
			return this;
		}
		
		public final Segment updateStyle(final Object key, final Object value) {
			return value != null && !value.toString().isEmpty() ? this.setStyle(key, value) : this;
		}
		
		public final Point3f getPoint1() {
			return this.point1;
		}
		
		public final Point3f getPoint2() {
			return this.point2;
		}
		
		public final boolean isEndPoint(final Point3f point) {
			return point == this.getPoint1() || point == this.getPoint2();
		}
		
		@Override
		public final int hashCode() {
			return this.getPoint1().hashCode() + this.getPoint2().hashCode();
		}
		
		@Override
		public final boolean equals(final Object object) {
			final Segment that = cast(this.getClass(), object);
			
			return that != null && this.getPoint1() == that.getPoint1() && this.getPoint2() == that.getPoint2();
		}
		
		@Override
		public final String toString() {
			return "constraint=" + this.getConstraint() + " style=" + this.getStyle();
		}

		private static final long serialVersionUID = 2645415714139697519L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-08-01)
	 */
	public static final class Group implements Serializable {
		
		private final List<Object> objects = new ArrayList<>();
		
		private boolean segmentSynchronizer;
		
		public final boolean isSegmentSynchronizer() {
			return this.segmentSynchronizer;
		}
		
		public final Group setSegmentSynchronizer(final boolean segmentSynchronizer) {
			this.segmentSynchronizer = segmentSynchronizer;
			
			return this;
		}
		
		public final List<Object> getObjects() {
			return this.objects;
		}
		
		public final Group addAll(final Collection<Object> objects) {
			this.getObjects().addAll(objects);
			
			return this;
		}
		
		private static final long serialVersionUID = -4109708550865552657L;
		
	}
	
}