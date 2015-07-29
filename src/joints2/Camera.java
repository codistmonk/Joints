package joints2;

import static java.lang.Math.round;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

/**
 * @author codistmonk (creation 2014-08-17)
 */
public final class Camera implements Serializable {
	
	private final JL jl;
	
	private final Dimension canvasSize;
	
	private final Rectangle2D.Float viewport;
	
	private final float[] clipping;
	
	private final Matrix4f view;
	
	private final Matrix4f projection;
	
	private ProjectionType projectionType;
	
	public Camera(final JL jl) {
		this.jl = jl;
		this.canvasSize = new Dimension(640, 480);
		this.viewport = new Rectangle2D.Float(0F, 0F, 0.9999F, 0.9999F);
		this.clipping = new float[] { -1F, 1F, -1F, 1F, 1F, 2F };
		this.view = new Matrix4f();
		this.projection = new Matrix4f();
		this.projectionType = ProjectionType.ORTHOGRAPHIC;
		
		this.view.setIdentity();
		this.projection.setIdentity();
		
		jl.enable(JL.Constant.SCISSOR_TEST);
	}
	
	public final synchronized ProjectionType getProjectionType() {
		return this.projectionType;
	}
	
	public final synchronized Camera setProjectionType(final ProjectionType projectionType) {
		this.projectionType = projectionType;
		
		return this;
	}
	
	public final synchronized float[] getClipping() {
		return this.clipping;
	}
	
	public final synchronized Camera setOrthographic() {
		final float left = this.clipping[0];
		final float right = this.clipping[1];
		final float bottom = this.clipping[2];
		final float top = this.clipping[3];
		final float near = this.clipping[4];
		final float far = this.clipping[5];
		final float width = right - left;
		final float height = top - bottom;
		final float depth = far - near;
		final float tx = -(left + right) / width;
		final float ty = -(bottom + top) / height;
		final float tz = -(near + far) / depth;
		
		this.getProjection().setRow(0, 2F / width, 0F, 0F, tx);
		this.getProjection().setRow(1, 0F, 2F / height, 0F, ty);
		this.getProjection().setRow(2, 0F, 0F, -2F / depth, tz);
		this.getProjection().setRow(3, 0F, 0F,0F, 1F);
		
		return this;
	}
	
	public final synchronized Camera setProjection(final float left, final float right, final float bottom, final float top) {
		return this.setProjection(left, right, bottom, top, this.clipping[4], this.clipping[5]);
	}
	
	public final synchronized Camera setProjection(final float near, final float far) {
		return this.setProjection(this.clipping[0], this.clipping[1], this.clipping[2], this.clipping[3], near, far);
	}
	
	public final synchronized Camera setProjection(final float left, final float right,
			final float bottom, final float top, final float near, final float far) {
		this.clipping[0] = left;
		this.clipping[1] = right;
		this.clipping[2] = bottom;
		this.clipping[3] = top;
		this.clipping[4] = near;
		this.clipping[5] = far;
		
		return this.setProjection();
	}
	
	public final synchronized Camera setProjection() {
		this.getProjectionType().setProjection(this.getClipping(), this.getProjection());
		
		return this;
	}
	
	public final synchronized Matrix4f getView() {
		return this.view;
	}
	
	public final synchronized Matrix4f getProjection() {
		return this.projection;
	}
	
	public final synchronized Matrix4f getProjectionView(final Matrix4f result) {
		result.mul(this.getProjection(), this.getView());
		
		return result;
	}
	
	public final synchronized Camera setView(final Point3f from, final Point3f at, final Vector3f up) {
		final Vector3f backward = new Vector3f();
		final Vector3f right = new Vector3f();
		final Vector3f actualUp = new Vector3f();
		
		backward.sub(from, at);
		backward.normalize();
		right.cross(up, backward);
		right.normalize();
		actualUp.cross(backward, right);
		actualUp.normalize();
		
		this.getView().setRow(0, right.x, right.y, right.z, -dot(from, right));
		this.getView().setRow(1, actualUp.x, actualUp.y, actualUp.z, -dot(from, actualUp));
		this.getView().setRow(2, backward.x, backward.y, backward.z, -dot(from, backward));
		this.getView().setRow(3, 0F, 0F, 0F, 1F);
		
		return this;
	}
	
	public final synchronized Rectangle2D.Float getViewport() {
		return this.viewport;
	}
	
	public static final float dot(final Tuple3f t1, final Tuple3f t2) {
		return t1.x * t2.x + t1.y * t2.y + t1.z * t2.z;
	}
	
	public final synchronized Dimension getCanvasSize() {
		return this.canvasSize;
	}
	
	public final synchronized Camera setCanvasSize(final int width, final int height) {
		this.getCanvasSize().setSize(width, height);
		
		return this;
	}
	
	public final synchronized Camera setViewport(final float x, final float y, final float width, final float height) {
		this.getViewport().setRect(x, y, width, height);
		
		return this;
	}
	
	public final synchronized void updateJL() {
		float x = this.getViewport().x;
		float y = this.getViewport().y;
		float width = this.getViewport().width;
		float height = this.getViewport().height;
		
		if (isProportion(x)) {
			x *= this.getCanvasSize().width;
		}
		
		if (isProportion(width)) {
			width *= this.getCanvasSize().width;
		}
		
		if (isProportion(y)) {
			y *= this.getCanvasSize().height;
		}
		
		if (isProportion(height)) {
			height *= this.getCanvasSize().height;
		}
		
		final int rx = round(x);
		final int ry = round(y);
		final int rw = round(width);
		final int rh = round(height);
		
		this.jl.viewport(rx, ry, rw, rh);
		this.jl.scissor(rx, ry, rw, rh);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -2223180723258763939L;
	
	public static final boolean isProportion(final float value) {
		return 0F < value && value < 1F;
	}
	
	/**
	 * @author codistmonk (creation 2014-10-18)
	 */
	public static enum ProjectionType {
		
		ORTHOGRAPHIC {
			
			@Override
			public final void setProjection(final float[] clipping, final Matrix4f projection) {
				final float left = clipping[0];
				final float right = clipping[1];
				final float bottom = clipping[2];
				final float top = clipping[3];
				final float near = clipping[4];
				final float far = clipping[5];
				final float width = right - left;
				final float height = top - bottom;
				final float depth = far - near;
				final float tx = -(left + right) / width;
				final float ty = -(bottom + top) / height;
				final float tz = -(near + far) / depth;
				
				projection.setRow(0, 2F / width, 0F, 0F, tx);
				projection.setRow(1, 0F, 2F / height, 0F, ty);
				projection.setRow(2, 0F, 0F, -2F / depth, tz);
				projection.setRow(3, 0F, 0F,0F, 1F);
			}
			
		}, PERSPECTIVE {
			
			@Override
			public final void setProjection(final float[] clipping, final Matrix4f projection) {
				final float left = clipping[0];
				final float right = clipping[1];
				final float bottom = clipping[2];
				final float top = clipping[3];
				final float near = clipping[4];
				final float far = clipping[5];
				final float width = right - left;
				final float height = top - bottom;
				final float depth = far - near;
				final float n2 = 2F * near;
				final float a = (left + right) / width;
				final float b = (bottom + top) / height;
				final float c = -(near + far) / depth;
				final float d = -n2 * far / depth;
				
				projection.setRow(0, n2 / width, 0F, a, 0F);
				projection.setRow(1, 0F, n2 / height, b, 0F);
				projection.setRow(2, 0F, 0F, c, d);
				projection.setRow(3, 0F, 0F, -1F, 0F);
			}
			
		};
		
		public abstract void setProjection(float[] clipping, Matrix4f projection);
		
	}
	
}