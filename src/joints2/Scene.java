package joints2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.io.Serializable;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.swing.Timer;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

/**
 * @author codistmonk (creation 2015-07-28)
 */
public final class Scene implements Serializable, Consumer<Graphics2D> {
	
	private final JLView view = new JLView();
	
	private final Camera camera = new Camera(this.view);
	
	private final Map<Object, List<Point3f>> locations = new HashMap<>();
	
	private final Map<Point3f, Point3f> transformedLocations = new IdentityHashMap<>();
	
	private final Matrix4f transform = new Matrix4f();
	
	private final AtomicBoolean updateNeeded = new AtomicBoolean();
	
	private final Timer timer = new Timer(40, e -> {
		if (this.getUpdateNeeded().getAndSet(false)) {
			this.getView().repaint();
		}
	});
	
	{
		this.getView().getRenderers().add(this);
		this.getTimer().setRepeats(true);
		this.getTimer().start();
	}
	
	public final JLView getView() {
		return this.view;
	}
	
	public final Camera getCamera() {
		return this.camera;
	}
	
	public final Map<Object, List<Point3f>> getLocations() {
		return this.locations;
	}
	
	public final AtomicBoolean getUpdateNeeded() {
		return this.updateNeeded;
	}
	
	public final Timer getTimer() {
		return this.timer;
	}
	
	@Override
	public final void accept(final Graphics2D g) {
		g.setColor(Color.BLACK);
		g.fill(new Rectangle(-1, -1, 2, 2));
		
		this.getCamera().getProjectionView(this.transform);
		this.getLocations().values().forEach(l -> l.forEach(v -> this.transform.transform(
				v, this.transformedLocations.computeIfAbsent(v, Point3f::new))));
	}
	
	public final Path2D polygon(final Object locationsKey, final Path2D result) {
		final List<Point3f> locations = this.getLocations().get(locationsKey);
		final int n = locations.size();
		Point3f p = this.transformedLocations.get(locations.get(0));
		
		result.moveTo(p.x, p.y);
		
		for (int i = 1; i < n; ++i) {
			p = this.transformedLocations.get(locations.get(i));
			result.lineTo(p.x, p.y);
		}
		
		result.closePath();
		
		return result;
	}
	
	private static final long serialVersionUID = -855341394725808027L;
	
}