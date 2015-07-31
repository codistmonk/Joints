package joints2;

import static java.lang.Math.random;
import static java.util.stream.Collectors.toList;
import static multij.xml.XMLTools.getNumber;
import static multij.xml.XMLTools.getString;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import multij.swing.MouseHandler;
import multij.swing.SwingTools;
import multij.xml.XMLTools;

/**
 * @author codistmonk (creation 2015-07-31)
 */
public final class JointsEditorPanel extends JPanel {
	
	private final Scene scene;
	
	private final List<Point3f> jointLocations;
	
	private final List<Segment> segments;
	
	private final int[] highlighted;
	
	private final Collection<Integer> selection;
	
	private final Orbiter orbiter;
	
	public JointsEditorPanel() {
		super(new BorderLayout());
		this.scene = new Scene().setClearColor(Color.WHITE);
		this.jointLocations = this.getScene().getLocations().computeIfAbsent("joints", k -> new ArrayList<>());
		this.segments = new ArrayList<>();
		this.highlighted = new int[1];
		this.selection = new HashSet<>();
		this.orbiter = new Orbiter(this.getScene().getUpdateNeeded(), this.getScene().getCamera()).addTo(this.getScene().getView());
		
		this.addHierarchyListener(new HierarchyListener() {
			
			@Override
			public final void hierarchyChanged(final HierarchyEvent event) {
				getScene().setWindow(SwingUtilities.getWindowAncestor(JointsEditorPanel.this));
			}
			
		});
		
		getJointLocations().add(new Point3f());
		
		getScene().getView().getRenderers().add(g -> {
			renderJoints(g);
			renderSegments(g);
		});
		
		new MouseHandler() {
			
			private final Point mouse = new Point();
			
			private boolean moving = false;
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				if (this.moving) {
					final List<Point3f> activePoints = collectPointsFromSelection();
					
					if (!activePoints.isEmpty()) {
						final AffineTransform graphicsTransform = getScene().getGraphicsTransform();
						final Point2D previousMouse = new Point2D.Double(this.mouse.x, this.mouse.y);
						final Point2D currentMouse = new Point2D.Double(event.getX(), event.getY());
						
						try {
							graphicsTransform.inverseTransform(previousMouse, previousMouse);
							graphicsTransform.inverseTransform(currentMouse, currentMouse);
							
							final Matrix4f m = getScene().getCamera().getProjectionView(new Matrix4f());
							
							m.invert();
							
							final Point3f previousLocation = point3f(previousMouse);
							final Point3f currentLocation = point3f(currentMouse);
							
							m.transform(previousLocation);
							m.transform(currentLocation);
							
							currentLocation.sub(previousLocation);
							
							activePoints.forEach(p -> p.add(currentLocation));
							
							scheduleUpdate();
						} catch (final NoninvertibleTransformException exception) {
							exception.printStackTrace();
						}
					}
				}
				
				this.mouse.setLocation(event.getX(), event.getY());
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				final int idUnderMouse = getScene().getIdUnderMouse();
				
				if (idUnderMouse != getHighlighted()[0]) {
					getHighlighted()[0] = idUnderMouse;
					scheduleUpdate();
				}
			}
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.mouse.setLocation(event.getX(), event.getY());
				
				if (event.isPopupTrigger()) {
					return;
				}
				
				final int id = getHighlighted()[0];
				
				if (id != 0) {
					if (event.isShiftDown()) {
						if (!getSelection().add(id)) {
							getSelection().remove(id);
						}
					} else {
						getSelection().clear();
						getSelection().add(id);
					}
					
					this.moving = true;
					getScene().getView().removeMouseMotionListener(getOrbiter());
					
					scheduleUpdate();
				} else if (!Arrays.asList(getScene().getView().getMouseMotionListeners()).contains(getOrbiter())) {
					this.moving = false;
					getScene().getView().addMouseMotionListener(getOrbiter());
				}
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				if (event.isPopupTrigger()) {
					return;
				}
				
				final int id = getHighlighted()[0];
				
				if (event.getClickCount() == 2 && id == 0) {
					final Matrix4f m = new Matrix4f();
					
					getScene().getCamera().getProjectionView(m);
					
					m.invert();
					
					final Point3f p = new Point3f(
							2F * event.getX() / getScene().getView().getWidth() - 1F,
							1F - 2F * event.getY() / getScene().getView().getHeight(),
							0F);
					
					m.transform(p);
					
					getJointLocations().add(p);
					getHighlighted()[0] = getJointLocations().size() * 2 - 1;
					
					scheduleUpdate();
				}
			}
			
			private static final long serialVersionUID = 8216721073603131316L;
			
		}.addTo(getScene().getView());
		
		getScene().getView().addKeyListener(new KeyAdapter() {
			
			@Override
			public final void keyPressed(final KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_SPACE) {
					final Point3f center = new Point3f();
					final List<Point3f> points = collectPointsFromSelection();
					
					if (!points.isEmpty()) {
						points.forEach(center::add);
						center.scale(1F / points.size());
						
						getOrbiter().getTarget().set(center);
						getOrbiter().updateCamera();
					}
				} if (event.getKeyCode() == KeyEvent.VK_O && event.isControlDown()) {
					open();
				} else if (event.getKeyCode() == KeyEvent.VK_S && event.isControlDown()) {
					save();
				} else if (event.getKeyCode() == KeyEvent.VK_D) {
					SwingTools.show(getScene().getIds().getImage(), "ids", false);
				} else if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
					deleteSelection();
				} else if (event.getKeyCode() == KeyEvent.VK_ENTER) {
					final Integer[] selected = getSelection().toArray(new Integer[getSelection().size()]);
					
					if (selected.length == 2 && (selected[0] & 1) == 1 && (selected[1] & 1) == 1) {
						getSegments().add(new Segment(point(selected[0]), point(selected[1])));
						getSelection().clear();
						getSelection().add(2 + 2 * (getSegments().size() - 1));
						scheduleUpdate();
					} else if (0 < selected.length && Arrays.stream(selected).allMatch(id -> (id & 1) == 0)) {
						final double average = Arrays.stream(selected).map(JointsEditorPanel.this::segment).mapToDouble(Segment::getConstraint).average().getAsDouble();
						final String newConstraintAsString = JOptionPane.showInputDialog("constraint:", average);
						
						if (newConstraintAsString != null) {
							final double newConstraint = Double.parseDouble(newConstraintAsString);
							
							Arrays.stream(selected).forEach(id -> segment(id).setConstraint(newConstraint));
							
							scheduleUpdate();
						}
					}
				}
			}
			
		});
		
		getScene().getView().setFocusable(true);
		
		getScene().getView().getRenderers().add(g -> {
			applyConstraints(getSegments(), getScene().getUpdateNeeded());
		});
		
		{
			final List<Point3f> locations = getScene().getLocations().computeIfAbsent("shape", k -> new ArrayList<>());
			
			locations.add(new Point3f(-1F, 0F, -1F));
			locations.add(new Point3f(-1F, 0F, 1F));
			locations.add(new Point3f(1F, 0F, 1F));
			locations.add(new Point3f(1F, 0F, -1F));
		}
		
		getScene().getView().getRenderers().add(g -> {
			getScene().draw(getScene().polygon("shape", new Path2D.Float()), Color.RED, -1, g);
		});
		
		this.add(this.getScene().getView(), BorderLayout.CENTER);
	}
	
	public final Scene getScene() {
		return this.scene;
	}
	
	public final List<Point3f> getJointLocations() {
		return this.jointLocations;
	}
	
	public final List<JointsEditorPanel.Segment> getSegments() {
		return this.segments;
	}
	
	public final int[] getHighlighted() {
		return this.highlighted;
	}
	
	public final Collection<Integer> getSelection() {
		return this.selection;
	}
	
	public final Orbiter getOrbiter() {
		return this.orbiter;
	}
	
	public final void open() {
		final JFileChooser fileChooser = new JFileChooser();
		
		if (fileChooser.showOpenDialog(JointsEditorPanel.this) == JFileChooser.APPROVE_OPTION) {
			this.open(fileChooser.getSelectedFile());
		}
		
	}
	
	public final JointsEditorPanel open(final File file) {
		try (final InputStream input = new FileInputStream(file)) {
			this.fromXML(XMLTools.parse(input));
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
		
		return this;
	}
	
	public final void save() {
		final JFileChooser fileChooser = new JFileChooser();
		
		if (fileChooser.showSaveDialog(JointsEditorPanel.this) == JFileChooser.APPROVE_OPTION) {
			this.save(fileChooser.getSelectedFile());
		}
	}
	
	public final JointsEditorPanel save(final File file) {
		XMLTools.write(this.toXML(), file, 0);
		
		return this;
	}
	
	public final void fromXML(final Document xml) {
		final List<Point3f> newJointLocations = XMLTools.getNodes(xml, "//joint").stream().map(n -> new Point3f(
				getFloat(n, "@x"), getFloat(n, "@y"), getFloat(n, "@z"))).collect(toList());
		final List<Segment> newSegments = XMLTools.getNodes(xml, "//segment").stream().map(n -> new Segment(
				getPoint1(n, newJointLocations), getPoint2(n, newJointLocations)).setConstraint(getConstraint(n))).collect(toList());
		
		getJointLocations().clear();
		getSegments().clear();
		
		getSelection().clear();
		getHighlighted()[0] = 0;
		getJointLocations().addAll(newJointLocations);
		getSegments().addAll(newSegments);
		
		scheduleUpdate();
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
			
			element.setAttribute("point1", Integer.toString(getJointLocations().indexOf(segment.getPoint1())));
			element.setAttribute("point2", Integer.toString(getJointLocations().indexOf(segment.getPoint2())));
			element.setAttribute("constraint", Double.toString(segment.getConstraint()));
		}
		
		return result;
	}
	
	public final void deleteSelection() {
		if (!getSelection().isEmpty()) {
			final Integer[] selected = getSelection().toArray(new Integer[getSelection().size()]);
			final List<Point3f> pointsToRemove = new ArrayList<>();
			final List<Segment> segmentsToRemove = new ArrayList<>();
			
			for (final int id : selected) {
				if (isSegment(id)) {
					segmentsToRemove.add(segment(id));
				} else {
					final Point3f point = point(id);
					
					pointsToRemove.add(point);
					segmentsToRemove.addAll(getSegments().stream().filter(s -> s.isEndPoint(point)).collect(toList()));
				}
			}
			
			getSelection().clear();
			getJointLocations().removeAll(pointsToRemove);
			getSegments().removeAll(segmentsToRemove);
			
			scheduleUpdate();
		}
	}
	
	final void renderSegments(final Graphics2D g) {
		final int n = getSegments().size();
		
		for (int i = 0; i < n; ++i) {
			final int id = 2 * i + 2;
			final JointsEditorPanel.Segment segment = getSegments().get(i);
			final Path2D shape = new Path2D.Double();
			final Point3f p1 = getScene().getTransformed(segment.getPoint1());
			final Point3f p2 = getScene().getTransformed(segment.getPoint2());
			
			shape.moveTo(p1.x, p1.y);
			shape.lineTo(p2.x, p2.y);
			
			getScene().draw(shape, id == getHighlighted()[0] ? Color.YELLOW : getSelection().contains(id) ? Color.RED : Color.BLUE, id, g);
			
			{
				final Point2D p2D = point2D(middle(p1, p2));
				final AffineTransform transform = getScene().getGraphicsTransform();
				
				transform.transform(p2D, p2D);
				getScene().setGraphicsTransform(new AffineTransform());
				
				final String string = String.format("%.3f", segment.getPoint1().distance(segment.getPoint2())) + "/" + String.format("%.3f", segment.getConstraint());
				final Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(string, g);
				
				final float left = (float) (p2D.getX() - stringBounds.getWidth() / 2.0);
				final float bottom = (float) (p2D.getY() + stringBounds.getHeight() / 2.0);
				final float top = (float) (p2D.getY() - stringBounds.getHeight() / 2.0);
				
				stringBounds.setRect(left, top, stringBounds.getWidth(), stringBounds.getHeight());
				
				g.drawString(string, left, bottom);
				
				getScene().fillId(stringBounds, id);
				getScene().setGraphicsTransform(transform);
			}
		}
	}
	
	final void renderJoints(final Graphics2D g) {
		final int n = getJointLocations().size();
		
		for (int i = 0; i < n; ++i) {
			final int id = 2 * i + 1;
			final Point3f p = getScene().getTransformed(getJointLocations().get(i));
			final double r = 0.05;
			final Shape shape = new Ellipse2D.Double(p.x - r, p.y - r, 2.0 * r, 2.0 * r);
			
			getScene().fill(shape, id == getHighlighted()[0] ? Color.YELLOW : getSelection().contains(id) ? Color.RED : Color.BLUE, id, g);
		}
	}
	
	final void scheduleUpdate() {
		getScene().getUpdateNeeded().set(true);
	}
	
	final Point3f point(final int id) {
		return this.getJointLocations().get(jointIndex(id));
	}
	
	final Segment segment(final int id) {
		return this.getSegments().get(segmentIndex(id));
	}
	
	final List<Point3f> collectPointsFromSelection() {
		final List<Point3f> result = new ArrayList<>();
		
		for (final int id : getSelection()) {
			if (isJoint(id)) {
				result.add(point(id));
			} else {
				final Segment segment = segment(id);
				
				result.add(segment.getPoint1());
				result.add(segment.getPoint2());
			}
		}
		
		return result;
	}

	private static final long serialVersionUID = 6374986295888991754L;
	
	public static final void applyConstraints(final List<JointsEditorPanel.Segment> segments, final AtomicBoolean updateNeeded) {
		for (final JointsEditorPanel.Segment segment : segments) {
			final Point3f point1 = segment.getPoint1();
			final Point3f point2 = segment.getPoint2();
			final double constraint = segment.getConstraint();
			double distance = point1.distance(point2);
			
			if (distance != constraint) {
				final Point3f middle = middle(point1, point2);
				
				if (distance == 0.0) {
					final Point3f delta = new Point3f((float) (random() - 0.5), (float) (random() - 0.5), (float) (random() - 0.5));
					point1.add(delta);
					point2.sub(delta);
					
					distance = point1.distance(point2);
				}
				
				if (distance != 0.0) {
					final float k = (float) ((constraint + distance) / 2.0 / distance);
					
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
	
	public static final Point3f point3f(final Point2D p) {
		return point3f(p, new Point3f());
	}
	
	public static final Point3f point3f(final Point2D p, final Point3f result) {
		result.x = (float) p.getX();
		result.y = (float) p.getY();
		result.z = 0F;
		
		return result;
	}
	
	public static final Point2D point2D(final Point3f p) {
		return point2D(p, new Point2D.Double());
	}
	
	public static final Point2D point2D(final Point3f p, final Point2D result) {
		result.setLocation(p.x, p.y);
		
		return result;
	}
	
	public static final Point3f middle(final Point3f p1, final Point3f p2) {
		return middle(p1, p2, new Point3f());
	}
	
	public static final Point3f middle(final Point3f p1, final Point3f p2, final Point3f result) {
		result.set(middle(p1.x, p2.x), middle(p1.y, p2.y), middle(p1.z, p2.z));
		
		return result;
	}
	
	public static final float middle(final float a, final float b) {
		return (a + b) / 2F;
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
	
	static final boolean isJoint(final int id) {
		return (id & 1) == 1;
	}
	
	static final boolean isSegment(final int id) {
		return (id & 1) == 0;
	}
	
	static final int jointIndex(final int id) {
		return id >> 1;
	}
	
	static final int segmentIndex(final int id) {
		return (id - 2) >> 1;
	}
	
	/**
	 * @author codistmonk (creation 2015-07-30)
	 */
	public static final class Segment implements Serializable {
		
		private final Point3f point1;
		
		private final Point3f point2;
		
		private double constraint;
		
		public Segment(final Point3f point1, final Point3f point2) {
			this.point1 = point1;
			this.point2 = point2;
			
			this.setConstraint(point1.distance(point2));
		}
		
		public final double getConstraint() {
			return this.constraint;
		}
		
		public final JointsEditorPanel.Segment setConstraint(final double constraint) {
			this.constraint = constraint;
			
			return this;
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
		
		private static final long serialVersionUID = 2645415714139697519L;
		
	}
	
}