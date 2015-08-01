package joints2;

import static java.lang.Float.parseFloat;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;
import static joints2.JointsModel.indexOf;
import static multij.swing.SwingTools.horizontalSplit;
import static multij.swing.SwingTools.scrollable;
import static multij.tools.Tools.*;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import joints2.JointsModel.Segment;
import multij.swing.MouseHandler;
import multij.swing.SwingTools;
import multij.xml.XMLTools;

/**
 * @author codistmonk (creation 2015-07-31)
 */
public final class JointsEditorPanel extends JPanel {
	
	private final ControlPanel controlPanel;
	
	private final Scene scene;
	
	private final JointsModel model;
	
	private final int[] highlighted;
	
	private final Collection<Integer> selection;
	
	private final Orbiter orbiter;
	
	public JointsEditorPanel() {
		super(new BorderLayout());
		this.controlPanel = new ControlPanel();
		this.scene = new Scene().setClearColor(Color.WHITE);
		this.model = new JointsModel(this.getScene(), "joints");
		this.highlighted = new int[1];
		this.selection = new LinkedHashSet<>();
		this.orbiter = new Orbiter(this.getScene().getUpdateNeeded(), this.getScene().getCamera()).addTo(this.getScene().getView());
		
		this.addHierarchyListener(new HierarchyListener() {
			
			@Override
			public final void hierarchyChanged(final HierarchyEvent event) {
				getScene().setWindow(SwingUtilities.getWindowAncestor(JointsEditorPanel.this));
			}
			
		});
		
		this.setupControlPanel();
		this.setupScene();
		
		this.add(horizontalSplit(this.getControlPanel(), this.getScene().getView()), BorderLayout.CENTER);
	}
	
	public final ControlPanel getControlPanel() {
		return this.controlPanel;
	}
	
	public final Scene getScene() {
		return this.scene;
	}
	
	public final JointsModel getModel() {
		return this.model;
	}
	
	public final List<Point3f> getJointLocations() {
		return this.getModel().getJointLocations();
	}
	
	public final List<Segment> getSegments() {
		return this.getModel().getSegments();
	}
	
	public final int[] getHighlighted() {
		return this.highlighted;
	}
	
	public final Collection<Integer> getSelection() {
		return this.selection;
	}
	
	public final Orbiter getOrbiter() {
		return this.orbiter;
	}
	
	public final void clear() {
		final DefaultTableModel properties = ((DefaultTableModel) getControlPanel().getPropertyTable().getModel());
		
		for (int i = properties.getRowCount() - 1; 0 <= i; --i) {
			@SuppressWarnings("unchecked")
			final List<String> key = (List<String>) properties.getValueAt(i, 0);
			
			if (key.get(0).startsWith("segments/") || key.get(0).startsWith("joints/")) {
				properties.removeRow(i);
			}
		}
		
		clearSelection();
		getHighlighted()[0] = 0;
		
		this.getModel().clear();
		
		this.scheduleUpdate();
	}
	
	public final void deletePropertiesByValues(final List<? extends Object> values) {
		final DefaultTableModel properties = ((DefaultTableModel) getControlPanel().getPropertyTable().getModel());
		
		for (int i = properties.getRowCount() - 1; 0 <= i; --i) {
			for (final Object value : values) {
				if (value == properties.getValueAt(i, 1)) {
					properties.removeRow(i);
					
					break;
				}
			}
		}
	}
	
	public final void open() {
		final JFileChooser fileChooser = new JFileChooser();
		
		if (fileChooser.showOpenDialog(JointsEditorPanel.this) == JFileChooser.APPROVE_OPTION) {
			this.open(fileChooser.getSelectedFile());
		}
		
	}
	
	public final JointsEditorPanel open(final File file) {
		try (final InputStream input = new FileInputStream(file)) {
			this.getModel().addFromXML(XMLTools.parse(input));
			
			{
				final DefaultTableModel properties = ((DefaultTableModel) getControlPanel().getPropertyTable().getModel());
				
				{
					final List<Point3f> jointLocations = this.getModel().getJointLocations();
					final int n = jointLocations.size();
					
					for (int i = 0; i < n; ++i) {
						properties.addRow(array(list("joints/" + i), jointLocations.get(i)));
					}
				}
				
				{
					final List<Segment> segments = this.getModel().getSegments();
					final int n = segments.size();
					
					for (int i = 0; i < n; ++i) {
						properties.addRow(array(list("segments/" + i), segments.get(i)));
					}
				}
			}
			
			scheduleUpdate();
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
		
		return this;
	}
	
	public final void save() {
		final JFileChooser fileChooser = new JFileChooser();
		
		if (fileChooser.showSaveDialog(JointsEditorPanel.this) == JFileChooser.APPROVE_OPTION) {
			this.save(fileChooser.getSelectedFile());
		}
	}
	
	public final JointsEditorPanel save(final File file) {
		XMLTools.write(this.getModel().toXML(), file, 0);
		
		return this;
	}
	
	public final void deleteSelection() {
		if (!getSelection().isEmpty()) {
			final Integer[] selected = getSelection().toArray(new Integer[getSelection().size()]);
			final List<Point3f> pointsToRemove = new ArrayList<>();
			final List<Segment> segmentsToRemove = new ArrayList<>();
			
			for (final int id : selected) {
				if (isSegment(id)) {
					segmentsToRemove.add(segment(id));
				} else {
					final Point3f point = point(id);
					
					pointsToRemove.add(point);
					segmentsToRemove.addAll(getSegments().stream().filter(s -> s.isEndPoint(point)).collect(toList()));
				}
			}
			
			this.clearSelection();
			getJointLocations().removeAll(pointsToRemove);
			getSegments().removeAll(segmentsToRemove);
			this.deletePropertiesByValues(segmentsToRemove);
			this.deletePropertiesByValues(pointsToRemove);
			
			scheduleUpdate();
		}
	}
	
	public final void clearSelection() {
		this.getControlPanel().getPropertyTable().getSelectionModel().clearSelection();
		this.getSelection().clear();
	}
	
	public final boolean addToSelection(final int id) {
		{
			final Object object = isJoint(id) ? this.point(id) : this.segment(id);
			final TableModel controlModel = this.getControlPanel().getPropertyTable().getModel();
			final ListSelectionModel selectionModel = this.getControlPanel().getPropertyTable().getSelectionModel();
			final int n = controlModel.getRowCount();
			
			for (int i = 0; i < n; ++i) {
				if (controlModel.getValueAt(i, 1) == object) {
					final boolean result = !selectionModel.isSelectedIndex(i);
					
					selectionModel.addSelectionInterval(i, i);
					
					return result;
				}
			}
		}
		
		throw new IllegalStateException();
	}
	
	public final boolean removeFromSelection(final int id) {
		{
			final Object object = isJoint(id) ? this.point(id) : this.segment(id);
			final TableModel controlModel = this.getControlPanel().getPropertyTable().getModel();
			final ListSelectionModel selectionModel = this.getControlPanel().getPropertyTable().getSelectionModel();
			final int n = controlModel.getRowCount();
			
			for (int i = 0; i < n; ++i) {
				if (controlModel.getValueAt(i, 1) == object) {
					selectionModel.removeSelectionInterval(i, i);
				}
			}
		}
		
		return this.getSelection().remove(id);
	}
	
	final void renderSegments(final Graphics2D g) {
		final int n = getSegments().size();
		
		for (int i = 0; i < n; ++i) {
			final Segment segment = getSegments().get(i);
			
			if ("true".equalsIgnoreCase(segment.getStyle("visible"))) {
				final int id = 2 * i + 2;
				final Path2D shape = new Path2D.Double();
				final Point3f p1 = getScene().getTransformed(segment.getPoint1());
				final Point3f p2 = getScene().getTransformed(segment.getPoint2());
				
				shape.moveTo(p1.x, p1.y);
				shape.lineTo(p2.x, p2.y);
				
				getScene().draw(shape, id == getHighlighted()[0] ? Color.YELLOW : getSelection().contains(id) ? Color.RED : decodeColor(segment.getStyle("color")), id, g);
				
				if ("true".equalsIgnoreCase(getControlPanel().getValue(KEY_CONFIG_SHOW_CONSTRAINTS).toString())) {
					final Point2D p2D = point2D(middle(p1, p2));
					final AffineTransform transform = getScene().getGraphicsTransform();
					
					transform.transform(p2D, p2D);
					getScene().setGraphicsTransform(new AffineTransform());
					
					final String string = String.format("%.1f", segment.getPoint1().distance(segment.getPoint2())) + "/" + String.format("%.1f", segment.getConstraint());
					final Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(string, g);
					
					final float left = (float) (p2D.getX() - stringBounds.getWidth() / 2.0);
					final float bottom = (float) (p2D.getY() + stringBounds.getHeight() / 2.0);
					final float top = (float) (p2D.getY() - stringBounds.getHeight() / 2.0);
					
					stringBounds.setRect(left, top, stringBounds.getWidth(), stringBounds.getHeight());
					
					g.drawString(string, left, bottom);
					
					getScene().fillId(stringBounds, id);
					getScene().setGraphicsTransform(transform);
				}
			}
		}
	}
	
	final void renderJoints(final Graphics2D g) {
		final double r = Double.parseDouble(getControlPanel().getValue(KEY_CONFIG_JOINT_RADIUS));
		final int n = getJointLocations().size();
		
		for (int i = 0; i < n; ++i) {
			final int id = 2 * i + 1;
			final Point3f p = getScene().getTransformed(getJointLocations().get(i));
			final Shape shape = new Ellipse2D.Double(p.x - r, p.y - r, 2.0 * r, 2.0 * r);
			
			getScene().fill(shape, id == getHighlighted()[0] ? Color.YELLOW : getSelection().contains(id) ? Color.RED : Color.BLUE, id, g);
		}
	}
	
	final void scheduleUpdate() {
		getScene().getUpdateNeeded().set(true);
	}
	
	final Point3f point(final int id) {
		return this.getJointLocations().get(jointIndex(id));
	}
	
	final Segment segment(final int id) {
		return this.getSegments().get(segmentIndex(id));
	}
	
	final List<Point3f> collectPointsFromSelection() {
		final List<Point3f> result = new ArrayList<>();
		
		for (final int id : getSelection()) {
			if (isJoint(id)) {
				result.add(point(id));
			} else {
				final Segment segment = segment(id);
				
				result.add(segment.getPoint1());
				result.add(segment.getPoint2());
			}
		}
		
		return result;
	}
	
	private final void setupScene() {
		getScene().getView().getRenderers().add(g -> {
			renderJoints(g);
			renderSegments(g);
		});
		
		new MouseHandler() {
			
			private float z = Float.NaN;
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				if (!Float.isNaN(this.z)) {
					final List<Point3f> activePoints = collectPointsFromSelection();
					
					if (!activePoints.isEmpty()) {
						final Point3f center = center(activePoints);
						final AffineTransform graphicsTransform = getScene().getGraphicsTransform();
						final Point2D currentMouse = new Point2D.Double(event.getX(), event.getY());
						
						try {
							graphicsTransform.inverseTransform(currentMouse, currentMouse);
							
							final Matrix4f m = getScene().getCamera().getProjectionView(new Matrix4f());
							
							m.invert();
							
							final Point3f currentLocation = point3f(currentMouse, this.z);
							
							m.transform(currentLocation);
							
							currentLocation.sub(center);
							
							activePoints.forEach(p -> p.add(currentLocation));
							
							scheduleUpdate();
						} catch (final NoninvertibleTransformException exception) {
							exception.printStackTrace();
						}
					}
				}
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				final int idUnderMouse = getScene().getIdUnderMouse();
				
				if (idUnderMouse != getHighlighted()[0]) {
					getHighlighted()[0] = idUnderMouse;
					scheduleUpdate();
				}
			}
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				getScene().getView().requestFocusInWindow();
				
				if (event.isPopupTrigger()) {
					return;
				}
				
				final int id = getHighlighted()[0];
				
				if (id != 0) {
					this.select(id, event);
					
					this.z = isJoint(id) ? getScene().getTransformed(point(id)).z : middle(getScene().getTransformed(segment(id).getPoint1()).z, getScene().getTransformed(segment(id).getPoint2()).z);
					getScene().getView().removeMouseMotionListener(getOrbiter());
					
					scheduleUpdate();
				} else if (!Arrays.asList(getScene().getView().getMouseMotionListeners()).contains(getOrbiter())) {
					this.z = Float.NaN;
					getScene().getView().addMouseMotionListener(getOrbiter());
				}
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				if (event.isPopupTrigger()) {
					return;
				}
				
				final int id = getHighlighted()[0];
				
				if (event.getClickCount() == 2 && id == 0) {
					final Matrix4f m = new Matrix4f();
					
					getScene().getCamera().getProjectionView(m);
					
					m.invert();
					
					final Point3f p = new Point3f(
							2F * event.getX() / getScene().getView().getWidth() - 1F,
							1F - 2F * event.getY() / getScene().getView().getHeight(),
							0F);
					
					m.transform(p);
					
					final int index = getJointLocations().size();
					
					getJointLocations().add(p);
					((DefaultTableModel) getControlPanel().getPropertyTable().getModel()).addRow(
							array(list("joints/" + index), p));
					this.select(getHighlighted()[0] = index * 2 + 1, event);
					
					scheduleUpdate();
				}
			}
			
			private final void select(final int id, final MouseEvent event) {
				if (event.isShiftDown()) {
					if (!addToSelection(id)) {
						removeFromSelection(id);
					}
				} else {
					clearSelection();
					addToSelection(id);
				}
			}
			
			private static final long serialVersionUID = 8216721073603131316L;
			
		}.addTo(getScene().getView());
		
		getScene().getView().addKeyListener(new KeyAdapter() {
			
			@Override
			public final void keyPressed(final KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_SPACE) {
					final List<Point3f> points = collectPointsFromSelection();
					
					if (!points.isEmpty()) {
						getOrbiter().getTarget().set(center(points));
						getOrbiter().updateCamera();
					}
				} else if (event.getKeyCode() == KeyEvent.VK_N && event.isControlDown()) {
					clear();
				} else if (event.getKeyCode() == KeyEvent.VK_O && event.isControlDown()) {
					open();
				} else if (event.getKeyCode() == KeyEvent.VK_S && event.isControlDown()) {
					save();
				} else if (event.getKeyCode() == KeyEvent.VK_D) {
					SwingTools.show(getScene().getIds().getImage(), "ids", false);
				} else if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
					deleteSelection();
				} else if (event.getKeyCode() == KeyEvent.VK_ENTER) {
					final Integer[] selected = getSelection().toArray(new Integer[getSelection().size()]);
					final int n = selected.length;
					
					if (0 < n) {
						if (Arrays.stream(selected).allMatch(JointsEditorPanel::isJoint)) {
							clearSelection();
							
							for (int i = 0; i < n; ++i) {
								for (int j = i + 1; j < n; ++j) {
									final int i1 = min(selected[i], selected[j]);
									final int i2 = max(selected[i], selected[j]);
									final Segment segment = new Segment(point(i1), point(i2));
									
									if (!getSegments().contains(segment)) {
										final int index = getSegments().size();
										
										getSegments().add(segment);
										((DefaultTableModel) getControlPanel().getPropertyTable().getModel()).addRow(
												array(list("segments/" + index), segment));
										addToSelection(segmentId(index));
									}
								}
							}
							
							scheduleUpdate();
						} else if (Arrays.stream(selected).allMatch(JointsEditorPanel::isSegment)) {
							final ControlPanel message = new ControlPanel();
							final double average = Arrays.stream(selected).map(JointsEditorPanel.this::segment).mapToDouble(Segment::getConstraint).average().getAsDouble();
							final String commonVisibility = Arrays.stream(selected).map(JointsEditorPanel.this::segment).map(s -> s.getStyle("visible")).reduce((v1, v2) -> v1.equals(v2) ? v1 : "").get().toString();
							final String commonColor = Arrays.stream(selected).map(JointsEditorPanel.this::segment).map(s -> s.getStyle("color")).reduce((v1, v2) -> v1.equals(v2) ? v1 : "").get().toString();
							
							message.setValue("constraint", Double.toString(average));
							message.setValue("visible", commonVisibility);
							message.setValue("color", commonColor);
							
							if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(JointsEditorPanel.this, message)) {
								final double newConstraint = Double.parseDouble(nonempty(message.getValue("constraint"), "NaN"));
								final String newVisibility = message.getValue("visible");
								final String newColor = message.getValue("color");
								
								Arrays.stream(selected).forEach(id -> segment(id).updateConstraint(newConstraint).updateStyle("visible", newVisibility).updateStyle("color", newColor));
								
								scheduleUpdate();
							}
						}
					}
				}
			}
			
		});
		
		getScene().getView().setFocusable(true);
		
		getScene().getView().getRenderers().add(g -> {
			if ("true".equalsIgnoreCase(getControlPanel().getValue(KEY_CONFIG_SOLVE_CONSTRAINTS).toString())) {
				getModel().applyConstraints(getScene().getUpdateNeeded());
			}
		});
		
		{
			final List<Point3f> locations = getScene().getLocations().computeIfAbsent("shape", k -> new ArrayList<>());
			
			locations.add(new Point3f(-1F, 0F, -1F));
			locations.add(new Point3f(-1F, 0F, 1F));
			locations.add(new Point3f(1F, 0F, 1F));
			locations.add(new Point3f(1F, 0F, -1F));
		}
		
		getScene().getView().getRenderers().add(g -> {
			getScene().draw(getScene().polygon("shape", new Path2D.Float()), Color.BLACK, -1, g);
		});
	}
	
	private final void setupControlPanel() {
		final JTable propertyTable = this.getControlPanel().getPropertyTable();
		final DefaultTableModel properties = (DefaultTableModel) propertyTable.getModel();
		
		properties.addRow(array(KEY_CONFIG_SHOW_CONSTRAINTS, "false"));
		properties.addRow(array(KEY_CONFIG_SOLVE_CONSTRAINTS, "true"));
		properties.addRow(array(KEY_CONFIG_SEGMENT_THICKNESS, Float.toString(JLView.DEFAULT_LINE_THICKNESS)));
		properties.addRow(array(KEY_CONFIG_JOINT_RADIUS, "0.02"));
		
		properties.addTableModelListener(new TableModelListener() {
			
			@Override
			public final void tableChanged(final TableModelEvent event) {
				if (event.getType() == TableModelEvent.UPDATE) {
					final Object key = properties.getValueAt(event.getFirstRow(), 0);
					
					if (KEY_CONFIG_SEGMENT_THICKNESS.equals(key)) {
						final String value = properties.getValueAt(event.getFirstRow(), 1).toString();
						final JLView view = getScene().getView();
						final Graphics2D canvasGraphics = view.getCanvas().getGraphics();
						
						canvasGraphics.setStroke(new BasicStroke(parseFloat(value) / max(view.getWidth(), view.getHeight())));
						getScene().getIds().getGraphics().setStroke(canvasGraphics.getStroke());
						
						scheduleUpdate();
					}
				}
			}
			
		});
		
		final ListSelectionModel selectionModel = propertyTable.getSelectionModel();
		
		selectionModel.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public final void valueChanged(final ListSelectionEvent event) {
				if (!event.getValueIsAdjusting()) {
					final int n = properties.getRowCount();
					boolean scheduleUpdate = false;
					
					for (int i = 0; i < n; ++i) {
						final Object value = properties.getValueAt(i, 1);
						int index = indexOf(value, getJointLocations());
						int id = 0;
						
						if (0 <= index) {
							id = jointId(index);
						} else {
							index = indexOf(value, getSegments());
							
							if (0 <= index) {
								id = segmentId(index);
							}
						}
						
						if (0 < id) {
							if (selectionModel.isSelectedIndex(i)) {
								getSelection().add(id);
								scheduleUpdate = true;
							} else {
								getSelection().remove(id);
								scheduleUpdate = true;
							}
						}
					}
					
					if (scheduleUpdate) {
						scheduleUpdate();
					}
				}
			}
			
		});
	}
	
	private static final long serialVersionUID = 6374986295888991754L;
	
	static final List<String> KEY_CONFIG_SHOW_CONSTRAINTS = Arrays.asList("config/show_constraints");
	
	static final List<String> KEY_CONFIG_SEGMENT_THICKNESS = Arrays.asList("config/segment_thickness");
	
	static final List<String> KEY_CONFIG_JOINT_RADIUS = Arrays.asList("config/joint_radius");
	
	static final List<String> KEY_CONFIG_SOLVE_CONSTRAINTS = Arrays.asList("config/solve_constraints");
	
	public static final Point3f center(final List<Point3f> points) {
		final Point3f result = new Point3f();
		
		if (!points.isEmpty()) {
			points.forEach(result::add);
			result.scale(1F / points.size());
		}
		
		return result;
	}
	
	public static final String nonempty(final String value, final String defaultValue) {
		return value == null | value.isEmpty() ? defaultValue : value;
	}
	
	public static final <E> List<E> list(@SuppressWarnings("unchecked") final E... elements) {
		return new ArrayList<>(Arrays.asList(elements));
	}
	
	public static final Point3f point3f(final Point2D p) {
		return point3f(p, 0F);
	}
	
	public static final Point3f point3f(final Point2D p, final float z) {
		return point3f(p, z, new Point3f());
	}
	
	public static final Point3f point3f(final Point2D p, final Point3f result) {
		return point3f(p, 0F, result);
	}
	
	public static final Point3f point3f(final Point2D p, final float z, final Point3f result) {
		result.x = (float) p.getX();
		result.y = (float) p.getY();
		result.z = z;
		
		return result;
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
		result.set(middle(p1.x, p2.x), middle(p1.y, p2.y), middle(p1.z, p2.z));
		
		return result;
	}
	
	public static final Color decodeColor(final String color) {
		return new Color(Long.decode(color).intValue(), true);
	}
	
	public static final float middle(final float a, final float b) {
		return (a + b) / 2F;
	}
	
	static final boolean isJoint(final int id) {
		return (id & 1) == 1;
	}
	
	static final boolean isSegment(final int id) {
		return (id & 1) == 0;
	}
	
	static final int jointId(final int index) {
		return index * 2 + 1;
	}
	
	static final int segmentId(final int index) {
		return index * 2 + 2;
	}
	
	static final int jointIndex(final int id) {
		return id >> 1;
	}
	
	static final int segmentIndex(final int id) {
		return (id - 2) >> 1;
	}
	
	/**
	 * @author codistmonk (creation 2015-07-31)
	 */
	public static final class ControlPanel extends JPanel {
		
		private final JTextField filterField;
		
		private final JTable propertyTable;
		
		@SuppressWarnings("unchecked")
		public ControlPanel() {
			super(new BorderLayout());
			this.filterField = new JTextField();
			this.propertyTable = new JTable(new DefaultTableModel(array("Key", "Value"), 0) {
				
				@Override
				public final boolean isCellEditable(final int row, final int column) {
					return column != 0;
				}
				
				private static final long serialVersionUID = 6329345725539288994L;
				
			});
			
			this.getPropertyTable().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			this.getPropertyTable().setRowSorter(new TableRowSorter<>(this.getPropertyTable().getModel()));
			
			this.getFilterField().addActionListener(event -> {
				((TableRowSorter<DefaultTableModel>) getPropertyTable().getRowSorter()).setRowFilter(RowFilter.regexFilter(getFilterField().getText()));
			});
			
			this.add(this.getFilterField(), BorderLayout.NORTH);
			this.add(scrollable(this.getPropertyTable()), BorderLayout.CENTER);
		}
		
		public final JTextField getFilterField() {
			return this.filterField;
		}
		
		public final JTable getPropertyTable() {
			return this.propertyTable;
		}
		
		@SuppressWarnings("unchecked")
		public final <V> V getValue(final Object key) {
			final TableModel model = this.getPropertyTable().getModel();
			final int n = model.getRowCount();
			
			for (int i = 0; i < n; ++i) {
				if (key.equals(model.getValueAt(i, 0))) {
					return (V) model.getValueAt(i, 1);
				}
			}
			
			return null;
		}
		
		public final ControlPanel setValue(final Object key, final Object value) {
			final DefaultTableModel model = (DefaultTableModel) this.getPropertyTable().getModel();
			final int n = model.getRowCount();
			
			for (int i = 0; i < n; ++i) {
				if (key.equals(model.getValueAt(i, 0))) {
					model.setValueAt(value, i, 1);
					
					return this;
				}
			}
			
			model.addRow(array(key, value));
			
			return this;
		}
		
		private static final long serialVersionUID = 719479298612973559L;
		
	}
	
}