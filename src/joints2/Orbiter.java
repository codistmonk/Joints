package joints2;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sin;
import static java.lang.Math.tan;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.vecmath.Point3f;

import multij.swing.MouseHandler;

/**
 * @author codistmonk (creation 2014-10-17)
 */
public final class Orbiter extends MouseHandler {
	
	private final Camera camera;
	
	private Point mouse;
	
	private final Point3f target;
	
	private float distance;
	
	private double horizontalRadians;
	
	private double verticalRadians;
	
	private float clippingDepth;
	
	private double xRotation;
	
	private double yRotation;
	
	public Orbiter(final AtomicBoolean updateNeeded, final Camera camera) {
		super(updateNeeded);
		this.camera = camera;
		this.target = new Point3f();
		this.distance = 4F;
		this.clippingDepth = 1F;
		this.xRotation = PI / 64.0;
		this.yRotation = PI / 64.0;
		
		this.updateCamera();
	}
	
	public final Camera getCamera() {
		return this.camera;
	}
	
	public final float getDistance() {
		return this.distance;
	}
	
	public final void setDistance(final float distance) {
		this.distance = Math.max(0F, distance);
	}
	
	public final double getHorizontalRadians() {
		return this.horizontalRadians;
	}
	
	public final void setHorizontalRadians(final double horizontalRadians) {
		this.horizontalRadians = horizontalRadians;
	}
	
	public final double getVerticalRadians() {
		return this.verticalRadians;
	}
	
	public final void setVerticalRadians(final double verticalRadians) {
		this.verticalRadians = verticalRadians;
	}
	
	public final Point3f getTarget() {
		return this.target;
	}
	
	public final float getClippingDepth() {
		return this.clippingDepth;
	}
	
	public final void setClippingDepth(final float clippingDepth) {
		this.clippingDepth = clippingDepth;
	}
	
	public final double getXRotation() {
		return this.xRotation;
	}
	
	public final void setXRotation(final double xRotation) {
		this.xRotation = xRotation;
	}
	
	public final double getYRotation() {
		return this.yRotation;
	}
	
	public final void setYRotation(final double yRotation) {
		this.yRotation = yRotation;
	}
	
	@Override
	public final void mouseDragged(final MouseEvent event) {
		if (this.mouse != null) {
			final int x = event.getX();
			final int y = event.getY();
			final double deltaX = this.getXRotation();
			final double deltaY = this.getYRotation();
			this.horizontalRadians -= (x - this.mouse.x) * deltaX;
			this.verticalRadians += (y - this.mouse.y) * deltaY;
			this.verticalRadians = min(max(-PI / 2.0 + deltaY, this.verticalRadians), PI / 2.0 - deltaY);
			
			this.mouse.setLocation(x, y);
			
			this.updateCamera();
		} else {
			this.mouse = event.getPoint();
		}
	}
	
	@Override
	public final void mouseReleased(final MouseEvent event) {
		this.mouse = null;
	}
	
	@Override
	public final void mouseWheelMoved(final MouseWheelEvent event) {
		if (event.getWheelRotation() < 0) {
			this.setDistance(this.getDistance() * 0.8F);
		} else {
			this.setDistance(this.getDistance() * 1.2F);
		}
		
		this.updateCamera();
	}
	
	public final void updateCamera() {
		final float distance = this.getDistance();
		final float[] clipping = this.getCamera().getClipping();
		
		clipping[4] = Math.max(0.01F, distance - this.clippingDepth / 2F);
		clipping[5] = clipping[4] + this.clippingDepth;
		
		{
			final float oldTop = clipping[3];
			final double viewVerticalRadians = PI / 6.0; // TODO allow angle customization
			final float newTop = (float) (clipping[4] * tan(viewVerticalRadians));
			
			for (int i = 0; i <= 3; ++i) {
				clipping[i] *= newTop / oldTop;
			}
		}
		
		this.getCamera().setProjection();
		
		this.getCamera().setView(new Point3f(
				this.target.x + (float) (distance * cos(this.verticalRadians) * sin(this.horizontalRadians)),
				this.target.y + (float) (distance * sin(this.verticalRadians)),
				this.target.z + (float) (distance * cos(this.verticalRadians) * cos(this.horizontalRadians))
				), this.target, JL.Constant.UNIT_Y);
		
		this.getUpdateNeeded().set(true);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 8588231683123271508L;
	
}