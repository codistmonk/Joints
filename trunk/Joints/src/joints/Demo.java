package joints;

import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.awt.Color.YELLOW;
import static java.lang.Math.max;
import static java.lang.Math.random;
import static java.lang.Math.round;
import static java.util.Arrays.fill;
import static joints.Constraint.distance;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-03-19)
 */
public final class Demo {
	
	private Demo() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * {@value}.
	 */
	public static final int X = 0;
	
	/**
	 * {@value}.
	 */
	public static final int Y = 1;
	
	/**
	 * {@value}.
	 */
	public static final int Z = 2;
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final DoubleList vertices = new DoubleList();
		final DoubleList masses = new DoubleList();
		
		vertices.addAll(0.0, 0.0, -640000000.0);
		masses.addAll(Double.POSITIVE_INFINITY);
		
		final DoubleList transformedVertices = new DoubleList();
		final Canvas canvas = new Canvas().setFormat(600, 600, BufferedImage.TYPE_3BYTE_BGR);
		final Canvas ids = new Canvas().setFormat(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		final JLabel view = new JLabel(new ImageIcon(canvas.getImage()));
		final AtomicInteger idUnderMouse = new AtomicInteger(-1);
		final OrbiterMouseHandler orbiter = new OrbiterMouseHandler(null)
			.setCenterX(canvas.getWidth() / 2.0)
			.setCenterY(canvas.getHeight() / 2.0);
		new MouseHandler(orbiter.getUpdateNeeded()) {
			
			private double massUnderMouse;
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				if (event.isConsumed()) {
					return;
				}
				
				final int newIdUnderMouse = (ids.getImage().getRGB(event.getX(), event.getY()) & 0x00FFFFFF) - 1;
				final int oldIdUnderMouse = idUnderMouse.getAndSet(newIdUnderMouse);
				
				if (newIdUnderMouse != oldIdUnderMouse) {
					if (0 <= oldIdUnderMouse) {
						masses.set(oldIdUnderMouse, this.massUnderMouse);
					}
					
					if (0 <= newIdUnderMouse) {
						this.massUnderMouse = masses.get(newIdUnderMouse);
						masses.set(newIdUnderMouse, Double.POSITIVE_INFINITY);
					}
					
					this.getUpdateNeeded().set(true);
				}
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				if (event.isConsumed()) {
					return;
				}
				
				final int offsetUnderMouse = idUnderMouse.get() * 3;
				final double[] locations = transformedVertices.toArray();
				
				if (0 <= offsetUnderMouse) {
					final double[] location = { locations[offsetUnderMouse + X], locations[offsetUnderMouse + Y], locations[offsetUnderMouse + Z] };
					location[X] = event.getX();
					location[Y] = canvas.getHeight() - 1 - event.getY();
					orbiter.inverseTransform(location);
					vertices.set(offsetUnderMouse + X, location[X]);
					vertices.set(offsetUnderMouse + Y, location[Y]);
					vertices.set(offsetUnderMouse + Z, location[Z]);
					event.consume();
				}
				
				this.getUpdateNeeded().set(true);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 1415975957612152368L;
			
		}.addTo(view);
		final StickMan stickMan = new StickMan(vertices, masses).translate(orbiter.getCenterX(), orbiter.getCenterY(), orbiter.getCenterZ());
		
		orbiter.addTo(view);
		
		show(view, Demo.class.getName(), false);
		
		new Animator(view) {
			
			@Override
			protected final boolean animateFrame() {
				if (orbiter.getUpdateNeeded().getAndSet(false)) {
					transformedVertices.clear().addAll(vertices.toArray());
					orbiter.transform(transformedVertices.toArray());
					
					canvas.clear(Color.BLACK);
					ids.clear(Color.BLACK);
					
					drawGrid(canvas, orbiter);
					stickMan.draw(transformedVertices.toArray(), canvas, ids, idUnderMouse, orbiter);
					drawAxes(canvas, orbiter);
				}
				
				{
					final int n = vertices.size();
					
					for (int i = 3; i < n; i += 3) {
						final double m = masses.get(i / 3);
						
						if (!Double.isInfinite(m)) {
							final double oldZ = vertices.get(i + Z);
							final double newZ = max(0.0, oldZ - 10.0);
							
							if (oldZ != newZ) {
								orbiter.getUpdateNeeded().set(true);
								vertices.set(i + Z, newZ);
							}
						}
					}
				}
				
				while (10.0 < stickMan.update()) {
					orbiter.getUpdateNeeded().set(true);
				}
				
				return view.isShowing();
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -809734448123973442L;
			
		}.setFrameRate(10.0).animate();
	}
	
	public static final void drawGrid(final Canvas canvas, final OrbiterMouseHandler orbiter) {
		final double s = 500.0;
		final double minimumX = orbiter.getCenterX() - s;
		final double maximumX = orbiter.getCenterX() + s;
		final double minimumY = orbiter.getCenterY() - s;
		final double maximumY = orbiter.getCenterY() + s;
		final double z = 0.0;
		final int stripes = 10;
		final double amplitudeX = maximumX - minimumX;
		final double amplitudeY = maximumY - minimumY;
		final int n = 3 * 2 * (stripes + 1) * 2;
		final double[] segments = new double[n];
		
		for (int i = 0, j = 0; i <= stripes; ++i) {
			segments[j++] = minimumX;
			segments[j++] = minimumY + i * amplitudeY / stripes;
			segments[j++] = z;
			segments[j++] = maximumX;
			segments[j++] = minimumY + i * amplitudeY / stripes;
			segments[j++] = z;
			
			segments[j++] = minimumX + i * amplitudeX / stripes;
			segments[j++] = minimumY;
			segments[j++] = z;
			segments[j++] = minimumX + i * amplitudeY / stripes;
			segments[j++] = maximumY;
			segments[j++] = z;
		}
		
		orbiter.transform(segments);
		
		final Graphics2D g = canvas.getGraphics();
		final int bottom = canvas.getHeight() - 1;
		
		g.setColor(Color.GRAY);
		
		for (int i = 0; i < n; i += 6) {
			g.drawLine(iround(segments[i + 0]), iround(bottom - segments[i + 1]),
					iround(segments[i + 3]), iround(bottom - segments[i + 4]));
		}
	}
	
	public static final int iround(final double value) {
		return (int) round(value);
	}
	
	public static final void drawAxes(final Canvas canvas, final OrbiterMouseHandler orbiter) {
		final int bottom = canvas.getHeight() - 1;
		final double s = 10.0;
		final double[] axes = {
				0.0, 0.0, 0.0,
				s, 0.0, 0.0,
				0.0, s, 0.0,
				0.0, 0.0, s
		};
		
		orbiter.transform(axes, 0.0, 0.0, 0.0);
		
		final Graphics2D g = canvas.getGraphics();
		
		g.setColor(RED);
		g.drawLine((int) round(s + axes[0 + 0]), bottom - (int) round(s + axes[0 + 1]),
				(int) round(s + axes[3 + 0]), bottom - (int) round(s + axes[3 + 1]));
		g.setColor(GREEN);
		g.drawLine((int) round(s + axes[0 + 0]), bottom - (int) round(s + axes[0 + 1]),
				(int) round(s + axes[6 + 0]), bottom - (int) round(s + axes[6 + 1]));
		g.setColor(BLUE);
		g.drawLine((int) round(s + axes[0 + 0]), bottom - (int) round(s + axes[0 + 1]),
				(int) round(s + axes[9 + 0]), bottom - (int) round(s + axes[9 + 1]));
	}
	
	/**
	 * @author codistmonk (creation 2014-03-19)
	 */
	public static final class StickMan implements Serializable {
		
		private final Collection<Constraint> constraints;
		
		private final List<Constraint> muscleConstraints;
		
		private int activeMuscleIndex;
		
		private double activeMuscleDelta;
		
		private double objective;
		
		private final int offset;
		
		private final int nextOffset;
		
		private final DoubleList vertices;
		
		private final DoubleList masses;
		
		private final int[] segments;
		
		private final int[] muscles;
		
		public StickMan(final DoubleList vertices, final DoubleList masses) {
			this.constraints = new ArrayList<Constraint>();
			this.muscleConstraints = new ArrayList<Constraint>();
			this.offset = vertices.size();
			/*
			 *     |
			 *     +
			 *    / \
			 *  +-----+
			 *  |\   /|
			 *  + \ / +
			 *  |  +  |
			 *    / \
			 *   +---+
			 *   |   |
			 *   +   +
			 *   |   |
			 */
			this.vertices = vertices;
			this.vertices.addAll(
					// right leg
					-20.0, 0.0, 0.0,
					-20.0, 0.0, 40.0,
					-20.0, 0.0, 80.0,
					// left leg
					20.0, 0.0, 0.0,
					20.0, 0.0, 40.0,
					20.0, 0.0, 80.0,
					// bust
					0.0, 0.0, 110.0,
					0.0, 0.0, 160.0,
					// head
					0.0, 0.0, 180.0,
					// right arm
					-40.0, 0.0, 70.0,
					-40.0, 0.0, 110.0,
					-40.0, 0.0, 150.0,
					// left arm
					40.0, 0.0, 70.0,
					40.0, 0.0, 110.0,
					40.0, 0.0, 150.0
			);
			this.masses = masses;
			this.nextOffset = vertices.size();
			this.masses.addAll(repeat((this.nextOffset - this.offset) / 3, 1.0));
			this.segments = new int[] {
					// right leg
					0, 1,
					1, 2,
					// left leg
					3, 4,
					4, 5,
					// torso
					2, 6,
					6, 5,
					5, 2,
					6, 11,
					11, 14,
					14, 6,
					11, 7,
					7, 14,
					// head
					7, 8,
					// right arm
					9, 10,
					10, 11,
					// left arm
					12, 13,
					13, 14,
			};
			this.muscles = new int[] {
					// right leg
					0, 2,
					1, 5,
					// left leg
					3, 5,
					4, 2,
					// torso
					2, 11,
					5, 14,
					// head
					6, 8,
					// right arm
					9, 11,
					10, 7,
					// left arm
					12, 14,
					13, 7,
			};
			
			{
				final int n = this.segments.length;
				final int i0 = this.offset / 3;
				
				for (int i = 0; i < n; i += 2) {
					final int i1 = i0 + this.segments[i + 0];
					final int i2 = i0 + this.segments[i + 1];
					
					this.constraints.add(new Constraint(i1, i2).setPreferredDistance(distance(this.vertices.toArray(), i1, i2)));
				}
			}
			
			{
				// right leg
				this.addMuscleConstraint(0, 1, 2);
				this.addMuscleConstraint(1, 2, 5);
				// left leg
				this.addMuscleConstraint(3, 4, 5);
				this.addMuscleConstraint(4, 5, 2);
				// torso
				this.addMuscleConstraint(2, 6, 11);
				this.addMuscleConstraint(5, 6, 14);
				// head
				this.addMuscleConstraint(6, 7, 8);
				// right arm
				this.addMuscleConstraint(9, 10, 11);
				this.addMuscleConstraint(10, 11, 7);
				// left arm
				this.addMuscleConstraint(12, 13, 14);
				this.addMuscleConstraint(13, 14, 7);
			}
		}
		
		private final Constraint addMuscleConstraint(final int... path) {
			final int i0 = this.offset / 3;
			final double[] vertices = this.vertices.toArray();
			final int n = path.length;
			double maximumDistance = 0.0;
			
			for (int i = 0; i < n - 1; ++i) {
				maximumDistance += distance(vertices, i0 + path[i], i0 + path[i + 1]);
			}
			
			final Constraint result = new Constraint(i0 + path[0], i0 + path[n - 1])
				.setMinimumDistance(maximumDistance / 2.0).setMaximumDistance(maximumDistance)
				.setClampedPreferredDistance(15.0 * maximumDistance / 16.0);
			
			this.constraints.add(result);
			this.muscleConstraints.add(result);
			
			return result;
		}
		
		public final StickMan translate(final double tx, final double ty, final double tz) {
			final double[] locations = this.vertices.toArray();
			
			for (int i = this.offset; i < this.nextOffset; i += 3) {
				locations[i + 0] += tx;
				locations[i + 1] += ty;
				locations[i + 2] += tz;
			}
			
			return this;
		}
		
		public final double update() {
			final double[] vertices = this.vertices.toArray();
			
			if (false) {
				final double oldObjective = this.objective;
				this.objective = vertices[this.offset + 8 * 3 + 2];
				
				if (this.objective <= oldObjective) {
					this.activeMuscleIndex = (this.activeMuscleIndex + 1) % this.muscleConstraints.size();
					this.activeMuscleDelta = (random() - 0.5) * 100.0;
				}
				
				final Constraint activeMuscleConstraint = this.muscleConstraints.get(this.activeMuscleIndex);
				
				activeMuscleConstraint.setClampedPreferredDistance(activeMuscleConstraint.getPreferredDistance() + this.activeMuscleDelta);
				
				debugPrint(this.activeMuscleIndex, this.activeMuscleDelta, oldObjective, this.objective);
			}
			
			return Constraint.applyExplicit(this.constraints, vertices, this.masses.toArray());
		}
		
		public final StickMan draw(final double[] locations, final Canvas canvas, final Canvas ids, final AtomicInteger idUnderMouse, final OrbiterMouseHandler orbiter) {
			final Graphics2D g = canvas.getGraphics();
			final int bottom = canvas.getHeight() - 1;
			
			{
				g.setColor(Color.CYAN);
				
				final int n = this.segments.length;
				
				for (int i = 0; i < n; i += 2) {
					final int v0 = this.offset + 3 * this.segments[i + 0];
					final int v1 = this.offset + 3 * this.segments[i + 1];
					
					g.drawLine(iround(locations[v0 + 0]), bottom - iround(locations[v0 + 1]),
							iround(locations[v1 + 0]), bottom - iround(locations[v1 + 1]));
				}
			}
			
			{
				g.setColor(Color.YELLOW);
				
				final int n = this.muscles.length;
				
				for (int i = 0; i < n; i += 2) {
					final int v0 = this.offset + 3 * this.muscles[i + 0];
					final int v1 = this.offset + 3 * this.muscles[i + 1];
					
					g.drawLine(iround(locations[v0 + 0]), bottom - iround(locations[v0 + 1]),
							iround(locations[v1 + 0]), bottom - iround(locations[v1 + 1]));
				}
			}
			
			for (int i = this.offset; i < this.nextOffset; i += 3) {
				final int r = 3;
				final int left = iround(locations[i + 0]) - r;
				final int top = bottom - iround(locations[i + 1]) - r;
				final int d = 2 * r;
				final int id = i / 3;
				
				g.setColor(id != idUnderMouse.get() ? RED : YELLOW);
				g.fillOval(left, top, d, d);
				
				ids.getGraphics().setColor(new Color(id + 1));
				ids.getGraphics().fillOval(left, top, d, d);
			}
			
			return this;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 6458200427869210903L;
		
		public static final double[] repeat(final int n, final double value) {
			final double[] result = new double[n];
			
			fill(result, value);
			
			return result;
		}
		
	}
	
}
