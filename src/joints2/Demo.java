package joints2;

import java.awt.Color;
import java.awt.Shape;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import multij.swing.MouseHandler;
import multij.tools.IllegalInstantiationException;
import multij.tools.Pair;

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
					final List<Pair<Integer, Integer>> segments = new ArrayList<>();
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
								final Pair<Integer, Integer> segment = segments.get(i);
								final Path2D shape = new Path2D.Double();
								final Point3f p1 = scene.getTransformed(jointLocations.get(segment.getFirst() >> 1));
								final Point3f p2 = scene.getTransformed(jointLocations.get(segment.getSecond() >> 1));
								
								shape.moveTo(p1.x, p1.y);
								shape.lineTo(p2.x, p2.y);
								
								scene.draw(shape, id == highlighted[0] ? Color.YELLOW : selection.contains(id) ? Color.RED : Color.BLUE, id, g);
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
								
								scene.getUpdateNeeded().set(true);
							}
						}
						
						private static final long serialVersionUID = 8216721073603131316L;
						
					}.addTo(scene.getView());
					
					scene.getView().addKeyListener(new KeyAdapter() {
						
						@Override
						public final void keyPressed(final KeyEvent event) {
							if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
								// TODO
							} else if (event.getKeyCode() == KeyEvent.VK_ENTER) {
								final Integer[] selected = selection.toArray(new Integer[selection.size()]);
								
								if (selected.length == 2 && (selected[0] & 1) == 1 && (selected[1] & 1) == 1) {
									segments.add(new Pair<>(selected[0], selected[1]));
									scene.getUpdateNeeded().set(true);
								}
							}
						}
						
					});
					
					scene.getView().setFocusable(true);
				}
				
				{
					final List<Point3f> locations = scene.getLocations().computeIfAbsent("shape", k -> new ArrayList<>());
					
					locations.add(new Point3f(-0.5F, 0.5F, 0F));
					locations.add(new Point3f(-0.5F, -0.5F, 0F));
					locations.add(new Point3f(0.5F, -0.5F, 0F));
					locations.add(new Point3f(0.5F, 0.5F, 0F));
				}
				
				scene.getView().getRenderers().add(g -> {
					scene.draw(scene.polygon("shape", new Path2D.Float()), Color.RED, -1, g);
				});
				
				scene.getWindow(Demo.class.getName());
			}
			
		});
	}
	
}
