package joints2;

import static java.lang.Math.random;

import java.awt.Color;
import java.awt.Shape;
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
import javax.swing.SwingUtilities;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import multij.swing.MouseHandler;
import multij.swing.SwingTools;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-07-28)
 */
public final class Demo {
	
	private Demo() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				final Scene scene = new Scene().setClearColor(Color.WHITE);
				
				new Orbiter(scene.getUpdateNeeded(), scene.getCamera()).addTo(scene.getView());
				
				{
					final List<Point3f> jointLocations = scene.getLocations().computeIfAbsent("joints", k -> new ArrayList<>());
					final List<Segment> segments = new ArrayList<>();
					final int[] highlighted = { 0 };
					final Collection<Integer> selection = new HashSet<>();
					
					jointLocations.add(new Point3f());
					
					scene.getView().getRenderers().add(g -> {
						{
							final int n = jointLocations.size();
							
							for (int i = 0; i < n; ++i) {
								final int id = 2 * i + 1;
								final Point3f p = scene.getTransformed(jointLocations.get(i));
								final double r = 0.05;
								final Shape shape = new Ellipse2D.Double(p.x - r, p.y - r, 2.0 * r, 2.0 * r);
								
								scene.fill(shape, id == highlighted[0] ? Color.YELLOW : selection.contains(id) ? Color.RED : Color.BLUE, id, g);
							}
						}
						
						{
							final int n = segments.size();
							
							for (int i = 0; i < n; ++i) {
								final int id = 2 * i + 2;
								final Segment segment = segments.get(i);
								final Path2D shape = new Path2D.Double();
								final Point3f p1 = scene.getTransformed(segment.getPoint1());
								final Point3f p2 = scene.getTransformed(segment.getPoint2());
								
								shape.moveTo(p1.x, p1.y);
								shape.lineTo(p2.x, p2.y);
								
								scene.draw(shape, id == highlighted[0] ? Color.YELLOW : selection.contains(id) ? Color.RED : Color.BLUE, id, g);
								
								{
									final Point2D p2D = point2D(middle(p1, p2));
									final AffineTransform transform = scene.getGraphicsTransform();
									
									transform.transform(p2D, p2D);
									scene.setGraphicsTransform(new AffineTransform());
									
									final String string = String.format("%.3f", segment.getPoint1().distance(segment.getPoint2())) + "/" + String.format("%.3f", segment.getConstraint());
									final Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(string, g);
									
									final float left = (float) (p2D.getX() - stringBounds.getWidth() / 2.0);
									final float bottom = (float) (p2D.getY() + stringBounds.getHeight() / 2.0);
									final float top = (float) (p2D.getY() - stringBounds.getHeight() / 2.0);
									
									stringBounds.setRect(left, top, stringBounds.getWidth(), stringBounds.getHeight());
									
									g.drawString(string, left, bottom);
									
									scene.fillId(stringBounds, id);
									scene.setGraphicsTransform(transform);
								}
							}
						}
					});
					
					new MouseHandler() {
						
						@Override
						public final void mouseMoved(final MouseEvent event) {
							final int idUnderMouse = scene.getIdUnderMouse();
							
							if (idUnderMouse != highlighted[0]) {
								highlighted[0] = idUnderMouse;
								scene.getUpdateNeeded().set(true);
							}
						}
						
						@Override
						public final void mouseClicked(final MouseEvent event) {
							if (event.isPopupTrigger()) {
								return;
							}
							
							final int id = highlighted[0];
							
							if (event.getClickCount() == 1 && id != 0) {
								if (event.isShiftDown()) {
									if (!selection.add(id)) {
										selection.remove(id);
									}
								} else {
									selection.clear();
									selection.add(id);
								}
								
								scene.getUpdateNeeded().set(true);
							} else if (event.getClickCount() == 2 && id == 0) {
								final Matrix4f m = new Matrix4f();
								
								scene.getCamera().getProjectionView(m);
								
								m.invert();
								
								final Point3f p = new Point3f(
										2F * event.getX() / scene.getView().getWidth() - 1F,
										1F - 2F * event.getY() / scene.getView().getHeight(),
										0F);
								
								m.transform(p);
								
								jointLocations.add(p);
								highlighted[0] = jointLocations.size() * 2 - 1;
								
								scene.getUpdateNeeded().set(true);
							}
						}
						
						private static final long serialVersionUID = 8216721073603131316L;
						
					}.addTo(scene.getView());
					
					scene.getView().addKeyListener(new KeyAdapter() {
						
						@Override
						public final void keyPressed(final KeyEvent event) {
							if (event.getKeyCode() == KeyEvent.VK_D) {
								SwingTools.show(scene.getIds().getImage(), "ids", false);
							} else if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
								// TODO
							} else if (event.getKeyCode() == KeyEvent.VK_ENTER) {
								final Integer[] selected = selection.toArray(new Integer[selection.size()]);
								
								if (selected.length == 2 && (selected[0] & 1) == 1 && (selected[1] & 1) == 1) {
									segments.add(new Segment(jointLocations.get(selected[0] >> 1), jointLocations.get(selected[1] >> 1)));
									scene.getUpdateNeeded().set(true);
								} else if (0 < selected.length && Arrays.stream(selected).allMatch(id -> (id & 1) == 0)) {
									final double average = Arrays.stream(selected).map(id -> segments.get((id - 2) >> 1)).mapToDouble(Segment::getConstraint).average().getAsDouble();
									final String newConstraintAsString = JOptionPane.showInputDialog("constraint:", average);
									
									if (newConstraintAsString != null) {
										final double newConstraint = Double.parseDouble(newConstraintAsString);
										
										Arrays.stream(selected).forEach(id -> segments.get((id - 2) >> 1).setConstraint(newConstraint));
										
										scene.getUpdateNeeded().set(true);
									}
								}
							}
						}
						
					});
					
					scene.getView().setFocusable(true);
					
					scene.getView().getRenderers().add(g -> {
						applyConstraints(segments, scene.getUpdateNeeded());
					});
				}
				
				{
					final List<Point3f> locations = scene.getLocations().computeIfAbsent("shape", k -> new ArrayList<>());
					
					locations.add(new Point3f(-1F, 0F, -1F));
					locations.add(new Point3f(-1F, 0F, 1F));
					locations.add(new Point3f(1F, 0F, 1F));
					locations.add(new Point3f(1F, 0F, -1F));
				}
				
				scene.getView().getRenderers().add(g -> {
					scene.draw(scene.polygon("shape", new Path2D.Float()), Color.RED, -1, g);
				});
				
				scene.getWindow(Demo.class.getName());
			}
			
		});
	}
	
	public static final void applyConstraints(final List<Segment> segments, final AtomicBoolean updateNeeded) {
		for (final Segment segment : segments) {
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
		
		public final Segment setConstraint(final double constraint) {
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
