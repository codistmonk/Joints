package joints2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

import multij.swing.MouseHandler;
import multij.swing.SwingTools;
import multij.tools.Canvas;

/**
 * @author codistmonk (creation 2015-07-28)
 */
public final class Scene implements Serializable, Consumer<Graphics2D> {
	
	private final JLView view = new JLView();
	
	private final Canvas ids = new Canvas();
	
	private final Camera camera = new Camera(this.view);
	
	private Color clearColor = Color.BLACK;
	
	private final Map<Object, List<Point3f>> locations = new HashMap<>();
	
	private final Map<Point3f, Point3f> transformedLocations = new IdentityHashMap<>();
	
	private final Matrix4f transform = new Matrix4f();
	
	private final AtomicBoolean updateNeeded = new AtomicBoolean();
	
	private Window window;
	
	private final Timer timer = new Timer(40, e -> {
		if (this.getUpdateNeeded().getAndSet(false)) {
			this.getView().repaint();
		}
	});
	
	private int idUnderMouse;
	
	{
		this.getView().getRenderers().add(this);
		this.getTimer().setRepeats(true);
		this.getTimer().start();
		this.getView().addComponentListener(new ComponentAdapter() {
			
			@Override
			public final void componentResized(final ComponentEvent event) {
				getView().getCanvas().getGraphics().setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				
				getIds().setFormat(getView().getWidth(), getView().getHeight());
				getIds().getGraphics().setTransform(getView().getCanvas().getGraphics().getTransform());
				getIds().getGraphics().setStroke(getView().getCanvas().getGraphics().getStroke());
				getIds().getGraphics().setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			}
			
		});
		
		new MouseHandler() {
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				setIdUnderMouse(getIds().getImage().getRGB(event.getX(), event.getY()) & 0x00FFFFFF);
			}
			
			private static final long serialVersionUID = -3777081695610045505L;
			
		}.addTo(this.getView());
	}
	
	public final JLView getView() {
		return this.view;
	}
	
	public final Canvas getIds() {
		return this.ids;
	}
	
	public final Camera getCamera() {
		return this.camera;
	}
	
	public final Color getClearColor() {
		return this.clearColor;
	}
	
	public final Scene setClearColor(final Color clearColor) {
		this.clearColor = clearColor;
		
		return this;
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
	
	public final int getIdUnderMouse() {
		return this.idUnderMouse;
	}
	
	public final void setIdUnderMouse(final int idUnderMouse) {
		this.idUnderMouse = idUnderMouse;
	}
	
	@Override
	public final void accept(final Graphics2D g) {
		fill(g, this.getClearColor());
		fill(this.getIds().getGraphics(), Color.BLACK);
		
		this.getCamera().getProjectionView(this.transform);
		this.getLocations().values().forEach(l -> l.forEach(v -> this.transform.transform(
				v, this.transformedLocations.computeIfAbsent(v, Point3f::new))));
	}
	
	public final Path2D polygon(final Object locationsKey, final Path2D result) {
		final List<Point3f> locations = this.getLocations().get(locationsKey);
		final int n = locations.size();
		Point3f p = this.getTransformed(locations.get(0));
		
		result.moveTo(p.x, p.y);
		
		for (int i = 1; i < n; ++i) {
			p = this.getTransformed(locations.get(i));
			result.lineTo(p.x, p.y);
		}
		
		result.closePath();
		
		return result;
	}
	
	public final Point3f getTransformed(final Point3f location) {
		return this.transformedLocations.get(location);
	}
	
	public final void draw(final Shape shape, final Color color, final int id, final Graphics2D graphics) {
		graphics.setColor(color);
		graphics.draw(shape);
		
		if (0 <= id) {
			this.getIds().getGraphics().setColor(new Color(id));
			this.getIds().getGraphics().draw(shape);
		}
	}
	
	public final void fill(final Shape shape, final Color color, final int id, final Graphics2D graphics) {
		graphics.setColor(color);
		graphics.fill(shape);
		
		if (0 <= id) {
			this.getIds().getGraphics().setColor(new Color(id));
			this.getIds().getGraphics().fill(shape);
		}
	}
	
	public final Window getWindow(final String title) {
		if (this.window == null) {
			this.window = SwingTools.show(this.getView(), title, false);
			
			this.window.addWindowListener(new WindowAdapter() {
				
				@Override
				public final void windowClosing(final WindowEvent event) {
					getTimer().stop();
				}
				
			});
		}
		
		return this.window;
	}
	
	private static final long serialVersionUID = -855341394725808027L;
	
	private static final Rectangle VIEW = new Rectangle(-1, -1, 2, 2);
	
	private static final void fill(final Graphics2D graphics, final Color color) {
		graphics.setColor(color);
		graphics.fill(VIEW);
	}
	
}