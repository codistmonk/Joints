package joints2;

import static java.lang.Math.random;
import static java.util.stream.Collectors.toList;
import static multij.tools.Tools.*;
import static multij.xml.XMLTools.getNumber;
import static multij.xml.XMLTools.getString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.vecmath.Point3f;

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
	
	private final List<JointsModel.Segment> segments;
	
	public JointsModel(final Scene scene, final String name) {
		this.name = name;
		this.jointLocations = scene.getLocations().computeIfAbsent(name, k -> new ArrayList<>());
		this.previousJointLocations = new IdentityHashMap<>();
		this.segments = new ArrayList<>();
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
	
	public final JointsModel clear() {
		getJointLocations().clear();
		getSegments().clear();
		
		return this;
	}
	
	public final JointsModel addFromXML(final Document xml) {
		final List<Point3f> newJointLocations = XMLTools.getNodes(xml, "//joint").stream().map(n -> new Point3f(
				getFloat(n, "@x"), getFloat(n, "@y"), getFloat(n, "@z"))).collect(toList());
		final List<JointsModel.Segment> newSegments = XMLTools.getNodes(xml, "//segment").stream().map(n ->
				new Segment(getPoint1(n, newJointLocations), getPoint2(n, newJointLocations))
				.setConstraint(getConstraint(n)).setStyle(XMLTools.getString(n, "@style"))).collect(toList());
		
		getJointLocations().addAll(newJointLocations);
		getSegments().addAll(newSegments);
		
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
		
		for (final JointsModel.Segment segment : getSegments()) {
			final Element element = (Element) root.appendChild(result.createElement("segment"));
			
			element.setAttribute("point1", Integer.toString(indexOf(segment.getPoint1(), getJointLocations())));
			element.setAttribute("point2", Integer.toString(indexOf(segment.getPoint2(), getJointLocations())));
			element.setAttribute("constraint", Double.toString(segment.getConstraint()));
			element.setAttribute("style", segment.getStyleAsString());
		}
		
		return result;
	}
	
	public final void applyConstraints(final AtomicBoolean updateNeeded) {
		final float momentum = 0.4F;
		final float rigidity = 0.9F;
		
		for (final Point3f point : this.getJointLocations()) {
			final Point3f previous = this.previousJointLocations.computeIfAbsent(point, p -> new Point3f(p));
			
			point.x += (point.x - previous.x) * momentum;
			point.y += (point.y - previous.y) * momentum;
			point.z += (point.z - previous.z) * momentum;
			
			previous.set(point);
		}
		
		for (final JointsModel.Segment segment : this.getSegments()) {
			final Point3f point1 = segment.getPoint1();
			final Point3f point2 = segment.getPoint2();
			final double constraint = segment.getConstraint();
			double distance = point1.distance(point2);
			
			if (distance != constraint) {
				final Point3f middle = JointsEditorPanel.middle(point1, point2);
				
				if (distance == 0.0) {
					final Point3f delta = new Point3f((float) (random() - 0.5), (float) (random() - 0.5), (float) (random() - 0.5));
					point1.add(delta);
					point2.sub(delta);
					
					distance = point1.distance(point2);
				}
				
				if (distance != 0.0) {
					final float k = (float) (lerp(distance, rigidity, constraint) / distance);
					
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
		return getNumber(node, xPath).floatValue();
	}
	
	public static final Point3f getPoint1(final Node segmentNode, final List<Point3f> points) {
		return points.get(getInt(segmentNode, "@point1"));
	}
	
	public static final Point3f getPoint2(final Node segmentNode, final List<Point3f> points) {
		return points.get(getInt(segmentNode, "@point2"));
	}
	
	public static final double getConstraint(final Node segmentNode) {
		return parseDouble(getString(segmentNode, "@constraint"));
	}
	
	/**
	 * @author codistmonk (creation 2015-07-30)
	 */
	public static final class Segment implements Serializable {
		
		private final Point3f point1;
		
		private final Point3f point2;
		
		private double constraint;
		
		private final Map<Object, Object> style;
		
		public Segment(final Point3f point1, final Point3f point2) {
			this.point1 = point1;
			this.point2 = point2;
			this.style = new LinkedHashMap<>();
			
			this.setConstraint(point1.distance(point2));
			this.setStyle("visible", "true").setStyle("color", "#FF0000FF");
		}
		
		public final double getConstraint() {
			return this.constraint;
		}
		
		public final Segment setConstraint(final double constraint) {
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
				final String[] kv = keyValue.split(": ");
				
				if (kv.length == 2) {
					this.setStyle(kv[0], kv[1]);
				} else {
					debugError((Object[]) kv);
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
	
}