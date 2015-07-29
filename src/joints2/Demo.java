package joints2;

import static multij.tools.Tools.*;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.vecmath.Point3f;

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
				final Scene scene = new Scene();
				
				{
					final List<Point3f> locationsIn = scene.getLocations().computeIfAbsent("shape", k -> new ArrayList<>());
					
					locationsIn.add(new Point3f(-0.5F, 0.5F, 0F));
					locationsIn.add(new Point3f(-0.5F, -0.5F, 0F));
					locationsIn.add(new Point3f(0.5F, -0.5F, 0F));
					locationsIn.add(new Point3f(0.5F, 0.5F, 0F));
				}
				
				scene.getView().getRenderers().add(g -> {
					g.setColor(Color.RED);
					g.draw(scene.polygon("shape", new Path2D.Float()));
				});
				
				new Orbiter(scene.getUpdateNeeded(), scene.getCamera()).addTo(scene.getView());
				
				SwingTools.show(scene.getView(), Demo.class.getName(), false).addWindowListener(new WindowAdapter() {
					
					@Override
					public final void windowClosing(final WindowEvent event) {
						scene.getTimer().stop();
					}
					
				});
			}
			
		});
	}
	
}
