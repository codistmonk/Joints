package joints2;

import static java.lang.Math.random;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Shape;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import multij.swing.MouseHandler;
import multij.swing.SwingTools;

/**
 * @author codistmonk (creation 2015-07-31)
 */
public final class JointsEditorPanel extends JPanel {
	
	private final Scene scene;
	
	private final List<Point3f> jointLocations;
	
	private final List<JointsEditorPanel.Segment> segments;
	
	private final int[] highlighted;
	
	private final Collection<Integer> selection;
	
	public JointsEditorPanel() {
		super(new BorderLayout());
		this.scene = new Scene().setClearColor(Color.WHITE);
		this.jointLocations = this.getScene().getLocations().computeIfAbsent("joints", k -> new ArrayList<>());
		this.segments = new ArrayList<>();
		this.highlighted = new int[1];
		this.selection = new HashSet<>();
		
		new Orbiter(this.getScene().getUpdateNeeded(), this.getScene().getCamera()).addTo(this.getScene().getView());
		
		this.addHierarchyListener(new HierarchyListener() {
			
			@Override
			public final void hierarchyChanged(final HierarchyEvent event) {
				getScene().setWindow(SwingUtilities.getWindowAncestor(JointsEditorPanel.this));
			}
			
		});
		
		getJointLocations().add(new Point3f());
		
		getScene().getView().getRenderers().add(g -> {
			{
				final int n = getJointLocations().size();
				
				for (int i = 0; i < n; ++i) {
					final int id = 2 * i + 1;
					final Point3f p = getScene().getTransformed(getJointLocations().get(i));
					final double r = 0.05;
					final Shape shape = new Ellipse2D.Double(p.x - r, p.y - r, 2.0 * r, 2.0 * r);
					
					getScene().fill(shape, id == getHighlighted()[0] ? Color.YELLOW : getSelection().contains(id) ? Color.RED : Color.BLUE, id, g);
				}
			}
			
			{
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
		});
		new MouseHandler() {
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				final int idUnderMouse = getScene().getIdUnderMouse();
				
				if (idUnderMouse != getHighlighted()[0]) {
					getHighlighted()[0] = idUnderMouse;
					getScene().getUpdateNeeded().set(true);
				}
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				if (event.isPopupTrigger()) {
					return;
				}
				
				final int id = getHighlighted()[0];
				
				if (event.getClickCount() == 1 && id != 0) {
					if (event.isShiftDown()) {
						if (!getSelection().add(id)) {
							getSelection().remove(id);
						}
					} else {
						getSelection().clear();
						getSelection().add(id);
					}
					
					getScene().getUpdateNeeded().set(true);
				} else if (event.getClickCount() == 2 && id == 0) {
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
					
					getScene().getUpdateNeeded().set(true);
				}
			}
			
			private static final long serialVersionUID = 8216721073603131316L;
			
		}.addTo(getScene().getView());
		
		getScene().getView().addKeyListener(new KeyAdapter() {
			
			@Override
			public final void keyPressed(final KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_D) {
					SwingTools.show(getScene().getIds().getImage(), "ids", false);
				} else if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
					// TODO delete selection
				} else if (event.getKeyCode() == KeyEvent.VK_ENTER) {
					final Integer[] selected = getSelection().toArray(new Integer[getSelection().size()]);
					
					if (selected.length == 2 && (selected[0] & 1) == 1 && (selected[1] & 1) == 1) {
						getSegments().add(new Segment(getJointLocations().get(selected[0] >> 1), getJointLocations().get(selected[1] >> 1)));
						getScene().getUpdateNeeded().set(true);
					} else if (0 < selected.length && Arrays.stream(selected).allMatch(id -> (id & 1) == 0)) {
						final double average = Arrays.stream(selected).map(id -> getSegments().get((id - 2) >> 1)).mapToDouble(Segment::getConstraint).average().getAsDouble();
						final String newConstraintAsString = JOptionPane.showInputDialog("constraint:", average);
						
						if (newConstraintAsString != null) {
							final double newConstraint = Double.parseDouble(newConstraintAsString);
							
							Arrays.stream(selected).forEach(id -> getSegments().get((id - 2) >> 1).setConstraint(newConstraint));
							
							getScene().getUpdateNeeded().set(true);
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
		result.set(middle(p1.x, p2.x), middle(p1.y, p2.y), middle(p1.y, p2.y));
		
		return result;
	}
	
	public static final float middle(final float a, final float b) {
		return (a + b) / 2F;
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
		
		private static final long serialVersionUID = 2645415714139697519L;
		
	}
	
}