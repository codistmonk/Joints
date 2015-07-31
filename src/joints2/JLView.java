package joints2;

import static java.lang.Math.max;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JComponent;

import multij.tools.Canvas;

/**
 * @author codistmonk (creation 2015-07-28)
 */
public final class JLView extends JComponent implements JL {
	
	private final Canvas canvas = new Canvas();
	
	private final Rectangle viewport = new Rectangle();
	
	private final List<Consumer<Graphics2D>> renderers = new ArrayList<>();
	
	private Rectangle scissor;
	
	{
		this.addComponentListener(new ComponentAdapter() {
			
			@Override
			public final void componentResized(final ComponentEvent event) {
				final int w = getWidth();
				final int h = getHeight();
				
				if (0 < w && 0 < h) {
					final Graphics2D g = getCanvas().setFormat(w, h).getGraphics();
					final int m = max(w, h);
					
					g.setTransform(new AffineTransform());
					g.translate(w / 2.0, h / 2.0);
					g.scale(m / 2.0, -m / 2.0);
					g.setStroke(new BasicStroke(2F / m));
				}
			}
			
		});
		
		this.setPreferredSize(new Dimension(512, 512));
	}
	
	public final Canvas getCanvas() {
		return this.canvas;
	}
	
	public final List<Consumer<Graphics2D>> getRenderers() {
		return this.renderers;
	}
	
	@Override
	public final void enable(final Constant flag) {
		if (Constant.SCISSOR_TEST.equals(flag)) {
			this.scissor = new Rectangle();
		}
	}
	
	@Override
	public final void disable(final Constant flag) {
		if (Constant.SCISSOR_TEST.equals(flag)) {
			this.scissor = null;
		}
	}
	
	@Override
	public final void viewport(final int x, final int y, final int w, final int h) {
		this.viewport.setBounds(x, y, w, h);
	}
	
	@Override
	public final void scissor(final int x, final int y, final int w, final int h) {
		this.scissor.setBounds(x, y, w, h);
	}
	
	@Override
	protected final void paintComponent(final Graphics g) {
		super.paintComponent(g);
		
		final Graphics2D g2d = this.getCanvas().getGraphics();
		
		if (g2d != null) {
			this.getRenderers().forEach(r -> r.accept(g2d));
			
			g.drawImage(this.getCanvas().getImage(), 0, 0, null);
		}
	}
	
	private static final long serialVersionUID = -6662651660924313728L;
	
}