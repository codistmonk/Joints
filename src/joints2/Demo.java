package joints2;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.vecmath.Point3f;

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
					final List<Point3f> locationsIn = scene.getLocations().computeIfAbsent("shape", k -> new ArrayList<>());
					
					locationsIn.add(new Point3f(-0.5F, 0.5F, 0F));
					locationsIn.add(new Point3f(-0.5F, -0.5F, 0F));
					locationsIn.add(new Point3f(0.5F, -0.5F, 0F));
					locationsIn.add(new Point3f(0.5F, 0.5F, 0F));
				}
				
				scene.getView().getRenderers().add(g -> {
					scene.draw(scene.polygon("shape", new Path2D.Float()), Color.RED, 1, g);
				});
				
				scene.getWindow(Demo.class.getName());
			}
			
		});
	}
	
}
